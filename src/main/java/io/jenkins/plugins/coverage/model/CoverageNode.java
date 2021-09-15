package io.jenkins.plugins.coverage.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import io.jenkins.plugins.coverage.adapter.JavaCoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * A hierarchical decomposition of coverage results.
 *
 * @author Ullrich Hafner
 */
public class CoverageNode {

    public static CoverageNode fromResult(final CoverageResult result) {
        CoverageElement element = result.getElement();
        if (result.getChildren().isEmpty()) {
            CoverageNode coverageNode = new CoverageNode(element, result.getName());
            for (Map.Entry<CoverageElement, Ratio> coverage : result.getLocalResults().entrySet()) {
                CoverageLeaf leaf = new CoverageLeaf(coverage.getKey(), new Coverage(coverage.getValue()));
                coverageNode.add(leaf);
            }
            return coverageNode;
        }
        else {
            CoverageNode coverageNode = new CoverageNode(element, result.getName());
            for (String childKey : result.getChildren()) {
                CoverageResult childResult = result.getChild(childKey);
                coverageNode.add(fromResult(childResult));
            }
            return coverageNode;
        }
    }

    private final CoverageElement element;
    private final String name;
    private final List<CoverageNode> children = new ArrayList<>();
    private final List<CoverageLeaf> leaves = new ArrayList<>();
    @CheckForNull
    private CoverageNode parent;

    public CoverageNode(final CoverageElement element, final String name) {
        this.element = element;
        this.name = name;
    }

    public CoverageElement getElement() {
        return element;
    }

    public String getName() {
        return name;
    }

    public List<CoverageNode> getChildren() {
        return children;
    }

    private void addAll(final List<CoverageNode> nodes) {
        nodes.forEach(this::add);
    }

    /**
     * Appends the specified child element to the list of children.
     *
     * @param child
     *         the child to add
     */
    public void add(final CoverageNode child) {
        children.add(child);
        child.setParent(this);
    }

    public void add(final CoverageLeaf leaf) {
        leaves.add(leaf);
    }

    /**
     * Returns whether this node is the root of the tree.
     *
     * @return {@code true} if this node is the root of the tree, {@code false} otherwise
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns whether this node has a parent node.
     *
     * @return {@code true} if this node has a parent node, {@code false} if it is the root of the hierarchy
     */
    public boolean hasParent() {
        return !isRoot();
    }

    void setParent(final CoverageNode parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    /**
     * Returns the name of the parent element or "-" if there is no such element.
     *
     * @return the name of the parent element
     */
    public String getParentName() {
        return parent == null ? "-" : parent.getName();
    }

    /**
     * Prints the coverage for the specified element.
     *
     * @param searchElement
     *         the element to print the coverage for
     *
     * @return coverage ratio in a human-readable format
     */
    public String printCoverageFor(final CoverageElement searchElement) {
        Coverage coverage = getCoverage(searchElement);
        if (coverage.getTotal() > 0) {
            return String.format("%.2f%%", coverage.getCoveredPercentage() * 100);
        }
        return "n/a";
    }

    public Coverage getCoverage(final CoverageElement searchElement) {
        Coverage childrenCoverage = children.stream()
                .map(node -> node.getCoverage(searchElement))
                .reduce(Coverage.NO_COVERAGE, Coverage::add);
        return leaves.stream()
                .map(node -> node.getCoverage(searchElement))
                .reduce(childrenCoverage, Coverage::add);
    }

    /**
     * Returns recursively all elements of the given type.
     *
     * @param searchElement
     *         the element type to look for
     *
     * @return all elements of the given type
     */
    public List<CoverageNode> getAll(final CoverageElement searchElement) {
        List<CoverageNode> selectedChildNodes = children
                .stream()
                .filter(result -> searchElement.equals(result.getElement()))
                .collect(Collectors.toList());
        children.stream()
                .filter(result -> isContainerNode(result, searchElement))
                .map(child -> child.getAll(searchElement))
                .flatMap(List::stream).forEach(selectedChildNodes::add);
        return selectedChildNodes;
    }

    private boolean isContainerNode(final CoverageNode result, final CoverageElement otherElement) {
        CoverageElement coverageElement = result.getElement();
        return !otherElement.equals(coverageElement) && !coverageElement.isBasicBlock();
    }

    /**
     * Finds the coverage element with the given name starting from this node.
     *
     * @param searchElement
     *         the coverage element to search for
     * @param searchName
     *         the name of the node
     *
     * @return the result if found
     */
    public Optional<CoverageNode> find(final CoverageElement searchElement, final String searchName) {
        return find(searchElement, Integer.parseInt(searchName));
    }

    /**
     * Finds the coverage element with the given name starting from this node.
     *
     * @param searchElement
     *         the coverage element to search for
     * @param searchNameHashCode
     *         the hash code of the node name
     *
     * @return the result if found
     */
    public Optional<CoverageNode> find(final CoverageElement searchElement, final int searchNameHashCode) {
        for (CoverageNode child : children) {
            if (child.matches(searchElement, searchNameHashCode)) {
                return Optional.of(child);
            }
        }
        return children
                .stream()
                .map(child -> child.find(element, searchNameHashCode))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findAny();
    }

    /**
     * Returns whether this node matches the specified coverage element and name.
     *
     * @param searchElement
     *         the coverage element to search for
     * @param searchName
     *         the name of the node
     *
     * @return the result if found
     */
    public boolean matches(final CoverageElement searchElement, final String searchName) {
        return element.equals(searchElement) && name.equals(searchName);
    }

    /**
     * Returns whether this node matches the specified coverage element and name.
     *
     * @param searchElement
     *         the coverage element to search for
     * @param searchNameHashCode
     *         the hash code of the node name
     *
     * @return the result if found
     */
    public boolean matches(final CoverageElement searchElement, final int searchNameHashCode) {
        return element.equals(searchElement) && name.hashCode() == searchNameHashCode;
    }

    public void splitPackages() {
        if (CoverageElement.REPORT.equals(element)) {
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

    private CoverageNode createChild(final String childName) {
        for (CoverageNode child : children) {
            if (child.getName().equals(childName)) {
                return child;
            }

        }
        CoverageNode newNode = new CoverageNode(JavaCoverageReportAdapterDescriptor.PACKAGE, childName);
        add(newNode);
        return newNode;
    }

    public TreeChartNode toChartTree() {
        TreeChartNode root = toChartTree(this);
        for (TreeChartNode child : root.getChildren()) {
            child.collapseEmptyPackages();
        }

        return root;
    }

    private TreeChartNode toChartTree(final CoverageNode node) {
        Coverage coverage = node.getCoverage(CoverageElement.LINE);

        TreeChartNode treeNode = new TreeChartNode(node.getName(),
                assignColor(coverage.getCoveredPercentage() * 100),
                coverage.getTotal(), coverage.getCovered());
        if (node.getElement().equals(CoverageElement.FILE)) {
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

    @Override
    public String toString() {
        return String.format("[%s] %s", element, name);
    }
}
