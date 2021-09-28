package io.jenkins.plugins.coverage.model;

/**
 * Converts a tree of {@link CoverageNode coverage nodes} to a corresponding tree of {@link TreeChartNode ECharts tree
 * map nodes}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverter {
    TreeChartNode toTeeChartModel(final CoverageNode node) {
        TreeChartNode root = toTreeChartNode(node);
        for (TreeChartNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeChartNode toTreeChartNode(final CoverageNode node) {
        Coverage coverage = node.getCoverage(CoverageMetric.LINE);

        TreeChartNode treeNode = new TreeChartNode(node.getName(),
                assignColor(coverage.getCoveredPercentage() * 100),
                coverage.getTotal(), coverage.getCovered());
        if (node.getMetric().equals(CoverageMetric.FILE)) {
            return treeNode;
        }

        node.getChildren().stream()
                .map(this::toTreeChartNode)
                .forEach(treeNode::insertNode);
        return treeNode;
    }

    private String assignColor(final double percentage) {
        String[] colors = {"#ef9a9a", "#f6bca0", "#fbdea6", "#e2f1aa", "#c4e4a9", "#a5d6a7"};
        double[] levels = {75, 50, 85, 90, 95};

        for (int index = 0; index < levels.length; index++) {
            if (percentage < levels[index]) {
                return colors[index];
            }
        }
        return colors[levels.length - 1];
    }

}
