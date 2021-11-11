package io.jenkins.plugins.coverage;

import java.io.Serializable;

import java.util.Objects;

import io.jenkins.plugins.coverage.model.CoverageMetric;

/**
 * Defines a quality gate based on a specific threshold of coverages (Module, Package, File, Class, Method,
 * Instruction, Line, Branch, Conditional) in the current build. After a build has been finished, a set of
 * {@link QualityGate quality gates} will be evaluated and the overall quality gate status will be reported in
 * Jenkins UI.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class QualityGate implements Serializable {
    private static final long serialVersionUID = -8305345358877991900L;

    private final double threshold;
    private final CoverageMetric coverageMetric;
    private final QualityGateStatus statusIfNotPassedSuccessful;

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *        the minimum coverage percentage for passing the QualityGate successful. In the range of {@code [0, 1]}.
     * @param coverageMetric
     *        the coverage metric which is checked in this QualityGate
     * @param unstable
     *        determines if the the build will be allowed to pass unstable with warnings in case of unreached threshold or it should fail
     */
    public QualityGate(final double threshold, final CoverageMetric coverageMetric, final boolean unstable) {
        this.threshold = threshold;
        this.coverageMetric = coverageMetric;
        this.statusIfNotPassedSuccessful = unstable ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
    }

    /**
     * Returns the minimum percentage of coverage that will fail the quality gate.
     *
     * @return minimum percentage of coverage
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Returns the quality gate status to set if the quality gate is failed.
     *
     * @return the status
     */
    public QualityGateStatus getStatusIfNotPassedSuccessful() {
        return statusIfNotPassedSuccessful;
    }

    /**
     * Returns the coverage metric type bound to this QualityGate.
     *
     * @return the coverage metric type
     */
    public CoverageMetric getCoverageMetric() {
        return coverageMetric;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QualityGate that = (QualityGate) o;
        return threshold == that.threshold && coverageMetric == that.coverageMetric && statusIfNotPassedSuccessful
                == that.statusIfNotPassedSuccessful;
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, coverageMetric, statusIfNotPassedSuccessful);
    }
}
