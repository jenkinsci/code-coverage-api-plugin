package io.jenkins.plugins.coverage.model.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.hm.hafner.util.FilteredLog;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.FileCoverageNode;
import io.jenkins.plugins.forensics.delta.model.Change;
import io.jenkins.plugins.forensics.delta.model.ChangeEditType;
import io.jenkins.plugins.forensics.delta.model.FileChanges;

/**
 * Calculates and attaches values to the {@link FileCoverageNode nodes} of the coverage tree which represent the changes
 * concerning code and coverage.
 *
 * @author Florian Orendi
 */
public class FileCoverageDeltaProcessor {

    /**
     * Attaches the changed code lines to the file nodes of the coverage tree.
     *
     * @param coverageNode
     *         The root node of the coverage tree
     * @param codeChanges
     *         The code changes to be attached
     */
    public void attachChangedCodeLines(final CoverageNode coverageNode, final Map<String, FileChanges> codeChanges) {
        Map<String, CoverageNode> nodePathMapping = coverageNode.getAllFileCoverageNodes().stream()
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        codeChanges.forEach((path, fileChange) -> {
            if (nodePathMapping.containsKey(path)) {
                CoverageNode changedNode = nodePathMapping.get(path);
                if (changedNode instanceof FileCoverageNode) {
                    attachChanges((FileCoverageNode) changedNode, fileChange.getChangesByType(ChangeEditType.INSERT));
                    attachChanges((FileCoverageNode) changedNode, fileChange.getChangesByType(ChangeEditType.REPLACE));
                }
            }
        });
    }

    /**
     * Attaches a set of changes to a specific {@link FileCoverageNode node}.
     *
     * @param changedNode
     *         The node which contains code changes
     * @param relevantChanges
     *         The relevant changes
     */
    private void attachChanges(final FileCoverageNode changedNode, final Set<Change> relevantChanges) {
        for (Change change : relevantChanges) {
            for (int i = change.getFromLine(); i <= change.getToLine(); i++) {
                changedNode.addChangedCodeLine(i);
            }
        }
    }

    /**
     * Attaches all found indirect coverage changes within the coverage tree, compared to a reference tree.
     *
     * @param node
     *         The root of the tree in which indirect coverage changes are searched
     * @param referenceNode
     *         The root of the reference tree
     * @param codeChanges
     *         The code changes that has been applied between the two commits underlying the node and its reference
     * @param log
     *         The log
     */
    public void attachIndirectCoveragesChanges(final CoverageNode node, final CoverageNode referenceNode,
            final Map<String, FileChanges> codeChanges, final FilteredLog log) {
        log.logInfo("Obtaining indirect coverage changes...");
        Map<String, FileCoverageNode> fileNodes = node.getAllFileCoverageNodes().stream()
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        Map<String, FileCoverageNode> referenceFileNodes = referenceNode.getAllFileCoverageNodes().stream()
                .filter(reference -> fileNodes.containsKey(reference.getPath()))
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        for (Map.Entry<String, FileCoverageNode> entry : fileNodes.entrySet()) {
            String path = entry.getKey();
            FileCoverageNode fileNode = entry.getValue();
            Optional<SortedMap<Integer, Coverage>> referenceCoveragePerLine =
                    getReferenceCoveragePerLine(referenceFileNodes, path);
            if (referenceCoveragePerLine.isPresent()) {
                SortedMap<Integer, Coverage> referenceCoverageMapping = new TreeMap<>(referenceCoveragePerLine.get());
                if (codeChanges.containsKey(path)) {
                    adjustedCoveragePerLine(referenceCoverageMapping, codeChanges.get(path));
                }
                fileNode.getCoveragePerLine().forEach((line, coverage) -> {
                    if (!fileNode.getChangedCodeLines().contains(line)) {
                        Coverage referenceCoverage = referenceCoverageMapping.get(line);
                        int covered = coverage.getCovered();
                        int referenceCovered = referenceCoverage.getCovered();
                        if (covered != referenceCovered) {
                            fileNode.putIndirectCoverageChange(line, covered - referenceCovered);
                        }
                    }
                });
            }
        }
    }

    /**
     * Gets the coverage, mapped by the line within a file, for a reference file, represented by its fully qualified
     * name.
     *
     * @param references
     *         All possible reference
     * @param fullyQualifiedName
     *         The fully qualified name of the file for which the coverage per line is required
     *
     * @return an Optional of the coverage mapping if existent, else an empty Optional
     */
    private Optional<SortedMap<Integer, Coverage>> getReferenceCoveragePerLine(
            final Map<String, FileCoverageNode> references, final String fullyQualifiedName) {
        if (references.containsKey(fullyQualifiedName)) {
            SortedMap<Integer, Coverage> coveragePerLine = references.get(fullyQualifiedName).getCoveragePerLine();
            if (coveragePerLine != null && !coveragePerLine.isEmpty()) {
                return Optional.of(coveragePerLine);
            }
        }
        return Optional.empty();
    }

    /**
     * Adjusts a coverage-per-line mapping of a file before changes has been applied so that the coverage values can be
     * compared to the coverage-per-line mapping after code changes within the file.
     *
     * @param coveragePerLine
     *         The coverage-per-line mapping of the file before the changes which should be adjusted
     * @param fileChanges
     *         The applied code changes of the file
     */
    private void adjustedCoveragePerLine(final SortedMap<Integer, Coverage> coveragePerLine,
            final FileChanges fileChanges) {
        List<List<Coverage>> coverages = coveragePerLine.values().stream()
                .map(coverage -> new ArrayList<>(Collections.singletonList(coverage)))
                .collect(Collectors.toList());

        IntStream.range(0, coveragePerLine.lastKey() + 1)
                .filter(line -> !coveragePerLine.containsKey(line))
                .forEach(line -> {
                    if (line < coverages.size()) {
                        coverages.add(line, new ArrayList<>(Collections.singletonList(null)));
                    }
                    else {
                        coverages.add(new ArrayList<>(Collections.singletonList(null)));
                    }
                });

        fileChanges.getChangesByType(ChangeEditType.DELETE).forEach(change -> {
            for (int i = change.getChangedFromLine(); i <= change.getChangedToLine(); i++) {
                coverages.get(i).clear();
            }
        });

        fileChanges.getChangesByType(ChangeEditType.INSERT).forEach(change -> {
            List<Coverage> inserted = coverages.get(change.getChangedFromLine());
            for (int i = change.getFromLine(); i <= change.getToLine(); i++) {
                inserted.add(null);
            }
        });

        fileChanges.getChangesByType(ChangeEditType.REPLACE).forEach(change -> {
            List<Coverage> replaced = coverages.get(change.getChangedFromLine());
            replaced.clear(); // coverage of replaced code is irrelevant
            for (int i = change.getFromLine(); i <= change.getToLine(); i++) {
                replaced.add(null);
            }
            for (int i = change.getChangedFromLine() + 1; i <= change.getChangedToLine(); i++) {
                coverages.get(i).clear();
            }
        });

        List<Coverage> adjustedCoveragesList = coverages.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        coveragePerLine.clear();
        for (int line = 1; line < adjustedCoveragesList.size(); line++) {
            Coverage coverage = adjustedCoveragesList.get(line);
            if (coverage != null) {
                coveragePerLine.put(line, coverage);
            }
        }
    }
}
