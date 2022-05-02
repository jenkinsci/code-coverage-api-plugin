package io.jenkins.plugins.coverage.model.visualization.tree;

import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;

/**
 * Converts a tree of {@link CoverageNode coverage nodes} to a corresponding tree of {@link TreeMapNode ECharts tree map
 * nodes}.
 *
 * @author Ullrich Hafner
 */
public class TreeMapNodeConverter {

    private final ColorProvider colorProvider;

    /**
     * Creates a converter which converts a {@link CoverageNode} to a tree map of {@link TreeMapNode}.
     *
     * @param colorProvider
     *         The {@link ColorProvider provider} which provides the colors used by the tree map
     */
    public TreeMapNodeConverter(final ColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    /**
     * Converts a coverage tree of {@link CoverageNode} to a ECharts tree map of {@link TreeMapNode}.
     *
     * @param node
     *         The root node of the tree to be converted
     *
     * @return the converted tree map representation
     */
    public TreeMapNode toTeeChartModel(final CoverageNode node) {
        TreeMapNode root = toTreeMapNode(node);
        for (TreeMapNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeMapNode toTreeMapNode(final CoverageNode node) {
        Coverage coverage = node.getCoverage(CoverageMetric.LINE);

        double coveragePercentage = coverage.getCoveredPercentage().getDoubleValue();

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
