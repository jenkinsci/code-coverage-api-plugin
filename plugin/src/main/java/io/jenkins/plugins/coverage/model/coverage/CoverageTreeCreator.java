package io.jenkins.plugins.coverage.model.coverage;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageLeaf;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.FileCoverageNode;

public class CoverageTreeCreator {

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

    public CoverageNode createUnexpectedCoverageChangesTree(final CoverageNode coverageNode) {
        CoverageNode copy = coverageNode.copyTree();

        boolean treeExists = calculateUnexpectedCoverageChangesTree(copy);
        if (treeExists) {
            attachUnexpectedCoverageChangesLeaves(copy);
        }
        else {
            clearChildrenAndLeaves(copy);
        }

        return copy;
    }

    private boolean calculateChangeCoverageTree(final CoverageNode root) {
        boolean hasChanged = false;
        if (root instanceof FileCoverageNode) {
            FileCoverageNode fileNode = (FileCoverageNode) root;
            clearChildrenAndLeaves(fileNode);
            // this is required since there might be changes which do not effect the code coverage -> ignore these files
            return fileNode.getCoveragePerLine().keySet().stream()
                    .anyMatch(line -> fileNode.getChangedCodeLines().contains(line));
        }
        Iterator<CoverageNode> nodeIterator = root.getChildren().iterator();
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

    private boolean calculateUnexpectedCoverageChangesTree(final CoverageNode root) {
        boolean hasChangedCoverage = false;
        if (root instanceof FileCoverageNode) {
            clearChildrenAndLeaves(root);
            return !((FileCoverageNode) root).getUnexpectedCoverageChanges().isEmpty();
        }
        Iterator<CoverageNode> nodeIterator = root.getChildren().iterator();
        while (nodeIterator.hasNext()) {
            CoverageNode child = nodeIterator.next();
            boolean childHasChangedCoverage = calculateUnexpectedCoverageChangesTree(child);
            if (!childHasChangedCoverage) {
                nodeIterator.remove();
            }
            hasChangedCoverage |= childHasChangedCoverage;
        }
        return hasChangedCoverage;
    }

    private void attachUnexpectedCoverageChangesLeaves(final CoverageNode node) {
        node.getAll(CoverageMetric.FILE).stream()
                .map(fileNode -> (FileCoverageNode) fileNode)
                .forEach(fileNode -> {
                    List<Coverage> changes = fileNode.getCoveragePerLine()
                            .entrySet().stream()
                            .filter(entry -> fileNode.getUnexpectedCoverageChanges().containsKey(entry.getKey()))
                            .map(Entry::getValue)
                            .collect(Collectors.toList());
                    createLeaves(fileNode, changes);
                });
    }

    private void attachChangeCoverageLeaves(final CoverageNode node) {
        node.getAll(CoverageMetric.FILE).stream()
                .map(fileNode -> (FileCoverageNode) fileNode)
                .forEach(fileNode -> {
                    List<Coverage> changes = fileNode.getCoveragePerLine()
                            .entrySet().stream()
                            .filter(entry -> fileNode.getChangedCodeLines().contains(entry.getKey()))
                            .map(Entry::getValue)
                            .collect(Collectors.toList());
                    createLeaves(fileNode, changes);
                });
    }

    private void createLeaves(final FileCoverageNode fileNode, final List<Coverage> changes) {
        Coverage lineCoverage = Coverage.NO_COVERAGE;
        Coverage branchCoverage = Coverage.NO_COVERAGE;
        for (Coverage change : changes) {
            if (change.getTotal() > 1) {
                branchCoverage = lineCoverage.add(new Coverage(change.getCovered(), change.getMissed()));
            }
            int covered = change.getCovered() > 0 ? 1 : 0;
            int missed = change.getMissed() > 0 ? 1 : 0;
            lineCoverage = lineCoverage.add(new Coverage(covered, missed));

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

    private void clearChildrenAndLeaves(final CoverageNode coverageNode) {
        coverageNode.getChildren().clear();
        coverageNode.getLeaves().clear();
    }
}
