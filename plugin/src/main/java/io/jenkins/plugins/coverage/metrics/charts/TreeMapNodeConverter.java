package io.jenkins.plugins.coverage.metrics.charts;

import java.util.Optional;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.echarts.ItemStyle;
import edu.hm.hafner.echarts.Label;
import edu.hm.hafner.echarts.LabeledTreeMapNode;
import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.coverage.metrics.color.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.color.CoverageLevel;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;

/**
 * Converts a tree of {@link Node coverage nodes} to a corresponding tree of
 * {@link TreeMapNode ECharts tree map nodes}.
 *
 * @author Ullrich Hafner
 */
public class TreeMapNodeConverter {
    private static final ElementFormatter FORMATTER = new ElementFormatter();

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
    public LabeledTreeMapNode toTreeChartModel(final Node node, final Metric metric, final ColorProvider colorProvider) {
        var tree = mergePackages(node);
        LabeledTreeMapNode root = toTreeMapNode(tree, metric, colorProvider)
                .orElse(new LabeledTreeMapNode(getId(node), node.getName()));
        for (LabeledTreeMapNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private String getId(final Node node) {
        String id = node.getName();
        if (node.isRoot()) {
            return id;
        }
        else {
            return getId(node.getParent()) + '/' + id;
        }
    }

    private Node mergePackages(final Node node) {
        if (node instanceof ModuleNode) {
            ModuleNode copy = (ModuleNode) node.copyTree();
            copy.splitPackages();
            return copy;
        }
        return node;
    }

    private Optional<LabeledTreeMapNode> toTreeMapNode(final Node node, final Metric metric,
            final ColorProvider colorProvider) {
        var value = node.getValue(metric);
        if (value.isPresent()) {
            var rootValue = value.get();
            if (rootValue instanceof Coverage) {
                return Optional.of(createCoverageTree((Coverage) rootValue, colorProvider, node, metric));
            }
        }

        return Optional.empty();
    }

    private LabeledTreeMapNode createCoverageTree(final Coverage coverage, final ColorProvider colorProvider, final Node node,
            final Metric metric) {
        double coveragePercentage = coverage.getCoveredPercentage().toDouble();

        DisplayColors colors = CoverageLevel.getDisplayColorsOfCoverageLevel(coveragePercentage, colorProvider);
        String lineColor = colors.getLineColorAsRGBHex();
        String fillColor = colors.getFillColorAsRGBHex();

        Label label = new Label(true, lineColor);
        Label upperLabel = new Label(true, lineColor);

        var id = getId(node);
        if (node instanceof FileNode) {
            return new LabeledTreeMapNode(id, node.getName(), new ItemStyle(fillColor), label, upperLabel,
                    String.valueOf(coverage.getTotal()), FORMATTER.getTooltip(coverage));
        }

        ItemStyle packageStyle = new ItemStyle(fillColor, fillColor, 4);
        LabeledTreeMapNode treeNode = new LabeledTreeMapNode(id, node.getName(), packageStyle, label, upperLabel,
                String.valueOf(coverage.getTotal()), FORMATTER.getTooltip(coverage));

        node.getChildren().stream()
                .map(n -> toTreeMapNode(n, metric, colorProvider))
                .flatMap(Optional::stream)
                .forEach(treeNode::insertNode);

        return treeNode;
    }
}
