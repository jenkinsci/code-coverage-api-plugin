package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.model.util.FractionFormatter;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;

/**
 * Converts a tree of {@link CoverageNode coverage nodes} to a corresponding tree of {@link TreeMapNode ECharts tree map
 * nodes}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverter {

    private final ColorProvider colorProvider;

    TreeMapNodeConverter(final ColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    TreeMapNode toTeeChartModel(final CoverageNode node) {
        TreeMapNode root = toTreeMapNode(node);
        for (TreeMapNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeMapNode toTreeMapNode(final CoverageNode node) {
        Coverage coverage = node.getCoverage(CoverageMetric.LINE);

        double coveragePercentage = FractionFormatter
                .transformFractionToPercentage(coverage.getCoveredPercentage())
                .doubleValue();

        String color = CoverageLevel
                .getDisplayColorsOfCoverageLevel(coveragePercentage, colorProvider)
                .getFillColorAsHex();

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
