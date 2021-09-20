package io.jenkins.plugins.coverage.model;

import io.jenkins.plugins.coverage.targets.CoverageElement;

/**
 * A leaf in the coverage hierarchy. A leaf is a non-divisible coverage metric like line or branch coverage.
 *
 * @author Ullrich Hafner
 */
public class CoverageLeaf {
    private final CoverageElement element;
    private final Coverage coverage;

    /**
     * Creates a new leaf with the given coverage for the specified element.
     *
     * @param element
     *         the element
     * @param coverage
     *         the coverage of the element
     */
    public CoverageLeaf(final CoverageElement element, final Coverage coverage) {
        this.element = element;
        this.coverage = coverage;
    }

    public CoverageElement getElement() {
        return element;
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
        if (element.equals(searchElement)) {
            return coverage;
        }
        return Coverage.NO_COVERAGE;
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", element, coverage);
    }
}
