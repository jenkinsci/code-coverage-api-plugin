package io.jenkins.plugins.coverage.model;

import java.util.Objects;

/**
 * A {@link CoverageNode} for a specific method.
 *
 * @author Florian Orendi
 */
public class MethodCoverageNode extends CoverageNode {

    private static final long serialVersionUID = -5765205034179396434L;

    /**
     * The line number where the code of method begins (not including the method head).
     */
    private final int lineNumber;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param name
     *         The human-readable name of the node
     * @param lineNumber
     *         The line number where the method begins (not including the method head)
     */
    public MethodCoverageNode(final String name, final int lineNumber) {
        super(CoverageMetric.METHOD, name);
        this.lineNumber = lineNumber;
    }

    /**
     * Checks whether the line number is valid.
     *
     * @return {@code true} if the line number is valid, else {@code false}
     */
    public boolean hasValidLineNumber() {
        return lineNumber > 0;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    protected CoverageNode copyEmpty() {
        return new MethodCoverageNode(getName(), getLineNumber());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MethodCoverageNode that = (MethodCoverageNode) o;
        return lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lineNumber);
    }
}
