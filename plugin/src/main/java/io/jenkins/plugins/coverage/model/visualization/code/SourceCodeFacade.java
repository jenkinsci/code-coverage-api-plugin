package io.jenkins.plugins.coverage.model.visualization.code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.FilteredLog;

import hudson.FilePath;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.util.TextFile;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import io.jenkins.plugins.prism.CharsetValidation;
import io.jenkins.plugins.prism.FilePermissionEnforcer;
import io.jenkins.plugins.prism.SourceDirectoryFilter;

/**
 * Facade to the source code file structure in Jenkins build folder. Access of those files should be done using an
 * instance of this class only.
 *
 * @author Ullrich Hafner
 */
public class SourceCodeFacade {
    /** Toplevel directory in the build folder of the controller that contains the zipped source files. */
    static final String COVERAGE_SOURCES_DIRECTORY = "coverage-sources";
    static final String COVERAGE_SOURCES_ZIP = "coverage-sources.zip";
    static final int MAX_FILENAME_LENGTH = 245; // Windows has limitations on long file names
    static final String ZIP_FILE_EXTENSION = ".zip";

    String getCoverageSourcesDirectory() {
        return COVERAGE_SOURCES_DIRECTORY;
    }

    /**
     * Copies the zipped source files from the agent to the controller and unpacks them in the coverage-sources folder
     * of the current build.
     *
     * @param build
     *         the build with the coverage result
     * @param workspace
     *         the workspace on the agent that created the ZIP file
     * @param log
     *         the log
     *
     * @throws InterruptedException
     *         in case the user terminated the job
     */
    void copySourcesToBuildFolder(final Run<?, ?> build, final FilePath workspace, final FilteredLog log)
            throws InterruptedException {
        try {
            FilePath buildFolder = new FilePath(build.getRootDir()).child(COVERAGE_SOURCES_DIRECTORY);
            FilePath buildZip = buildFolder.child(COVERAGE_SOURCES_ZIP);
            workspace.child(COVERAGE_SOURCES_ZIP).copyTo(buildZip);
            log.logInfo("-> extracting...");
            buildZip.unzip(buildFolder);
            buildZip.delete();
            log.logInfo("-> done");
        }
        catch (IOException exception) {
            log.logException(exception, "Can't copy zipped sources from agent to controller");
        }
    }

