package io.jenkins.plugins.coverage.metrics.visualization.charts;

import edu.hm.hafner.echarts.ItemStyle;
import edu.hm.hafner.echarts.Label;
import edu.hm.hafner.echarts.TreeMapNode;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageLevel;

/**
 * Converts a tree of {@link Node coverage nodes} to a corresponding tree of
 * {@link TreeMapNode ECharts tree map nodes}.
 *
 * @author Ullrich Hafner
 */
public class TreeMapNodeConverter {
    /**
     * Converts a coverage tree of {@link Node nodes} to an ECharts tree map of {@link TreeMapNode}.
     *
     * @param node
     *         The root node of the tree to be converted
     * @param metric
     *         The coverage metric that should be represented (line and branch coverage are available)
     * @param colorProvider
     *         Provides the colors to be used for highlighting the tree nodes
     *
     * @return the converted tree map representation
     */
    public TreeMapNode toTeeChartModel(final Node node, final Metric metric, final ColorProvider colorProvider) {
        TreeMapNode root = toTreeMapNode(node, metric, colorProvider);
        for (TreeMapNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeMapNode toTreeMapNode(final Node node, final Metric metric,
            final ColorProvider colorProvider) {
        Coverage coverage = (Coverage)node.getValue(metric).orElse(Coverage.nullObject(metric));

        double coveragePercentage = coverage.getCoveredPercentage().doubleValue() * 100.0;

        DisplayColors colors = CoverageLevel.getDisplayColorsOfCoverageLevel(coveragePercentage, colorProvider);
        String lineColor = colors.getLineColorAsRGBHex();
        String fillColor = colors.getFillColorAsRGBHex();

        Label label = new Label(true, lineColor);
        Label upperLabel = new Label(true, lineColor);

        if (node instanceof FileNode) {
            ItemStyle style = new ItemStyle(fillColor);
            return new TreeMapNode(node.getName(), style, label, upperLabel, coverage.getTotal(), coverage.getCovered());
        }

        ItemStyle packageStyle = new ItemStyle(fillColor, fillColor, 4);
        TreeMapNode treeNode =
                new TreeMapNode(node.getName(), packageStyle, label, upperLabel, coverage.getTotal(),
                        coverage.getCovered());

        node.getChildren().stream()
                .map(n -> toTreeMapNode(n, metric, colorProvider))
                .forEach(treeNode::insertNode);
        return treeNode;
    }
}
