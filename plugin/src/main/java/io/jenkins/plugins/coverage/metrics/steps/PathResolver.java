package io.jenkins.plugins.coverage.metrics.steps;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.prism.FilePermissionEnforcer;
import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.prism.SourceDirectoryFilter;
import io.jenkins.plugins.util.RemoteResultWrapper;

/**
 * Resolves source code files on the agent using the stored paths of the coverage reports. Since these paths are
 * relative, this resolver tries to find the absolute paths by guessing the prefix to the relative path. It also
 * evaluates the defined source paths as prefixes when resolving the absolute paths.
 */
public class PathResolver {
    /**
     * Resolves source code files on the agent using the stored paths of the coverage reports. Since these paths are
     * relative, this resolver tries to find the absolute paths by guessing the prefix to the relative path. It also
     * evaluates the defined source paths as prefixes when resolving the absolute paths.
     *
     * @param relativePaths
     *         the relative paths to map
     * @param requestedSourceDirectories
     *         the requested relative and absolute source directories (in the step configuration)
     * @param workspace
     *         the workspace that contains the source code files
     * @param log
     *         the log to write to
     *
     * @return the resolved paths as mapping of relative to absolute paths
     */
    public Map<String, String> resolvePaths(final Set<String> relativePaths,
            final Set<String> requestedSourceDirectories,
            final FilePath workspace, final FilteredLog log) throws InterruptedException {
        try {
            Set<String> permittedSourceDirectories = PrismConfiguration.getInstance()
                    .getSourceDirectories()
                    .stream()
                    .map(PermittedSourceCodeDirectory::getPath)
                    .collect(Collectors.toSet());

            var resolver = new AgentPathResolver(relativePaths, permittedSourceDirectories, requestedSourceDirectories);
            var agentLog = workspace.act(resolver);
            log.merge(agentLog);
            return agentLog.getResult();
        }
        catch (IOException exception) {
            log.logException(exception, "Can't resolve source files on agent");
        }
        return Collections.emptyMap();
    }

    /**
     * Resolves source code files on the agent using the stored paths of the coverage reports. Since these paths are
     * relative, this resolver tries to find the absolute paths by guessing the prefix to the relative path. It also
     * evaluates the defined source paths as prefixes when resolving the absolute paths.
     */
    static class AgentPathResolver extends MasterToSlaveFileCallable<RemoteResultWrapper<HashMap<String, String>>> {
        private static final long serialVersionUID = 3966282357309568323L;
        private static final PathUtil PATH_UTIL = new PathUtil();

        private final Set<String> relativePaths;
        private final Set<String> permittedSourceDirectories;
        private final Set<String> requestedSourceDirectories;

        /**
         * Creates a new instance of {@link AgentPathResolver}.
         *
         * @param relativePaths
         *         the relative paths to map
         * @param permittedSourceDirectories
         *         the permitted source code directories (in Jenkins global configuration)
         * @param requestedSourceDirectories
         *         the requested relative and absolute source directories (in the step configuration)
         */
        AgentPathResolver(final Set<String> relativePaths,
                final Set<String> permittedSourceDirectories,
                final Set<String> requestedSourceDirectories) {
            super();

            this.relativePaths = relativePaths;
            this.permittedSourceDirectories = permittedSourceDirectories;
            this.requestedSourceDirectories = requestedSourceDirectories;
        }

        @Override
        public RemoteResultWrapper<HashMap<String, String>> invoke(
                final File workspaceFile, final VirtualChannel channel) {
            FilteredLog log = new FilteredLog("Errors while resolving source files on agent:");

            Set<String> sourceDirectories = filterSourceDirectories(workspaceFile, log);
            if (sourceDirectories.isEmpty()) {
                log.logInfo("Searching for source code files in root of workspace '%s'", workspaceFile);
            }
            else if (sourceDirectories.size() == 1) {
                log.logInfo("Searching for source code files in '%s'", sourceDirectories.iterator().next());
            }
            else {
                log.logInfo("Searching for source code files in:", workspaceFile);
                sourceDirectories.forEach(dir -> log.logInfo("-> %s", dir));
            }

            var workspace = new FilePath(workspaceFile);
            var mapping = relativePaths.stream()
                    .map(path -> new SimpleEntry<>(path, locateSource(path, workspace, sourceDirectories, log)))
                    .filter(entry -> entry.getValue().isPresent())
                    .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().get()));

            if (mapping.size() == relativePaths.size()) {
                log.logInfo("-> resolved absolute paths for all %d source files", mapping.size());
            }
            else {
                log.logInfo("-> finished resolving of absolute paths (found: %d, not found: %d)",
                        mapping.size(), relativePaths.size() - mapping.size());
            }

            var changedFilesMapping = mapping.entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().equals(entry.getValue()))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            var result = new RemoteResultWrapper<>(new HashMap<>(changedFilesMapping), "Errors during source path resolving:");
            result.merge(log);
            return result;
        }

        private Set<String> filterSourceDirectories(final File workspace, final FilteredLog log) {
            SourceDirectoryFilter filter = new SourceDirectoryFilter();
            return filter.getPermittedSourceDirectories(workspace.getAbsolutePath(),
                    permittedSourceDirectories, requestedSourceDirectories, log);
        }

        private Optional<String> locateSource(final String relativePath, final FilePath workspace,
                final Set<String> sourceSearchDirectories, final FilteredLog log) {

            try {
                FilePath absolutePath = new FilePath(new File(relativePath));
                if (absolutePath.exists()) {
                    return enforcePermissionFor(absolutePath, workspace, sourceSearchDirectories, log);
                }

                FilePath relativePathInWorkspace = workspace.child(relativePath);
                if (relativePathInWorkspace.exists()) {
                    return enforcePermissionFor(relativePathInWorkspace, workspace, sourceSearchDirectories, log);
                }

                for (String sourceFolder : sourceSearchDirectories) {
                    FilePath sourcePath = workspace.child(sourceFolder).child(relativePath);
                    if (sourcePath.exists()) {
                        return enforcePermissionFor(sourcePath, workspace, sourceSearchDirectories, log);
                    }
                }

                log.logError("- Source file '%s' not found", relativePath);
            }
            catch (InvalidPathException | IOException | InterruptedException exception) {
                log.logException(exception, "No valid path in coverage node: '%s'", relativePath);
            }
            return Optional.empty();
        }

        private Optional<String> enforcePermissionFor(final FilePath absolutePath, final FilePath workspace,
                final Set<String> sourceDirectories, final FilteredLog log) {
            FilePermissionEnforcer enforcer = new FilePermissionEnforcer();
            var fileName = absolutePath.getRemote();
            if (enforcer.isInWorkspace(fileName, workspace, sourceDirectories)) {
                if (isWithinWorkspace(fileName, workspace)) {
                    return Optional.of(PATH_UTIL.getRelativePath(workspace.getRemote(), fileName));
                }
                else {
                    return Optional.of(PATH_UTIL.getAbsolutePath(fileName));
                }
            }
            log.logError("- Skipping resolving of file: %s (not part of workspace or permitted source code folders)",
                    fileName);
            return Optional.empty();
        }

        private boolean isWithinWorkspace(final String fileName, final FilePath workspace) {
            var workspacePath = PATH_UTIL.getAbsolutePath(workspace.getRemote());
            return PATH_UTIL.getAbsolutePath(fileName).startsWith(workspacePath);
        }
    }
}
