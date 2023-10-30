package io.jenkins.plugins.coverage.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;

/**
 * Creates coverage trees which represent different types of coverage.
 *
 * @author Florian Orendi
 */
public class CoverageTreeCreator {

    /**
     * Creates a coverage tree which represents the change coverage.
     *
     * @param coverageNode
     *         The root of the origin coverage tree
     *
     * @return the filtered tree
     */
    public CoverageNode createChangeCoverageTree(final CoverageNode coverageNode) {
        CoverageNode copy = coverageNode.copyTree();

        boolean treeExists = calculateChangeCoverageTree(copy);
        if (treeExists) {
            attachChangeCoverageLeaves(copy);
        }
        else {
            clearChildrenAndLeaves(copy);
        }

        return copy;
    }

    /**
     * Creates a coverage tree which represents the indirect coverage changes.
     *
     * @param coverageNode
     *         The root of the origin coverage tree
     *
     * @return the filtered tree
     */
    public CoverageNode createIndirectCoverageChangesTree(final CoverageNode coverageNode) {
        CoverageNode copy = coverageNode.copyTree();

        boolean treeExists = calculateIndirectCoverageChangesTree(copy);
        if (treeExists) {
            attachIndirectCoverageChangesLeaves(copy);
        }
        else {
            clearChildrenAndLeaves(copy);
        }

        return copy;
    }

    /**
     * Recursively calculates a coverage tree which represents the change coverage.
     *
     * @param root
     *         The {@link CoverageNode root} of the tree
     *
     * @return {@code true} whether the tree has been calculated successfully, else {@link false}
     */
    private boolean calculateChangeCoverageTree(final CoverageNode root) {
        if (root instanceof FileCoverageNode) {
            FileCoverageNode fileNode = (FileCoverageNode) root;
            clearChildrenAndLeaves(fileNode);
            // this is required since there might be changes which do not effect the code coverage -> ignore these files
            return fileNode.getCoveragePerLine().keySet().stream()
                    .anyMatch(line -> fileNode.getChangedCodeLines().contains(line));
        }
        Iterator<CoverageNode> nodeIterator = root.getChildren().iterator();
        boolean hasChanged = false;
        while (nodeIterator.hasNext()) {
            CoverageNode child = nodeIterator.next();
            boolean childHasChanged = calculateChangeCoverageTree(child);
            if (!childHasChanged) {
                nodeIterator.remove();
            }
            hasChanged |= childHasChanged;
        }
        return hasChanged;
    }

    /**
     * Recursively calculates a coverage tree which represents the indirect coverage changes.
     *
     * @param root
     *         The {@link CoverageNode root} of the tree
     *
     * @return {@code true} whether the tree has been calculated successfully, else {@link false}
     */
    private boolean calculateIndirectCoverageChangesTree(final CoverageNode root) {
        if (root instanceof FileCoverageNode) {
            clearChildrenAndLeaves(root);
            return !((FileCoverageNode) root).getIndirectCoverageChanges().isEmpty();
        }
        Iterator<CoverageNode> nodeIterator = root.getChildren().iterator();
        boolean hasChangedCoverage = false;
        while (nodeIterator.hasNext()) {
            CoverageNode child = nodeIterator.next();
            boolean childHasChangedCoverage = calculateIndirectCoverageChangesTree(child);
            if (!childHasChangedCoverage) {
                nodeIterator.remove();
            }
            hasChangedCoverage |= childHasChangedCoverage;
        }
        return hasChangedCoverage;
    }

    /**
     * Attaches leaves to the passed {@link CoverageNode node} which represent its underlying change coverage.
     *
     * @param node
     *         The node which contains the change coverage
     */
    private void attachChangeCoverageLeaves(final CoverageNode node) {
        node.getAllFileCoverageNodes()
                .forEach(fileNode -> {
                    List<Coverage> changes = fileNode.getCoveragePerLine()
                            .entrySet().stream()
                            .filter(entry -> fileNode.getChangedCodeLines().contains(entry.getKey()))
                            .map(Entry::getValue)
                            .collect(Collectors.toList());
                    createChangeCoverageLeaves(fileNode, changes);
                });
    }

    /**
     * Attaches leaves to the passed {@link CoverageNode node} which represent its underlying indirect coverage
     * changes.
     *
     * @param node
     *         The node which contains indirect coverage changes
     */
    private void attachIndirectCoverageChangesLeaves(final CoverageNode node) {
        node.getAllFileCoverageNodes().stream()
                .filter(fileNode -> !fileNode.getIndirectCoverageChanges().isEmpty())
                .forEach(this::createIndirectCoverageChangesLeaves);
    }

