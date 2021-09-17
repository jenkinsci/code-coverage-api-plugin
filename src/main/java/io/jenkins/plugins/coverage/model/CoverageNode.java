package io.jenkins.plugins.coverage.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.hm.hafner.util.Ensure;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import io.jenkins.plugins.coverage.adapter.JavaCoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.targets.CoverageElement;

/**
 * A hierarchical decomposition of coverage results.
 *
 * @author Ullrich Hafner
 */
public class CoverageNode {
    private static final Coverage COVERED_NODE = new Coverage(1, 0);
    private static final Coverage MISSED_NODE = new Coverage(0, 1);
    private static final int[] EMPTY_ARRAY = new int[0];

    static final String ROOT = "^";

    private final CoverageElement element;
    private final String name;
    private final List<CoverageNode> children = new ArrayList<>();
    private final List<CoverageLeaf> leaves = new ArrayList<>();
    @CheckForNull
    private CoverageNode parent;
    private int[] uncoveredLines = EMPTY_ARRAY;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param element
     *         the type of the coverage element
     * @param name
     *         the human-readable name of the node
     */
    public CoverageNode(final CoverageElement element, final String name) {
        this.element = element;
        this.name = name;
    }

    /**
     * Returns the type if the coverage element for this node.
     *
     * @return the element type
     */
    public CoverageElement getElement() {
        return element;
    }

    /**
     * Returns the available coverage elements for the whole tree starting with this node.
     *
     * @return the elements in this tree
     */
    public Set<CoverageElement> getElements() {
        Set<CoverageElement> elements = children.stream()
                .map(CoverageNode::getElements)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        elements.add(getElement());
        leaves.stream().map(CoverageLeaf::getElement).forEach(elements::add);

        return elements;
    }

    /**
     * Returns the most important coverage elements.
     *
     * @return most important coverage elements
     */
    public Collection<CoverageElement> getImportantElements() {
        List<CoverageElement> importantElements = new ArrayList<>();
        importantElements.add(CoverageElement.LINE);
        importantElements.add(CoverageElement.CONDITIONAL);
        importantElements.retainAll(getElements());
        return importantElements;
    }

    public SortedMap<CoverageElement, Coverage> getElementDistribution() {
        return getElements().stream()
                .collect(Collectors.toMap(Function.identity(), this::getCoverage, (o1, o2) -> o1, TreeMap::new));
    }

    public SortedMap<CoverageElement, Double> getElementPercentages() {
        return getElements().stream()
                .collect(Collectors.toMap(Function.identity(),
                        searchElement -> getCoverage(searchElement).getCoveredPercentage(), (o1, o2) -> o1, TreeMap::new));
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

    /**
     * Appends the specified leaf element to the list of leaves.
     *
     * @param leaf
     *         the leaf to add
     */
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
        return parent == null ? ROOT : parent.getName();
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

    /**
     * Returns the coverage for the specified element.
     *
     * @param searchElement
     *         the element to get the coverage for
     *
     * @return coverage ratio
     */
    public Coverage getCoverage(final CoverageElement searchElement) {
        if (searchElement.isBasicBlock()) {
            Coverage childrenCoverage = children.stream()
                    .map(node -> node.getCoverage(searchElement))
                    .reduce(Coverage.NO_COVERAGE, Coverage::add);
            return leaves.stream()
                    .map(node -> node.getCoverage(searchElement))
                    .reduce(childrenCoverage, Coverage::add);
        }
        else {
            Coverage childrenCoverage = children.stream()
                    .map(node -> node.getCoverage(searchElement))
                    .reduce(Coverage.NO_COVERAGE, Coverage::add);

            if (element.equals(searchElement)) {
                if (getCoverage(CoverageElement.LINE).getCovered() > 0) {
                    return childrenCoverage.add(COVERED_NODE);
                }
                else {
                    return childrenCoverage.add(MISSED_NODE);
                }
            }
            return childrenCoverage;
        }
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
        Ensure.that(searchElement.isBasicBlock()).isFalse("Basic blocks like '%s' are not stored as inner nodes of the tree", searchElement);

        List<CoverageNode> childNodes = children.stream()
                .map(child -> child.getAll(searchElement))
                .flatMap(List::stream).collect(Collectors.toList());
        if (element.equals(searchElement)) {
            childNodes.add(this);
        }
        return childNodes;
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
        return findByHashCode(searchElement, Integer.parseInt(searchName));
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
    public Optional<CoverageNode> findByHashCode(final CoverageElement searchElement, final int searchNameHashCode) {
        if (matches(searchElement, searchNameHashCode)) {
            return Optional.of(this);
        }
        return children
                .stream()
                .map(child -> child.findByHashCode(searchElement, searchNameHashCode))
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
        if (!element.equals(searchElement)) {
            return false;
        }
        return name.hashCode() == searchNameHashCode;
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

    public void setUncoveredLines(final int[] uncoveredLines) {
        // TODO: can we skip copying the parameter array?
        this.uncoveredLines = uncoveredLines;
    }

    public int[] getUncoveredLines() {
        // TODO: can we skip copying the returned array?
        return uncoveredLines;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", element, name);
    }

    public SortedMap<CoverageElement, Double> computeDelta(final CoverageNode reference) {
        SortedMap<CoverageElement, Double> deltaPercentages = new TreeMap<>();
        SortedMap<CoverageElement, Double> elementPercentages = getElementPercentages();
        SortedMap<CoverageElement, Double> referencePercentages = reference.getElementPercentages();
        elementPercentages.forEach((key, value) -> {
            deltaPercentages.put(key, value - referencePercentages.getOrDefault(key, 0.0));
        });
        return deltaPercentages;
    }
}
