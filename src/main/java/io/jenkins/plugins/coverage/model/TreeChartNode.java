package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Node for constructing a tree structure for a sunburst or treemap ECharts diagram.
 *
 * @author Andreas Pabst
 * @author Ullrich Hafner
 */
public class TreeChartNode {
    private final ItemStyle itemStyle;
    private String name;
    private final List<Double> values = new ArrayList<>();

    private final List<TreeChartNode> children = new ArrayList<>();

    /**
     * Creates a new {@link TreeChartNode} with the value 0.0.
     *
     * @param name
     *         the name of the node
     */
    public TreeChartNode(final String name) {
        this(name, 0.0);
    }

    /**
     * Creates a new {@link TreeChartNode} with the given value.
     *
     * @param name
     *         the name of the node
     * @param value
     *         the value of the node
     */
    public TreeChartNode(final String name, final double value) {
        this(name, "-", value);
    }

    /**
     * Creates a new {@link TreeChartNode} with the given values.
     *
     * @param name
     *         the name of the node
     * @param color
     *         the color of the node
     * @param value
     *         the value of the node
     * @param additionalValues
     *         additional values of the node
     */
    public TreeChartNode(final String name, final String color, final double value, final double... additionalValues) {
        this.itemStyle = new ItemStyle(color);
        this.name = name;
        this.values.add(value);
        Collections.addAll(values, ArrayUtils.toObject(additionalValues));
    }

    public List<Double> getValue() {
        return Collections.unmodifiableList(values);
    }

    public String getName() {
        return name;
    }

    public ItemStyle getItemStyle() {
        return itemStyle;
    }

    public List<TreeChartNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Inserts the specified node in the tree.
     *
     * @param node
     *         the node to insert
     */
    public void insertNode(final TreeChartNode node) {
        children.add(node);
    }

    /**
     * Collapse the package names. If a node only has one child, its name is appended to the current node and its
     * children are now the children of the current node. This is repeated as long as there are nodes with only one
     * child (package nodes at the top of the hierarchy).
     */
    public void collapseEmptyPackages() {
        while (children.size() == 1) {
            TreeChartNode singleChild = children.iterator().next();
            name = String.join(".", name, singleChild.getName());

            children.clear();
            children.addAll(singleChild.getChildren());
        }
    }

    @Override
    public String toString() {
        return String.format("'%s' (%s)", name, values);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TreeChartNode that = (TreeChartNode) o;

        if (!itemStyle.equals(that.itemStyle)) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        if (!values.equals(that.values)) {
            return false;
        }
        return children.equals(that.children);
    }

    @Override
    public int hashCode() {
        int result = itemStyle.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + values.hashCode();
        result = 31 * result + children.hashCode();
        return result;
    }
}
