package io.jenkins.plugins.coverage.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import io.jenkins.plugins.coverage.adapter.JavaCoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class CoverageNode {
    private final String type;
    private final String name;

    private final List<CoverageNode> children = new ArrayList<>();
    private final List<CoverageLeaf> leaves = new ArrayList<>();

    public CoverageNode(final String type, final String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<CoverageNode> getChildren() {
        return children;
    }

    private void addAll(final List<CoverageNode> nodes) {
        children.addAll(nodes);
    }

    public void add(final CoverageNode child) {
        children.add(child);
    }

    public void add(final CoverageLeaf leaf) {
        leaves.add(leaf);
    }

    public Coverage getCoverage(final String coverageType) {
        Coverage childrenCoverage = children.stream()
                .map(node -> node.getCoverage(coverageType))
                .reduce(Coverage.NO_COVERAGE, Coverage::add);
        return leaves.stream()
                .map(node -> node.getCoverage(coverageType))
                .reduce(childrenCoverage, Coverage::add);
    }

    public void splitPackages() {
        if (CoverageElement.REPORT.getName().equals(type)) {
            List<CoverageNode> allPackages = new ArrayList<>(children);
            children.clear();
            for (CoverageNode aPackage : allPackages) {
                Deque<String> packageLevels = new ArrayDeque<>(Arrays.asList(aPackage.getName().split("\\.")));
                insertPackage(aPackage, packageLevels);
            }
        }
    }

    private void insertPackage(final CoverageNode aPackage, final Deque<String> packageLevels) {
        String nextLevelName = packageLevels.pop();
        CoverageNode subPackage = createChild(nextLevelName);
        if (packageLevels.isEmpty()) {
            subPackage.addAll(aPackage.children);
        }
        else {
            subPackage.insertPackage(aPackage, packageLevels);
        }
    }

    private CoverageNode createChild(final String name) {
        for (CoverageNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }

        }
        CoverageNode newNode = new CoverageNode(JavaCoverageReportAdapterDescriptor.PACKAGE.getName(), name);
        add(newNode);
        return newNode;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", type, name);
    }

    public static CoverageNode fromResult(final CoverageResult result) {
        CoverageElement element = result.getElement();
        if (result.getChildren().isEmpty()) {
            CoverageNode coverageNode = new CoverageNode(element.getName(), result.getName());
            for (Map.Entry<CoverageElement, Ratio> coverage : result.getLocalResults().entrySet()) {
                CoverageLeaf leaf = new CoverageLeaf(coverage.getKey().getName(), new Coverage(coverage.getValue()));
                coverageNode.add(leaf);
            }
            return coverageNode;
        }
        else {
            CoverageNode coverageNode = new CoverageNode(element.getName(), result.getName());
            for (String childKey : result.getChildren()) {
                CoverageResult childResult = result.getChild(childKey);
                coverageNode.add(fromResult(childResult));
            }
            return coverageNode;
        }
    }

    public TreeChartNode toChartTree() {
        TreeChartNode root = toChartTree(this);
        for (TreeChartNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeChartNode toChartTree(final CoverageNode node) {
        Coverage coverage = node.getCoverage(CoverageElement.LINE.getName());

        TreeChartNode treeNode = new TreeChartNode(node.getName(),
                assignColor(coverage.getPercentage() * 100),
                coverage.getTotal(), coverage.getCovered());
        if (node.getType().equals(CoverageElement.FILE.getName())) {
            return treeNode;
        }

        node.getChildren().stream()
                .map(this::toChartTree)
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
