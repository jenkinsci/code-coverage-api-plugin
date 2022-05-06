package io.jenkins.plugins.coverage.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.model.exception.CodeDeltaException;
import io.jenkins.plugins.forensics.delta.DeltaCalculatorFactory;
import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.delta.model.FileEditType;

/**
 * Calculates the code delta between a Jenkins build and a reference build.
 *
 * @author Florian Orendi
 */
public class CodeDeltaCalculator {

    static final String AMBIGUOUS_PATHS_ERROR =
            "Failed to map SCM paths with coverage report paths due to ambiguous fully qualified names";

    private final Run<?, ?> build;
    private final FilePath workspace;
    private final TaskListener listener;
    private final String scm;

    /**
     * Creates a code delta calculator for a specific build.
     *
     * @param build
     *         The build
     * @param workspace
     *         The workspace
     * @param listener
     *         The listener
     * @param scm
     *         The selected SCM
     */
    public CodeDeltaCalculator(final Run<?, ?> build, final FilePath workspace,
            final TaskListener listener, final String scm) {
        this.build = build;
        this.workspace = workspace;
        this.listener = listener;
        this.scm = scm;
    }

    /**
     * Calculates the code delta between the {@link #build} and the passed reference build.
     *
     * @param referenceBuild
     *         The reference build
     * @param log
     *         The log
     *
     * @return the {@link Delta code delta} as Optional if existent, else an empty Optional
     */
    public Optional<Delta> calculateCodeDeltaToReference(final Run<?, ?> referenceBuild, final FilteredLog log) {
        return DeltaCalculatorFactory
                .findDeltaCalculator(scm, build, workspace, listener, log)
                .calculateDelta(build, referenceBuild, scm, log);
    }

    /**
     * Gets all code changes which are relevant for the change coverage (added and modified files).
     *
     * @param delta
     *         The calculated code {@link Delta}
     *
     * @return the relevant code changes
     */
    public Set<FileChanges> getChangeCoverageRelevantChanges(final Delta delta) {
        return delta.getFileChangesMap().values().stream()
                .filter(fileChange -> fileChange.getFileEditType().equals(FileEditType.MODIFY)
                        || fileChange.getFileEditType().equals(FileEditType.ADD))
                .collect(Collectors.toSet());
    }

    /**
     * Maps the passed {@link FileChanges code changes} to the corresponding fully qualified names as they are used by
     * the coverage reporting tools - usually the fully qualified name of the file.
     *
     * @param changes
     *         The code changes
     * @param root
     *         The root of the coverage tree
     * @param log
     *         The log
     *
     * @return the create code changes mapping
     * @throws CodeDeltaException
     *         when creating the mapping failed due to ambiguous paths
     */
    public Map<String, FileChanges> mapScmChangesToReportPaths(
            final Set<FileChanges> changes, final CoverageNode root, final FilteredLog log) throws CodeDeltaException {
        Set<String> reportPaths = root.getAllFileCoverageNodes().stream()
                .map(FileCoverageNode::getPath)
                .collect(Collectors.toSet());
        Set<String> scmPaths = changes.stream()
                .map(FileChanges::getFileName)
                .collect(Collectors.toSet());

        Map<String, String> pathMapping = getScmToReportPathMapping(scmPaths, reportPaths);
        verifyScmToReportPathMapping(pathMapping, log);

        return changes.stream()
                .filter(change -> reportPaths.contains(pathMapping.get(change.getFileName())))
                .collect(Collectors.toMap(
                        fileChange -> pathMapping.get(fileChange.getFileName()), Function.identity()));

    }

    /**
     * Creates a mapping between SCM paths and the corresponding coverage report paths.
     *
     * @param scmPaths
     *         The SCM paths
     * @param reportPaths
     *         The coverage report paths
     *
     * @return the created mapping
     */
    private Map<String, String> getScmToReportPathMapping(final Set<String> scmPaths, final Set<String> reportPaths) {
        Map<String, String> pathMapping = new HashMap<>();
        for (String scmPath : scmPaths) {
            reportPaths.stream()
                    .filter(scmPath::endsWith)
                    .max(Comparator.comparingInt(String::length))
                    .map(match -> {
                        pathMapping.put(scmPath, match);
                        return match;
                    })
                    .orElseGet(() -> pathMapping.put(scmPath, ""));
        }
        return pathMapping;
    }

    /**
     * Verifies the the passed mapping between SCM and coverage report paths.
     *
     * @param pathMapping
     *         The path mapping
     * @param log
     *         The log
     *
     * @throws CodeDeltaException
     *         when ambiguous paths has been detected
     */
    private void verifyScmToReportPathMapping(final Map<String, String> pathMapping, final FilteredLog log)
            throws CodeDeltaException {
        List<String> notEmptyValues = pathMapping.values().stream()
                .filter(path -> !path.isEmpty())
                .collect(Collectors.toList());
        if (notEmptyValues.size() != new HashSet<>(notEmptyValues).size()) {
            throw new CodeDeltaException(AMBIGUOUS_PATHS_ERROR);
        }
        log.logInfo("Successfully mapped SCM paths to coverage report paths");
    }
}
