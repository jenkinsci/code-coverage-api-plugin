package io.jenkins.plugins.coverage.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

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
    public Node createChangeCoverageTree(final Node coverageNode) {
        Node copy = coverageNode.copyTree();

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
    public Node createIndirectCoverageChangesTree(final Node coverageNode) {
        Node copy = coverageNode.copyTree();

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
     *         The {@link Node root} of the tree
     *
     * @return {@code true} whether the tree has been calculated successfully, else {@link false}
     */
    private boolean calculateChangeCoverageTree(final Node root) {
        if (root instanceof FileNode) {
            FileNode fileNode = (FileNode) root;
            clearChildrenAndLeaves(fileNode);
            // this is required since there might be changes which do not affect the code coverage -> ignore these files
            return fileNode.hasCoveredLinesInChangeSet();
        }
        var children = root.getChildren();
        Iterator<Node> nodeIterator = children.iterator();
        boolean hasChanged = false;
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.next();
            boolean hasChildChanges = calculateChangeCoverageTree(child);
            if (!hasChildChanges) {
                nodeIterator.remove();
            }
            hasChanged |= hasChildChanges;
        }
        root.clearChildren();
        root.addAllChildren(children);
        return hasChanged;
    }

    /**
     * Recursively calculates a coverage tree which represents the indirect coverage changes.
     *
     * @param root
     *         The {@link Node root} of the tree
     *
     * @return {@code true} whether the tree has been calculated successfully, else {@link false}
     */
    private boolean calculateIndirectCoverageChangesTree(final Node root) {
        if (root instanceof FileNode) {
            clearChildrenAndLeaves(root);
            return ((FileNode) root).hasIndirectCoverageChanges();
        }
        var children = root.getChildren();
        Iterator<Node> nodeIterator = children.iterator();
        boolean hasChangedCoverage = false;
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.next();
            boolean childHasChangedCoverage = calculateIndirectCoverageChangesTree(child);
            if (!childHasChangedCoverage) {
                nodeIterator.remove();
            }
            hasChangedCoverage |= childHasChangedCoverage;
        }
        root.clearChildren();
        root.addAllChildren(children);
        return hasChangedCoverage;
    }

    /**
     * Attaches leaves to the given {@link Node node} which represent its underlying change coverage.
     *
     * @param node
     *         The node which contains the change coverage
     */
    private void attachChangeCoverageLeaves(final Node node) {
        node.getAllFileNodes()
                .forEach(fileNode -> createChangeCoverageLeaves(fileNode, fileNode.getCoverageOfChangeSet()));
    }

    /**
     * Attaches leaves to the passed {@link Node node} which represent its underlying indirect coverage
     * changes.
     *
     * @param node
     *         The node which contains indirect coverage changes
     */
    private void attachIndirectCoverageChangesLeaves(final Node node) {
        node.getAllFileNodes().stream()
                .filter(FileNode::hasIndirectCoverageChanges)
                .forEach(this::createIndirectCoverageChangesLeaves);
    }

    /**
     * Creates both a line and a branch change coverage leaf for the given {@link FileNode node}.
     *
     * @param fileNode
     *         The node the leaves are attached to
     * @param changes
     *         The {@link Coverage} to be represented by the leaves
     */
    private void createChangeCoverageLeaves(final FileNode fileNode, final List<Coverage> changes) {
        Coverage lineCoverage = Coverage.nullObject(Metric.LINE);
        Coverage branchCoverage = Coverage.nullObject(Metric.BRANCH);

        CoverageBuilder builder = new CoverageBuilder();
        for (Coverage change : changes) {
            int covered = change.getCovered() > 0 ? 1 : 0;
            if (change.getTotal() > 1) {
                builder.setMetric(Metric.BRANCH).setCovered(change.getCovered()).setMissed(change.getMissed());
                branchCoverage = branchCoverage.add(builder.build());
                builder.setMetric(Metric.LINE).setCovered(covered).setMissed(1 - covered);
                lineCoverage = lineCoverage.add(builder.build());
            }
            else {
                int missed = change.getMissed() > 0 ? 1 : 0;
                builder.setMetric(Metric.LINE).setCovered(covered).setMissed(missed);
                lineCoverage = lineCoverage.add(builder.build());
            }
        }
        if (lineCoverage.isSet()) {
            fileNode.addValue(lineCoverage);
        }
        if (branchCoverage.isSet()) {
            fileNode.addValue(branchCoverage);
        }
    }

    /**
     * Creates both a line and a branch indirect coverage changes leaf for the passed {@link FileNode node}. The
     * leaves represent the delta for a file regarding the amount of lines / branches that got hit by tests.
     *
     * @param fileNode
     *         The node the leaves are attached to
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    // there is no useful possibility for outsourcing code
    private void createIndirectCoverageChangesLeaves(final FileNode fileNode) {
        Coverage lineCoverage = Coverage.nullObject(Metric.LINE);
        Coverage branchCoverage = Coverage.nullObject(Metric.BRANCH);
        for (Map.Entry<Integer, Integer> change : fileNode.getIndirectCoverageChanges().entrySet()) {
            int delta = change.getValue();
            Coverage currentLineCoverage = fileNode.getLineCoverage(change.getKey());
            Coverage currentBranchCoverage = fileNode.getBranchCoverage(change.getKey());
            CoverageBuilder builder = new CoverageBuilder();
            if (delta > 0) {
                // the line is fully covered - even in case of branch coverage
                if (delta == currentLineCoverage.getCovered()) {
                    builder.setMetric(Metric.LINE).setCovered(1).setMissed(0);
                    lineCoverage = lineCoverage.add(builder.build());
                }
                // the branch coverage increased for 'delta' hits
                if (currentBranchCoverage.getTotal() > 1) {
                    builder.setMetric(Metric.BRANCH).setCovered(delta).setMissed(0);
                    branchCoverage = branchCoverage.add(builder.build());
                }
            }
            else if (delta < 0) {
                // the line is not covered anymore
                if (currentLineCoverage.getCovered() == 0) {
                    builder.setMetric(Metric.LINE).setCovered(0).setMissed(1);
                    lineCoverage = lineCoverage.add(builder.build());
                }
                // the branch coverage is decreased by 'delta' hits
                if (currentBranchCoverage.getTotal() > 1) {
                    builder.setMetric(Metric.BRANCH).setCovered(0).setMissed(Math.abs(delta));
                }
            }
        }
        if (lineCoverage.isSet()) {
            fileNode.addValue(lineCoverage);
        }
        if (branchCoverage.isSet()) {
            fileNode.addValue(branchCoverage);
        }
    }

    /**
     * Clears all leaves and children of the passed {@link Node}.
     *
     * @param coverageNode
     *         The processed node
     */
    // TODO: can't we create a new tree?
    private void clearChildrenAndLeaves(final Node coverageNode) {
        coverageNode.clear();
    }
}
