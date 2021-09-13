package io.jenkins.plugins.coverage.model;

import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class Coverage {
    public static final Coverage NO_COVERAGE = new Coverage(0, 0);

    private final int covered;
    private final int missed;

    public Coverage(final int covered, final int missed) {
        this.covered = covered;
        this.missed = missed;
    }

    public Coverage(final Ratio lineCoverage) {
        this((int) lineCoverage.numerator, (int) (lineCoverage.denominator - lineCoverage.numerator));
    }

    public int getCovered() {
        return covered;
    }

    public int getMissed() {
        return missed;
    }

    public Coverage add(final Coverage additional) {
        return new Coverage(covered + additional.getCovered(),
                missed + additional.getMissed());
    }

    @Override
    public String toString() {
        int total = getTotal();
        if (total > 0) {
            return String.format("%d/%d", covered, total);
        }
        return "n/a";
    }

    public int getTotal() {
        return missed + covered;
    }

    public double getPercentage() {
        if (getTotal() == 0) {
            return 0;
        }
        return covered / (double) getTotal();
    }
}
