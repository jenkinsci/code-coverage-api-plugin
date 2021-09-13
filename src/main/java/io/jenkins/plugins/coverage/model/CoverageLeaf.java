package io.jenkins.plugins.coverage.model;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class CoverageLeaf {
    private final String type;
    private final Coverage coverage;

    public CoverageLeaf(final String type, final Coverage coverage) {
        this.type = type;
        this.coverage = coverage;
    }

    public Coverage getCoverage(final String coverageType) {
        if (type.equals(coverageType)) {
            return coverage;
        }
        return Coverage.NO_COVERAGE;
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", type, coverage);
    }
}
