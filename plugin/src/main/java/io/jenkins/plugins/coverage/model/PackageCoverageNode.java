package io.jenkins.plugins.coverage.model;

/**
 * A {@link CoverageNode} for a specific package. It converts a package structure to a corresponding path structure.
 *
 * @author Ullrich Hafner
 */
public class PackageCoverageNode extends CoverageNode {
    private static final long serialVersionUID = 8236436628673022634L;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public PackageCoverageNode(final String name) {
        super(CoverageMetric.PACKAGE, name);
    }

    @Override
    public String getPath() {
        return mergePath(getName().replaceAll("\\.", "/"));
    }

    @Override
    public PackageCoverageNode copyTree(final CoverageNode copiedParent) {
        PackageCoverageNode copy = new PackageCoverageNode(getName());
        if (copiedParent != null) {
            copy.setParent(copiedParent);
        }

        copyChildrenAndLeaves(this, copy);

        return copy;
    }
}