    /**
     * Reads the contents of the source file of the given coverage node into a String.
     *
     * @param buildResults
     *         Jenkins directory for build results
     * @param id
     *         if of the coverage results
     * @param path
     *         relative path to the coverage node base filename of the coverage node
     *
     * @return the file content as String
     */
    String read(final File buildResults, final String id, final String path)
            throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory(COVERAGE_SOURCES_DIRECTORY);
        FilePath unzippedSourcesDir = new FilePath(tempDir.toFile());
        try {
            FilePath inputZipFile = new FilePath(
                    new SourceCodeFacade().createFileInBuildFolder(buildResults, id, path));
            inputZipFile.unzip(unzippedSourcesDir);
            String actualPaintedSourceFileName = StringUtils.removeEnd(AgentCoveragePainter.sanitizeFilename(path),
                    ZIP_FILE_EXTENSION);
            File sourceFile = tempDir.resolve(actualPaintedSourceFileName).toFile();
            return new TextFile(sourceFile).read();
        }
        finally {
            unzippedSourcesDir.deleteRecursive();
        }
    }

    /**
     * Returns a file to the sources in release 2.1.0 and newer. Note that the file might not exist.
     *
     * @param buildResults
     *         Jenkins directory for build results
     * @param id
     *         if of the coverage results
     * @param path
     *         relative path to the coverage node base filename of the coverage node
     *
     * @return the file
     */
    public File createFileInBuildFolder(final File buildResults, final String id, final String path) {
        File sourceFolder = new File(buildResults, COVERAGE_SOURCES_DIRECTORY);
        File elementFolder = new File(sourceFolder, id);

        return new File(elementFolder, AgentCoveragePainter.sanitizeFilename(path) + ZIP_FILE_EXTENSION);
    }

    /**
     * Paints source code files on the agent using the recorded coverage information. All files are stored as zipped
     * HTML files that contain the painted source code. In the last step all zipped source files are aggregated into a
     * single archive to simplify copying to the controller.
     */
    static class AgentCoveragePainter extends MasterToSlaveFileCallable<FilteredLog> {
        private static final long serialVersionUID = 3966282357309568323L;

        private static String sanitizeFilename(final String inputName) {
            return StringUtils.right(inputName.replaceAll("[^a-zA-Z0-9-_.]", "_"), MAX_FILENAME_LENGTH);
        }

        private final List<PaintedNode> paintedFiles;
        private final Set<String> permittedSourceDirectories;

        private final Set<String> requestedSourceDirectories;
        private final String sourceCodeEncoding;

        private final String directory;

        /**
         * Creates a new instance of {@link AgentCoveragePainter}.
         *
         * @param paintedFiles
         *         the model for the file painting for each coverage node
         * @param permittedSourceDirectories
         *         the permitted source code directories (in Jenkins global configuration)
         * @param requestedSourceDirectories
         *         the requested relative and absolute source directories (in the step configuration)
         * @param sourceCodeEncoding
         *         the encoding of the source code files
         * @param directory
         *         the subdirectory where the source files will be stored in
         */
        AgentCoveragePainter(final Set<Entry<CoverageNode, CoveragePaint>> paintedFiles,
                final Set<String> permittedSourceDirectories, final Set<String> requestedSourceDirectories,
                final String sourceCodeEncoding, final String directory) {
            super();

            this.paintedFiles = paintedFiles.stream()
                    .map(e -> new PaintedNode(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            this.permittedSourceDirectories = permittedSourceDirectories;
            this.requestedSourceDirectories = requestedSourceDirectories;
            this.sourceCodeEncoding = sourceCodeEncoding;
            this.directory = directory;
        }

        @Override
        public FilteredLog invoke(final File workspaceFile, final VirtualChannel channel)
                throws IOException, InterruptedException {
            FilteredLog log = new FilteredLog("Errors during source code painting:");
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
            FilePath workspace = new FilePath(workspaceFile);

            try {
                FilePath outputFolder = workspace.child(directory);
                outputFolder.mkdirs();

                Charset charset = getCharset();
                int count = paintedFiles.parallelStream()
                        .mapToInt(file -> paintSource(file, workspace, sourceDirectories, charset, log))
                        .sum();

                if (count == paintedFiles.size()) {
                    log.logInfo("-> finished painting successfully");
                }
                else {
                    log.logInfo("-> finished painting (%d files have been painted, %d files failed)",
                            count, paintedFiles.size() - count);
                }

                FilePath zipFile = workspace.child(COVERAGE_SOURCES_ZIP);
                outputFolder.zip(zipFile);
                log.logInfo("-> zipping sources from folder '%s' as '%s'", outputFolder, zipFile);
            }
            catch (IOException exception) {
                log.logException(exception,
                        "Cannot create temporary directory in folder '%s' for the painted source files", workspace);
            }
            catch (InterruptedException exception) {
                log.logException(exception,
                        "Processing has been interrupted: skipping zipping of source files", workspace);
            }

            return log;
        }

        private Charset getCharset() {
            return new CharsetValidation().getCharset(sourceCodeEncoding);
        }

        private Set<String> filterSourceDirectories(final File workspace, final FilteredLog log) {
            SourceDirectoryFilter filter = new SourceDirectoryFilter();
            return filter.getPermittedSourceDirectories(workspace.getAbsolutePath(),
                            permittedSourceDirectories, requestedSourceDirectories, log);
        }

        private int paintSource(final PaintedNode fileNode, final FilePath workspace,
                final Set<String> sourceSearchDirectories, final Charset sourceEncoding, final FilteredLog log) {
            String relativePathIdentifier = fileNode.getNode().getPath();
            FilePath paintedFilesDirectory = workspace.child(directory);
            return findSourceFile(workspace, relativePathIdentifier, sourceSearchDirectories, log)
                    .map(resolvedPath -> paint(fileNode.getPaint(), relativePathIdentifier, resolvedPath,
                            paintedFilesDirectory,
                            sourceEncoding, log))
                    .orElse(0);
        }

        private int paint(final CoveragePaint paint, final String relativePathIdentifier, final FilePath resolvedPath,
                final FilePath paintedFilesDirectory, final Charset charset, final FilteredLog log) {
            String sanitizedFileName = sanitizeFilename(relativePathIdentifier);
            FilePath zipOutputPath = paintedFilesDirectory.child(sanitizedFileName + ZIP_FILE_EXTENSION);
            try {
                Path paintedFilesFolder = Files.createTempDirectory(directory);
                Path fullSourcePath = paintedFilesFolder.resolve(sanitizedFileName);
                try (BufferedWriter output = Files.newBufferedWriter(fullSourcePath)) {
                    List<String> lines = Files.readAllLines(Paths.get(resolvedPath.getRemote()), charset);
                    for (int line = 0; line < lines.size(); line++) {
                        String content = lines.get(line);
                        paintLine(line + 1, content, paint, output);
                    }
                    paint.setTotalLines(lines.size());
                }
                new FilePath(fullSourcePath.toFile()).zip(zipOutputPath);
                return 1;
            }
            catch (IOException | InterruptedException exception) {
                log.logException(exception, "Can't write coverage paint of '%s' to zipped source file '%s'",
                        relativePathIdentifier, zipOutputPath);
                return 0;
            }
        }

        private void paintLine(final int line, final String content, final CoveragePaint paint,
                final BufferedWriter output) throws IOException {
            if (paint.isPainted(line)) {
                final int hits = paint.getHits(line);
                final int branchCoverage = paint.getBranchCoverage(line);
                final int branchTotal = paint.getBranchTotal(line);
                final int coveragePercent = (hits == 0) ? 0 : (int) (branchCoverage * 100.0 / branchTotal);
                if (hits > 0) {
                    if (branchTotal == branchCoverage) {
                        output.write("<tr class=\"coverFull\">\n");
                    }
                    else {
                        output.write("<tr class=\"coverPart\" title=\"Line " + line + ": Conditional coverage "
                                + coveragePercent + "% ("
                                + branchCoverage + "/" + branchTotal + ")\">\n");
                    }
                }
                else {
                    output.write("<tr class=\"coverNone\">\n");
                }
                output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");
                output.write("<td class=\"hits\">" + hits + "</td>\n");
            }
            else {
                output.write("<tr class=\"noCover\">\n");
                output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");
                output.write("<td class=\"hits\"></td>\n");
            }
            output.write("<td class=\"code\">"
                    + content.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace(" ",
                            "&nbsp;")
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "</td>\n");
            output.write("</tr>\n");
        }

        private Optional<FilePath> findSourceFile(final FilePath workspace, final String fileName,
                final Set<String> sourceDirectories, final FilteredLog log) {
            try {
                FilePath absolutePath = new FilePath(new File(fileName));
                if (absolutePath.exists()) {
                    return enforcePermissionFor(absolutePath, workspace, sourceDirectories, log);
                }

                FilePath relativePath = workspace.child(fileName);
                if (relativePath.exists()) {
                    return enforcePermissionFor(relativePath, workspace, sourceDirectories, log);
                }

                for (String sourceFolder : sourceDirectories) {
                    FilePath sourcePath = workspace.child(sourceFolder).child(fileName);
                    if (sourcePath.exists()) {
                        return enforcePermissionFor(sourcePath, workspace, sourceDirectories, log);
                    }
                }

                log.logError("Source file '%s' not found", fileName);
            }
            catch (InvalidPathException | IOException | InterruptedException exception) {
                log.logException(exception, "No valid path in coverage node: '%s'", fileName);
            }
            return Optional.empty();
        }

        private Optional<FilePath> enforcePermissionFor(final FilePath absolutePath, final FilePath workspace,
                final Set<String> sourceDirectories, final FilteredLog log) {
            FilePermissionEnforcer enforcer = new FilePermissionEnforcer();
            if (enforcer.isInWorkspace(absolutePath.getRemote(), workspace, sourceDirectories)) {
                return Optional.of(absolutePath);
            }
            log.logError("Skipping coloring of file: %s (not part of workspace or permitted source code folders)",
                    absolutePath.getRemote());
            return Optional.empty();
        }

        private static class PaintedNode implements Serializable {
            private static final long serialVersionUID = -6044649044983631852L;

            private final CoverageNode node;
            private final CoveragePaint paint;

            PaintedNode(final CoverageNode node, final CoveragePaint paint) {
                this.node = node;
                this.paint = paint;
            }

            public CoverageNode getNode() {
                return node;
            }

            public CoveragePaint getPaint() {
                return paint;
            }
        }
    }
}
