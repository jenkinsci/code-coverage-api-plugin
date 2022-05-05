package io.jenkins.plugins.coverage.model;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import one.util.streamex.StreamEx;

/**
 * A hierarchical decomposition of coverage results.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
public class CoverageNode implements Serializable {
    private static final long serialVersionUID = -6608885640271135273L;

    private static final Coverage COVERED_NODE = new Coverage(1, 0);
    private static final Coverage MISSED_NODE = new Coverage(0, 1);

    /** Transient non static {@link CoverageTreeCreator} in order to be able to mock it for tests. */
    private transient CoverageTreeCreator coverageTreeCreator;

    static final String ROOT = "^";

    private final CoverageMetric metric;
    private final String name;
    private final List<CoverageNode> children = new ArrayList<>();
    private final List<CoverageLeaf> leaves = new ArrayList<>();
    @CheckForNull
    private CoverageNode parent;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param metric
     *         the coverage metric this node belongs to
     * @param name
     *         the human-readable name of the node
     */
    public CoverageNode(final CoverageMetric metric, final String name) {
        this(metric, name, new CoverageTreeCreator());
    }

    /**
     * Creates a new coverage item node with the given name and a mocked {@link CoverageTreeCreator}.
     *
     * @param metric
     *         the coverage metric this node belongs to
     * @param name
     *         the human-readable name of the node
     * @param coverageTreeCreator
     *         the coverage tree creator
     */
    @VisibleForTesting
    public CoverageNode(final CoverageMetric metric, final String name, final CoverageTreeCreator coverageTreeCreator) {
        this.metric = metric;
        this.name = name;
        this.coverageTreeCreator = coverageTreeCreator;
    }

    /**
     * Called after de-serialization to restore transient fields.
     *
     * @return this
     * @throws ObjectStreamException
     *         if the operation failed
     */
    protected Object readResolve() throws ObjectStreamException {
        if (coverageTreeCreator == null) {
            coverageTreeCreator = new CoverageTreeCreator();
        }
        return this;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent, if existent
     * @throws IllegalStateException
     *         if no parent exists
     */
    public CoverageNode getParent() {
        if (parent == null) {
            throw new IllegalStateException("Parent is not set");
        }
        return parent;
    }

    /**
     * Returns the source code path of this node.
     *
     * @return the element type
     */
    public String getPath() {
        return StringUtils.EMPTY;
    }

    protected String mergePath(final String localPath) {
        // default packages are named '-'
        if ("-".equals(localPath)) {
            return StringUtils.EMPTY;
        }

        if (hasParent()) {
            String parentPath = getParent().getPath();

            if (StringUtils.isBlank(parentPath)) {
                return localPath;
            }
            if (StringUtils.isBlank(localPath)) {
                return parentPath;
            }
            return parentPath + "/" + localPath;
        }

        return localPath;
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

    /**
     * Gets the coverage for each available metric as a fraction between 0 and 1.
     *
     * @return the coverage fractions mapped by their metric
     */
    public SortedMap<CoverageMetric, Fraction> getMetricFractions() {
        return getMetrics().stream()
                .collect(Collectors.toMap(Function.identity(),
                        searchMetric -> getCoverage(searchMetric).getCoveredFraction(), (o1, o2) -> o1,
                        TreeMap::new));
    }

    /**
     * Gets the coverage for each available metric as a percentage between 0 and 100.
     *
     * @return the coverage percentages mapped by their metric
     */
    public SortedMap<CoverageMetric, CoveragePercentage> getMetricPercentages() {
        return StreamEx.of(getMetricFractions().entrySet())
                .toSortedMap(Entry::getKey, e -> CoveragePercentage.getCoveragePercentage(e.getValue()));
    }

    public String getName() {
        return name;
    }

    public List<CoverageNode> getChildren() {
        return children;
    }

    public List<CoverageLeaf> getLeaves() {
        return leaves;
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
     * Prints the coverage for the specified element. Uses {@code Locale.getDefault()} to format the percentage.
     *
     * @param searchMetric
     *         the element to print the coverage for
     *
     * @return coverage ratio in a human-readable format
     * @see #printCoverageFor(CoverageMetric, Locale)
     */
    public String printCoverageFor(final CoverageMetric searchMetric) {
        return printCoverageFor(searchMetric, Locale.getDefault());
    }

    /**
     * Prints the coverage for the specified element.
     *
     * @param searchMetric
     *         the element to print the coverage for
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return coverage ratio in a human-readable format
     */
    public String printCoverageFor(final CoverageMetric searchMetric, final Locale locale) {
        return getCoverage(searchMetric).formatCoveredPercentage(locale);
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
     * Computes the coverage delta between this node and the specified reference node as fractions between 0 and 1.
     *
     * @param reference
     *         the reference node
     *
     * @return the delta coverage for each available metric as fraction
     */
    public SortedMap<CoverageMetric, Fraction> computeDelta(final CoverageNode reference) {
        SortedMap<CoverageMetric, Fraction> deltaPercentages = new TreeMap<>();
        SortedMap<CoverageMetric, Fraction> metricPercentages = getMetricFractions();
        SortedMap<CoverageMetric, Fraction> referencePercentages = reference.getMetricFractions();
        metricPercentages.forEach((key, value) ->
                deltaPercentages.put(key,
                        saveSubtractFraction(value, referencePercentages.getOrDefault(key, Fraction.ZERO))));
        return deltaPercentages;
    }

    /**
     * Computes the coverage delta between this node and the specified reference node as percentage between 0 and 100.
     *
     * @param reference
     *         the reference node
     *
     * @return the delta coverage for each available metric as percentage
     */
    public SortedMap<CoverageMetric, CoveragePercentage> computeDeltaAsPercentage(final CoverageNode reference) {
        return StreamEx.of(computeDelta(reference).entrySet())
                .toSortedMap(Entry::getKey, e -> CoveragePercentage.getCoveragePercentage(e.getValue()));
    }

    /**
     * Calculates the difference between two fraction. Since there might be an arithmetic exception due to an overflow,
     * the method handles it and calculates the difference based on the double values of the fractions.
     *
     * @param minuend
     *         The minuend as a fraction
     * @param subtrahend
     *         The subtrahend as a fraction
     *
     * @return the difference as a fraction
     */
    private Fraction saveSubtractFraction(final Fraction minuend, final Fraction subtrahend) {
        try {
            return minuend.subtract(subtrahend);
        }
        catch (ArithmeticException e) {
            double diff = minuend.doubleValue() - subtrahend.doubleValue();
            return Fraction.getFraction(diff);
        }
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
     * Returns recursively all nodes of the instance {@link FileCoverageNode}.
     *
     * @return all file coverage nodes
     * @since 3.0.0
     */
    public List<FileCoverageNode> getAllFileCoverageNodes() {
        List<FileCoverageNode> childNodes = children.stream()
                .map(CoverageNode::getAllFileCoverageNodes)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (this instanceof FileCoverageNode) {
            childNodes.add((FileCoverageNode) this);
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
        return children.stream()
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
        return children.stream()
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
        return name.hashCode() == searchNameHashCode || getPath().hashCode() == searchNameHashCode;
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

    /**
     * Filters the package structure for only package nodes which contain file nodes. The filtered tree is required in
     * order to calculate the package coverage. Note that packages without any files are fully removed.
     *
     * @return a filtered copy of this {@link CoverageNode}
     */
    public CoverageNode filterPackageStructure() {
        CoverageNode copy = copyTree();
        if (CoverageMetric.MODULE.equals(metric)) {
            Set<CoverageNode> packagesWithFiles = copy.getAll(CoverageMetric.PACKAGE).stream()
                    .filter(node -> node.getChildren().stream()
                            .anyMatch(child -> child.getMetric().equals(CoverageMetric.FILE)))
                    .collect(Collectors.toSet());
            packagesWithFiles.forEach(node -> {
                node.setParent(copy);
                Set<CoverageNode> fileChildren = node.getChildren().stream()
                        .filter(child -> !child.getMetric().equals(CoverageMetric.PACKAGE))
                        .collect(Collectors.toSet());
                node.children.clear();
                node.children.addAll(fileChildren);
            });
            Set<CoverageNode> nonePackageChildren = copy.children.stream()
                    .filter(node -> !node.getMetric().equals(CoverageMetric.PACKAGE))
                    .collect(Collectors.toSet());
            copy.children.clear();
            copy.children.addAll(nonePackageChildren);
            copy.children.addAll(packagesWithFiles);
        }
        return copy;
    }

    /**
     * Checks whether the coverage tree contains a change coverage at all. The method checks if line or branch coverage
     * are available since these are the basic metrics which are available if changes exist.
     *
     * @return {@code true} whether a change coverage exist, else {@code false}
     */
    public boolean hasChangeCoverage() {
        return hasChangeCoverage(CoverageMetric.LINE) || hasChangeCoverage(CoverageMetric.BRANCH);
    }

    /**
     * Checks whether the coverage tree contains a change coverage for the passed {@link CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return {@code true} whether a change coverage exist for the coverage metric, else {@code false}
     */
    public boolean hasChangeCoverage(final CoverageMetric coverageMetric) {
        return getChangeCoverageTree()
                .getCoverage(coverageMetric)
                .getTotal() > 0;
    }

    /**
     * Creates a filtered coverage tree which only contains nodes with code changes. The root of the tree is this.
     *
     * @return the filtered coverage tree
     */
    public CoverageNode getChangeCoverageTree() {
        return coverageTreeCreator.createChangeCoverageTree(this);
    }

    public int getFileAmountWithChangedCoverage() {
        return extractFileNodesWithChangeCoverage().size();
    }

    public long getLineAmountWithChangedCoverage() {
        return extractFileNodesWithChangeCoverage().stream()
                .map(node -> { // only mention lines with changes which affect coverage
                    SortedSet<Integer> filtered = new TreeSet<>(node.getChangedCodeLines());
                    return filtered.stream()
                            .filter(line -> node.getCoveragePerLine().containsKey(line))
                            .collect(Collectors.toSet());
                })
                .mapToLong(Collection::size)
                .sum();
    }

    private Set<FileCoverageNode> extractFileNodesWithChangeCoverage() {
        return getChangeCoverageTree().getAllFileCoverageNodes().stream()
                .filter(node -> node.getChangedCodeLines()
                        .stream() // only mention files with changes which affect coverage
                        .anyMatch(line -> node.getCoveragePerLine().containsKey(line)))
                .collect(Collectors.toSet());
    }

    public int getFileAmountWithIndirectCoverageChanges() {
        return extractFileNodesWithIndirectCoverageChanges().size();
    }

    public long getLineAmountWithIndirectCoverageChanges() {
        return extractFileNodesWithIndirectCoverageChanges().stream()
                .map(node -> node.getIndirectCoverageChanges().values())
                .mapToLong(Collection::size)
                .sum();
    }

    private Set<FileCoverageNode> extractFileNodesWithIndirectCoverageChanges() {
        return getIndirectCoverageChangesTree().getAllFileCoverageNodes().stream()
                .filter(node -> !node.getIndirectCoverageChanges().isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Checks whether the coverage tree contains indirect coverage changes at all. The method checks if line or branch
     * coverage are available since these are the basic metrics which are available if changes exist.
     *
     * @return {@code true} whether indirect coverage changes exist, else {@code false}
     */
    public boolean hasIndirectCoverageChanges() {
        return hasIndirectCoverageChanges(CoverageMetric.LINE) || hasIndirectCoverageChanges(CoverageMetric.BRANCH);
    }

    /**
     * Checks whether the coverage tree contains indirect coverage changes for the passed {@link CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return {@code true} whether indirect coverage changes exist for the coverage metric, else {@code false}
     */
    public boolean hasIndirectCoverageChanges(final CoverageMetric coverageMetric) {
        return getIndirectCoverageChangesTree()
                .getCoverage(coverageMetric)
                .getTotal() > 0;
    }

    /**
     * Creates a filtered coverage tree which only contains nodes with indirect coverage changes. The root of the tree
     * is this.
     *
     * @return the filtered coverage tree
     */
    public CoverageNode getIndirectCoverageChangesTree() {
        return coverageTreeCreator.createIndirectCoverageChangesTree(this);
    }

    /**
     * Checks whether code any changes have been detected no matter if the code coverage is affected or not.
     *
     * @return {@code true} whether code changes have been detected
     */
    public boolean hasCodeChanges() {
        return getAllFileCoverageNodes().stream()
                .anyMatch(fileNode -> !fileNode.getChangedCodeLines().isEmpty());
    }

    /**
     * Creates a deep copy of the coverage tree with this as root node.
     *
     * @return the root node of the copied tree
     */
    public CoverageNode copyTree() {
        return copyTree(null);
    }

    /**
     * Recursively copies the coverage tree with the passed {@link CoverageNode} as root.
     *
     * @param copiedParent
     *         The root node
     *
     * @return the copied tree
     */
    protected CoverageNode copyTree(@CheckForNull final CoverageNode copiedParent) {
        CoverageNode copy = copyEmpty();
        if (copiedParent != null) {
            copy.setParent(copiedParent);
        }

        getChildren().stream()
                .map(node -> node.copyTree(this))
                .forEach(copy::add);
        getLeaves().forEach(copy::add);

        return copy;
    }

    /**
     * Creates a copied instance of this node that has no children, leaves, and parent yet.
     *
     * @return the new and empty node
     */
    protected CoverageNode copyEmpty() {
        return new CoverageNode(metric, name);
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
        CoverageNode newNode = new PackageCoverageNode(childName);
        add(newNode);
        return newNode;
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
        return Objects.equals(metric, that.metric) && Objects.equals(name, that.name)
                && Objects.equals(children, that.children) && Objects.equals(leaves, that.leaves);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, name, children, leaves);
    }
}
