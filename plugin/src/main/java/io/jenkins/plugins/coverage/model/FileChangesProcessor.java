package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.math.Fraction;

import io.jenkins.plugins.forensics.delta.model.Change;
import io.jenkins.plugins.forensics.delta.model.ChangeEditType;
import io.jenkins.plugins.forensics.delta.model.FileChanges;

/**
 * Calculates and attaches values to the {@link FileCoverageNode nodes} of the coverage tree which represent the changes
 * concerning code and coverage.
 *
 * @author Florian Orendi
 */
public class FileChangesProcessor {

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
     * Attaches the delta between the total file coverage of all currently built files against the passed reference.
     *
     * @param root
     *         The root of the coverage tree
     * @param referenceNode
     *         The root of the reference coverage tree
     */
    public void attachFileCoverageDeltas(final CoverageNode root, final CoverageNode referenceNode) {
        Map<String, FileCoverageNode> fileNodes = root.getAllFileCoverageNodes().stream()
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));
        Map<String, FileCoverageNode> referenceFileNodes = getReferenceFileNodeMapping(fileNodes, referenceNode);
        root.getAllFileCoverageNodes().stream()
                .filter(node -> referenceFileNodes.containsKey(node.getPath()))
                .forEach(node -> attachFileCoverageDelta(node, referenceFileNodes.get(node.getPath())));
    }

    /**
     * Attaches the delta between the total coverage of a file against the same file from the reference build.
     *
     * @param fileNode
     *         The {@link FileCoverageNode node} which represents the total coverage of a file
     * @param referenceNode
     *         The {@link FileCoverageNode reference node} which represents the coverage of the reference file
     */
    private void attachFileCoverageDelta(final FileCoverageNode fileNode, final FileCoverageNode referenceNode) {
        SortedMap<CoverageMetric, Fraction> referenceCoverage = referenceNode.getMetricFractions();
        fileNode.getMetricFractions().forEach((metric, value) -> {
            if (referenceCoverage.containsKey(metric)) {
                Fraction delta = value.subtract(referenceCoverage.get(metric));
                fileNode.putFileCoverageDelta(metric, CoveragePercentage.getCoveragePercentage(delta));
            }
        });
    }

    /**
     * Attaches all found indirect coverage changes within the coverage tree, compared to a reference tree.
     *
     * @param root
     *         The root of the tree in which indirect coverage changes are searched
     * @param referenceNode
     *         The root of the reference tree
     * @param codeChanges
     *         The code changes that has been applied between the two commits underlying the node and its reference
     * @param oldPathMapping
     *         A mapping between the report paths of the current and the reference coverage tree
     */
    public void attachIndirectCoveragesChanges(final CoverageNode root, final CoverageNode referenceNode,
            final Map<String, FileChanges> codeChanges, final Map<String, String> oldPathMapping) {
        // current nodes mapped by the corresponding old paths from the reference report
        Map<String, FileCoverageNode> fileNodes = root.getAllFileCoverageNodes().stream()
                .filter(node -> oldPathMapping.containsKey(node.getPath()))
                .collect(Collectors.toMap(node -> oldPathMapping.get(node.getPath()), Function.identity()));

        Map<String, FileCoverageNode> referenceFileNodes = getReferenceFileNodeMapping(fileNodes, referenceNode);

        for (Map.Entry<String, FileCoverageNode> entry : fileNodes.entrySet()) {
            String referencePath = entry.getKey();
            FileCoverageNode fileNode = entry.getValue();
            Optional<SortedMap<Integer, Coverage>> referenceCoveragePerLine =
                    getReferenceCoveragePerLine(referenceFileNodes, referencePath);
            if (referenceCoveragePerLine.isPresent()) {
                SortedMap<Integer, Coverage> referenceCoverageMapping = new TreeMap<>(referenceCoveragePerLine.get());
                String currentPath = fileNode.getPath();
                if (codeChanges.containsKey(currentPath)) {
                    adjustedCoveragePerLine(referenceCoverageMapping,
                            codeChanges.get(currentPath));
                }
                attachIndirectCoverageChangeForFile(fileNode, referenceCoverageMapping);
            }
        }
    }

    /**
     * Attaches the indirect coverage changes for a specific file, represented by the passed {@link FileCoverageNode}.
     *
     * @param fileNode
     *         The file coverage node which represents the processed file
     * @param referenceCoverageMapping
     *         A mapping which contains the coverage per line of the reference file
     */
    private void attachIndirectCoverageChangeForFile(final FileCoverageNode fileNode,
            final SortedMap<Integer, Coverage> referenceCoverageMapping) {
        fileNode.getCoveragePerLine().forEach((line, coverage) -> {
            if (!fileNode.getChangedCodeLines().contains(line) && referenceCoverageMapping.containsKey(line)) {
                Coverage referenceCoverage = referenceCoverageMapping.get(line);
                int covered = coverage.getCovered();
                int referenceCovered = referenceCoverage.getCovered();
                if (covered != referenceCovered) {
                    fileNode.putIndirectCoverageChange(line, covered - referenceCovered);
                }
            }
        });
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
        List<List<Coverage>> coverages = transformCoveragePerLine(coveragePerLine, fileChanges);

        fileChanges.getChangesByType(ChangeEditType.DELETE).forEach(change -> {
            for (int i = change.getChangedFromLine(); i <= change.getChangedToLine(); i++) {
                coverages.get(i).clear();
            }
        });

        fileChanges.getChangesByType(ChangeEditType.INSERT).forEach(change -> {
            List<Coverage> inserted = coverages.get(change.getChangedFromLine());
            int changedLinesNumber = change.getToLine() - change.getFromLine() + 1;
            fillCoverageListWithNull(inserted, changedLinesNumber);
        });

        fileChanges.getChangesByType(ChangeEditType.REPLACE).forEach(change -> {
            List<Coverage> replaced = coverages.get(change.getChangedFromLine());
            replaced.clear(); // coverage of replaced code is irrelevant
            int changedLinesNumber = change.getToLine() - change.getFromLine() + 1;
            fillCoverageListWithNull(replaced, changedLinesNumber);
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

    /**
     * Transforms a coverage-per-line mapping to a list representation which can be expanded without influencing the
     * original line numbers.
     *
     * @param coveragePerLine
     *         The coverage-per-line mapping of the file before the changes which should be adjusted
     * @param fileChanges
     *         The applied code changes of the file
     *
     * @return the list expandable list representation of the coverage-per-line mapping
     */
    private List<List<Coverage>> transformCoveragePerLine(
            final SortedMap<Integer, Coverage> coveragePerLine, final FileChanges fileChanges) {
        List<List<Coverage>> coverages = coveragePerLine.values().stream()
                .map(coverage -> new ArrayList<>(Collections.singletonList(coverage)))
                .collect(Collectors.toList());

        // the highest covered line might not be the highest line which contains changes
        int maxLineNumber = coveragePerLine.lastKey();
        Optional<Integer> highestLineNumber = fileChanges.getChanges().values().stream()
                .flatMap(Set::stream)
                .map(Change::getChangedToLine)
                .max(Comparator.naturalOrder());
        if (highestLineNumber.isPresent() && highestLineNumber.get() > maxLineNumber) {
            maxLineNumber = highestLineNumber.get();
        }

        IntStream.range(0, maxLineNumber + 1)
                .filter(line -> !coveragePerLine.containsKey(line))
                .forEach(line -> {
                    if (line < coverages.size()) {
                        coverages.add(line, new ArrayList<>(Collections.singletonList(null)));
                    }
                    else {
                        coverages.add(new ArrayList<>(Collections.singletonList(null)));
                    }
                });

        return coverages;
    }

    /**
     * Gets all {@link FileCoverageNode file nodes} from a reference coverage tree which also exist in the current
     * coverage tree. The found nodes are mapped by their path.
     *
     * @param nodeMapping
     *         The file nodes of the current coverage tree, mapped by their paths
     * @param referenceNode
     *         The root of the reference coverage tree
     *
     * @return the created node mapping
     */
    private Map<String, FileCoverageNode> getReferenceFileNodeMapping(
            final Map<String, FileCoverageNode> nodeMapping, final CoverageNode referenceNode) {
        return referenceNode.getAllFileCoverageNodes().stream()
                .filter(reference -> nodeMapping.containsKey(reference.getPath()))
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));
    }

    /**
     * Adds {@code null} values to the passed list.
     *
     * @param coverageList
     *         The list which should be filled with {@code null}
     * @param number
     *         The number of values to be inserted
     */
    private void fillCoverageListWithNull(final List<Coverage> coverageList, final int number) {
        for (int i = 0; i < number; i++) {
            coverageList.add(null);
        }
    }
}