    /**
     * Creates both a line and a branch change coverage leaf for the passed {@link FileCoverageNode node}.
     *
     * @param fileNode
     *         The node the leaves are attached to
     * @param changes
     *         The {@link Coverage} to be represented by the leaves
     */
    private void createChangeCoverageLeaves(final FileCoverageNode fileNode, final List<Coverage> changes) {
        Coverage lineCoverage = CoverageBuilder.NO_COVERAGE;
        Coverage branchCoverage = CoverageBuilder.NO_COVERAGE;
        for (Coverage change : changes) {
            int covered = change.getCovered() > 0 ? 1 : 0;
            if (change.getTotal() > 1) {
                branchCoverage = branchCoverage.add(new Coverage.CoverageBuilder().setCovered(change.getCovered())
                        .setMissed(change.getMissed())
                        .build());
                lineCoverage = lineCoverage.add(
                        new Coverage.CoverageBuilder().setCovered(covered).setMissed(1 - covered).build());
            }
            else {
                int missed = change.getMissed() > 0 ? 1 : 0;
                lineCoverage = lineCoverage.add(
                        new Coverage.CoverageBuilder().setCovered(covered).setMissed(missed).build());
            }
        }
        if (lineCoverage.isSet()) {
            CoverageLeaf lineCoverageLeaf = new CoverageLeaf(CoverageMetric.LINE, lineCoverage);
            fileNode.add(lineCoverageLeaf);
        }
        if (branchCoverage.isSet()) {
            CoverageLeaf branchCoverageLeaf = new CoverageLeaf(CoverageMetric.BRANCH, branchCoverage);
            fileNode.add(branchCoverageLeaf);
        }
    }

    /**
     * Creates both a line and a branch indirect coverage changes leaf for the passed {@link FileCoverageNode node}. The
     * leaves represent the delta for a file regarding the amount of lines / branches that got hit by tests.
     *
     * @param fileNode
     *         The node the leaves are attached to
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    // there is no useful possibility for outsourcing code
    private void createIndirectCoverageChangesLeaves(final FileCoverageNode fileNode) {
        Coverage lineCoverage = CoverageBuilder.NO_COVERAGE;
        Coverage branchCoverage = CoverageBuilder.NO_COVERAGE;
        for (Map.Entry<Integer, Integer> change : fileNode.getIndirectCoverageChanges().entrySet()) {
            int delta = change.getValue();
            Coverage currentCoverage = fileNode.getCoveragePerLine().get(change.getKey());
            if (delta > 0) {
                // the line is fully covered - even in case of branch coverage
                if (delta == currentCoverage.getCovered()) {
                    lineCoverage = lineCoverage.add(new Coverage.CoverageBuilder().setCovered(1).setMissed(0).build());
                }
                // the branch coverage increased for 'delta' hits
                if (currentCoverage.getTotal() > 1) {
                    branchCoverage = branchCoverage.add(
                            new Coverage.CoverageBuilder().setCovered(delta).setMissed(0).build());
                }
            }
            else if (delta < 0) {
                // the line is not covered anymore
                if (currentCoverage.getCovered() == 0) {
                    lineCoverage = lineCoverage.add(new Coverage.CoverageBuilder().setCovered(0).setMissed(1).build());
                }
                // the branch coverage is decreased by 'delta' hits
                if (currentCoverage.getTotal() > 1) {
                    branchCoverage = branchCoverage.add(
                            new Coverage.CoverageBuilder().setCovered(0).setMissed(Math.abs(delta)).build());
                }
            }
        }
        if (lineCoverage.isSet()) {
            CoverageLeaf lineCoverageLeaf = new CoverageLeaf(CoverageMetric.LINE, lineCoverage);
            fileNode.add(lineCoverageLeaf);
        }
        if (branchCoverage.isSet()) {
            CoverageLeaf branchCoverageLeaf = new CoverageLeaf(CoverageMetric.BRANCH, branchCoverage);
            fileNode.add(branchCoverageLeaf);
        }
    }

    /**
     * Clears all leaves and children of the passed {@link CoverageNode}.
     *
     * @param coverageNode
     *         The processed node
     */
    private void clearChildrenAndLeaves(final CoverageNode coverageNode) {
        coverageNode.getChildren().clear();
        coverageNode.getLeaves().clear();
    }
}
