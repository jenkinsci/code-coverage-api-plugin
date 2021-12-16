package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.hm.hafner.util.Ensure;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A hierarchical decomposition of coverage results.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
public final class CoverageNode implements Serializable {
    private static final long serialVersionUID = -6608885640271135273L;

    private static final Coverage COVERED_NODE = new Coverage(1, 0);
    private static final Coverage MISSED_NODE = new Coverage(0, 1);
    private static final int[] EMPTY_ARRAY = new int[0];

    static final String ROOT = "^";

    private final CoverageMetric metric;
    private final String name;
    private final List<CoverageNode> children = new ArrayList<>();
    private final List<CoverageLeaf> leaves = new ArrayList<>();
    @CheckForNull
    private CoverageNode parent;
    private int[] uncoveredLines = EMPTY_ARRAY;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param metric
     *         the coverage metric this node belongs to
     * @param name
     *         the human-readable name of the node
     */
    public CoverageNode(final CoverageMetric metric, final String name) {
        this.metric = metric;
        this.name = name;
    }

    /**
     * Returns the type if the coverage metric for this node.
     *
     * @return the element type
     */
    public CoverageMetric getMetric() {
        return metric;
    }

    /**
     * Returns the available coverage metrics for the whole tree starting with this node.
     *
     * @return the elements in this tree
     */
    public SortedSet<CoverageMetric> getMetrics() {
        SortedSet<CoverageMetric> elements = children.stream()
                .map(CoverageNode::getMetrics)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        elements.add(getMetric());
        leaves.stream().map(CoverageLeaf::getMetric).forEach(elements::add);

        return elements;
    }

    /**
     * Returns the most important coverage metrics.
     *
     * @return most important coverage metrics
     */
    public Collection<CoverageMetric> getImportantMetrics() {
        List<CoverageMetric> importantElements = new ArrayList<>();
        importantElements.add(CoverageMetric.LINE);
        importantElements.add(CoverageMetric.BRANCH);
        importantElements.retainAll(getMetrics());
        return importantElements;
    }

    /**
     * Returns a mapping of metric to coverage. The root of the tree will be skipped.
     *
     * @return a mapping of metric to coverage.
     */
    public SortedMap<CoverageMetric, Coverage> getMetricsDistribution() {
        return getMetrics().stream()
                .collect(Collectors.toMap(Function.identity(), this::getCoverage, (o1, o2) -> o1, TreeMap::new));
    }

    public SortedMap<CoverageMetric, Double> getMetricPercentages() {
        return getMetrics().stream()
                .collect(Collectors.toMap(Function.identity(),
                        searchMetric -> getCoverage(searchMetric).getCoveredPercentage(), (o1, o2) -> o1,
                        TreeMap::new));
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
        if (parent == null) {
            return ROOT;
        }
        CoverageMetric type = parent.getMetric();

        List<String> parentsOfSameType = new ArrayList<>();
        for (CoverageNode node = parent; node != null && node.getMetric().equals(type); node = node.parent) {
            parentsOfSameType.add(0, node.getName());
        }
        return String.join(".", parentsOfSameType);
    }

    /**
     * Prints the coverage for the specified element.
     *
     * @param searchMetric
     *         the element to print the coverage for
     *
     * @return coverage ratio in a human-readable format
     */
    public String printCoverageFor(final CoverageMetric searchMetric) {
        return getCoverage(searchMetric).printCoveredPercentage();
    }

