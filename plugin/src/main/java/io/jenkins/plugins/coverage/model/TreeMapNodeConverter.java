package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageColorizationLevel;

/**
 * Converts a tree of {@link CoverageNode coverage nodes} to a corresponding tree of {@link TreeMapNode ECharts tree map
 * nodes}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverter {
    TreeMapNode toTeeChartModel(final CoverageNode node) {
        TreeMapNode root = toTreeMapNode(node);
        for (TreeMapNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeMapNode toTreeMapNode(final CoverageNode node) {
        Coverage coverage = node.getCoverage(CoverageMetric.LINE);

        String color = ColorUtils.colorAsHex(
                CoverageColorizationLevel
                        .getDisplayColorsOfCoveragePercentage(coverage.getRoundedPercentage() * 100.0)
                        .getFillColor()
        );

        TreeMapNode treeNode = new TreeMapNode(node.getName(), color, coverage.getTotal(), coverage.getCovered());
        if (node.getMetric().equals(CoverageMetric.FILE)) {
            return treeNode;
        }

        node.getChildren().stream()
                .map(this::toTreeMapNode)
                .forEach(treeNode::insertNode);
        return treeNode;
    }
}
