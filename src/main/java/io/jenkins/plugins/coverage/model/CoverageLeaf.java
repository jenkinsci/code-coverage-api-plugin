package io.jenkins.plugins.coverage.model;

import io.jenkins.plugins.coverage.targets.CoverageElement;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class CoverageLeaf {
    private final CoverageElement element;
    private final Coverage coverage;

    public CoverageLeaf(final CoverageElement element, final Coverage coverage) {
        this.element = element;
        this.coverage = coverage;
    }

    public CoverageElement getElement() {
        return element;
    }

    public Coverage getCoverage(final CoverageElement coverageType) {
        if (element.equals(coverageType)) {
            return coverage;
        }
        return Coverage.NO_COVERAGE;
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", element, coverage);
    }
}
