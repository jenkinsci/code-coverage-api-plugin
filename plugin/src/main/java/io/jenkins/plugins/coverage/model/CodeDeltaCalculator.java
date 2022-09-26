package io.jenkins.plugins.coverage.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;

import one.util.streamex.StreamEx;

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
    static final String AMBIGUOUS_OLD_PATHS_ERROR =
            "Failed to map SCM paths from the reference with coverage report paths from the reference "
                    + "due to ambiguous fully qualified names";

    static final String CODE_DELTA_TO_COVERAGE_DATA_MISMATCH_ERROR_TEMPLATE =
            "Unexpected behavior detected when comparing coverage data with the code delta "
                    + "- there are ambiguous paths when comparing new with former file paths: ";

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
     * Gets all code changes which are relevant for the coverage (added, renamed and modified files).
     *
     * @param delta
     *         The calculated code {@link Delta}
     *
     * @return the relevant code changes
     */
    public Set<FileChanges> getCoverageRelevantChanges(final Delta delta) {
        return delta.getFileChangesMap().values().stream()
                .filter(fileChange -> fileChange.getFileEditType().equals(FileEditType.MODIFY)
                        || fileChange.getFileEditType().equals(FileEditType.ADD)
                        || fileChange.getFileEditType().equals(FileEditType.RENAME))
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
     * Creates a mapping between the currently used coverage report paths and the corresponding paths that has been used
     * for the same coverage nodes before the modifications. This affects only renamed and untouched / modified files
     * without a renaming, since added files did not exist before and deleted files do not exist anymore.
     *
     * @param root
     *         The root of the coverage tree
     * @param referenceRoot
     *         The root of the coverage tree from the reference build
     * @param changes
     *         The {@link FileChanges changes}, mapped by the currently used coverage report path to which they
     *         correspond to
     * @param log
     *         The log
     *
     * @return the created mapping whose keys are the currently used paths and whose values are the paths before the
     *         modifications
     * @throws CodeDeltaException
     *         if the SCM path mapping is ambiguous
     */
    public Map<String, String> createOldPathMapping(final CoverageNode root, final CoverageNode referenceRoot,
            final Map<String, FileChanges> changes, final FilteredLog log)
            throws CodeDeltaException {
        Set<String> oldReportPaths = referenceRoot.getAllFileCoverageNodes().stream()
                .map(FileCoverageNode::getPath)
                .collect(Collectors.toSet());
        // the affected, currently used report paths and the SCM paths from the reference
        Map<String, String> newReportToOldScmPathMapping = changes.entrySet().stream()
                .filter(entry -> FileEditType.RENAME.equals(entry.getValue().getFileEditType()))
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getOldFileName()));
        // the SCM paths and the coverage report paths from the reference
        Map<String, String> oldScmToOldReportPathMapping =
                getScmToReportPathMapping(newReportToOldScmPathMapping.values(), oldReportPaths);

        // replacing the old SCM paths with the old report paths
        Set<String> newReportPathsWithRename = newReportToOldScmPathMapping.keySet();
        newReportToOldScmPathMapping.forEach((reportPath, oldScmPath) -> {
            String oldReportPath = oldScmToOldReportPathMapping.get(oldScmPath);
            newReportToOldScmPathMapping.replace(reportPath, oldReportPath);
        });
        if (!newReportPathsWithRename.equals(newReportToOldScmPathMapping.keySet())) {
            throw new CodeDeltaException(AMBIGUOUS_OLD_PATHS_ERROR);
        }

        // adding the paths, which exist in both trees and contain no changes, to the mapping
        root.getAllFileCoverageNodes().stream()
                .filter(node -> !newReportToOldScmPathMapping.containsKey(node.getPath()) && oldReportPaths.contains(
                        node.getPath()))
                .forEach(node -> newReportToOldScmPathMapping.put(node.getPath(), node.getPath()));

        verifyOldPathMapping(newReportToOldScmPathMapping, log);

        return newReportToOldScmPathMapping;
    }

    /**
     * Creates a mapping between SCM paths and the corresponding coverage report paths.
     *
     * @param scmPaths
     *         The SCM paths
     * @param reportPaths
     *         The coverage report paths
     *
     * @return the created mapping with the SCM path as key
     */
    private Map<String, String> getScmToReportPathMapping(
            final Collection<String> scmPaths, final Collection<String> reportPaths) {
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
     * Verifies the passed mapping between SCM and coverage report paths.
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

    /**
     * Verifies that the mapping between the file paths of the current build and the former file paths of the reference
     * builds are clearly assigned to each other. This is done to prevent an unexpected behavior triggered by a third
     * party library in case that the code delta does not match with the coverage data.
     *
     * @param oldPathMapping
     *         The file path mapping
     * @param log
     *         The log
     *
     * @throws CodeDeltaException
     *         when the mapping is ambiguous
     */
    static void verifyOldPathMapping(final Map<String, String> oldPathMapping, final FilteredLog log)
            throws CodeDeltaException {
        Set<String> duplicates = StreamEx.of(oldPathMapping.values())
                .distinct(2)
                .collect(Collectors.toSet());
        Map<String, String> duplicateEntries = new HashMap<>();
        for (Map.Entry<String, String> entry : oldPathMapping.entrySet()) {
            for (String duplicate : duplicates) {
                if (entry.getValue().equals(duplicate)) {
                    duplicateEntries.put(entry.getKey(), duplicate);
                }
            }
        }
        if (!duplicates.isEmpty()) {
            String mismatches = duplicateEntries.entrySet().stream()
                    .limit(20) // prevent log overflows
                    .map(entry -> String.format("new: '%s' - former: '%s'", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining("," + System.lineSeparator()));
            String errorMessage = CODE_DELTA_TO_COVERAGE_DATA_MISMATCH_ERROR_TEMPLATE + System.lineSeparator() + mismatches;
            throw new CodeDeltaException(errorMessage);
        }
        log.logInfo("Successfully verified that the coverage data matches with the code delta");
    }
}
