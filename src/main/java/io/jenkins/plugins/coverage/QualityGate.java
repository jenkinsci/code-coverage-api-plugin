package io.jenkins.plugins.coverage;

import java.io.Serializable;

import java.util.Objects;

import io.jenkins.plugins.coverage.model.CoverageMetric;

public class QualityGate implements Serializable {

    private final double threshold;
    private final CoverageMetric type;
    private final QualityGateStatus statusIfNotPassedSuccesful;

    /**
     *
     * @param threshold
     *        the minimum coverage percentage for passing the QualityGate successful. In the range of {@code [0, 1]}.
     * @param type
     *        the type of metric which is checked in this QualityGate
     * @param unstable
     *        determines if the the build will be allowed to pass unstable with warnings in case of unreached threshold or it should fail
     */
    public QualityGate(final double threshold, final CoverageMetric type, final boolean unstable) {
        this.threshold = threshold;
        this.type = type;
        this.statusIfNotPassedSuccesful = unstable ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
    }

    public double getThreshold() {
        return threshold;
    }

    public QualityGateStatus getStatusIfNotPassedSuccesful() {
        return statusIfNotPassedSuccesful;
    }

    public CoverageMetric getType() {
        return type;
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
        return threshold == that.threshold && type == that.type && statusIfNotPassedSuccesful
                == that.statusIfNotPassedSuccesful;
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, type, statusIfNotPassedSuccesful);
    }
}