    /**
     * Returns the coverage for the specified metric.
     *
     * @param searchMetric
     *         the element to get the coverage for
     *
     * @return coverage ratio
     */
    public Coverage getCoverage(final CoverageMetric searchMetric) {
        if (searchMetric.isLeaf()) {
            Coverage childrenCoverage = children.stream()
                    .map(node -> node.getCoverage(searchMetric))
                    .reduce(Coverage.NO_COVERAGE, Coverage::add);
            return leaves.stream()
                    .map(node -> node.getCoverage(searchMetric))
                    .reduce(childrenCoverage, Coverage::add);
        }
        else {
            Coverage childrenCoverage = children.stream()
                    .map(node -> node.getCoverage(searchMetric))
                    .reduce(Coverage.NO_COVERAGE, Coverage::add);

            if (metric.equals(searchMetric)) {
                if (getCoverage(CoverageMetric.LINE).getCovered() > 0) {
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
     * Computes the coverage delta between this node and the specified reference node.
     *
     * @param reference
     *         the reference node
     *
     * @return the delta coverage for each available metric
     */
    public SortedMap<CoverageMetric, Double> computeDelta(final CoverageNode reference) {
        SortedMap<CoverageMetric, Double> deltaPercentages = new TreeMap<>();
        SortedMap<CoverageMetric, Double> metricPercentages = getMetricPercentages();
        SortedMap<CoverageMetric, Double> referencePercentages = reference.getMetricPercentages();
        metricPercentages.forEach((key, value) ->
                deltaPercentages.put(key, value - referencePercentages.getOrDefault(key, 0.0)));
        return deltaPercentages;
    }

    /**
     * Returns recursively all nodes for the specified metric type.
     *
     * @param searchMetric
     *         the metric to look for
     *
     * @return all nodes for the given metric
     */
    public List<CoverageNode> getAll(final CoverageMetric searchMetric) {
        Ensure.that(searchMetric.isLeaf())
                .isFalse("Leaves like '%s' are not stored as inner nodes of the tree", searchMetric);

        List<CoverageNode> childNodes = children.stream()
                .map(child -> child.getAll(searchMetric))
                .flatMap(List::stream).collect(Collectors.toList());
        if (metric.equals(searchMetric)) {
            childNodes.add(this);
        }
        return childNodes;
    }

    /**
     * Finds the coverage metric with the given name starting from this node.
     *
     * @param searchMetric
     *         the coverage metric to search for
     * @param searchName
     *         the name of the node
     *
     * @return the result if found
     */
    public Optional<CoverageNode> find(final CoverageMetric searchMetric, final String searchName) {
        if (matches(searchMetric, searchName)) {
            return Optional.of(this);
        }
        return children
                .stream()
                .map(child -> child.find(searchMetric, searchName))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findAny();
    }

    /**
     * Finds the coverage metric with the given hash code starting from this node.
     *
     * @param searchMetric
     *         the coverage metric to search for
     * @param searchNameHashCode
     *         the hash code of the node name
     *
     * @return the result if found
     */
    public Optional<CoverageNode> findByHashCode(final CoverageMetric searchMetric, final int searchNameHashCode) {
        if (matches(searchMetric, searchNameHashCode)) {
            return Optional.of(this);
        }
        return children
                .stream()
                .map(child -> child.findByHashCode(searchMetric, searchNameHashCode))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findAny();
    }

    /**
     * Returns whether this node matches the specified coverage metric and name.
     *
     * @param searchMetric
     *         the coverage metric to search for
     * @param searchName
     *         the name of the node
     *
     * @return the result if found
     */
    public boolean matches(final CoverageMetric searchMetric, final String searchName) {
        return metric.equals(searchMetric) && name.equals(searchName);
    }

    /**
     * Returns whether this node matches the specified coverage metric and name.
     *
     * @param searchMetric
     *         the coverage metric to search for
     * @param searchNameHashCode
     *         the hash code of the node name
     *
     * @return the result if found
     */
    public boolean matches(final CoverageMetric searchMetric, final int searchNameHashCode) {
        if (!metric.equals(searchMetric)) {
            return false;
        }
        return name.hashCode() == searchNameHashCode;
    }

    /**
     * Splits flat packages into a package hierarchy. Changes the internal tree structure in place.
     */
    public void splitPackages() {
        if (CoverageMetric.MODULE.equals(metric)) {
            List<CoverageNode> allPackages = children.stream()
                    .filter(child -> CoverageMetric.PACKAGE.equals(child.getMetric()))
                    .collect(Collectors.toList());
            if (!allPackages.isEmpty()) {
                children.clear();
                for (CoverageNode packageNode : allPackages) {
                    String[] packageParts = packageNode.getName().split("\\.");
                    if (packageParts.length > 1) {
                        Deque<String> packageLevels = new ArrayDeque<>(Arrays.asList(packageParts));
                        insertPackage(packageNode, packageLevels);
                    }
                    else {
                        add(packageNode);
                    }
                }
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
        CoverageNode newNode = new CoverageNode(CoverageMetric.PACKAGE, childName);
        add(newNode);
        return newNode;
    }

    public void setUncoveredLines(final int... uncoveredLines) {
        this.uncoveredLines = copy(uncoveredLines);
    }

    public int[] getUncoveredLines() {
        return copy(uncoveredLines);
    }

    private int[] copy(final int... values) {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", metric, name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CoverageNode that = (CoverageNode) o;

        if (!metric.equals(that.metric)) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        if (!children.equals(that.children)) {
            return false;
        }
        if (!leaves.equals(that.leaves)) {
            return false;
        }
        return Arrays.equals(uncoveredLines, that.uncoveredLines);
    }

    @Override
    public int hashCode() {
        int result = metric.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + children.hashCode();
        result = 31 * result + leaves.hashCode();
        result = 31 * result + Arrays.hashCode(uncoveredLines);
        return result;
    }
}
