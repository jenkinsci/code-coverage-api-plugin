package io.jenkins.plugins.coverage.model;

import java.util.Comparator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import edu.umd.cs.findbugs.annotations.NonNull;

import io.jenkins.plugins.coverage.exception.QualityGatesInvalidException;

/**
 * QualityGates are used for the evaluation of a CoverageNode to set a build to failed or successful or return warnings.
 * If no QualityGates are defined, the evaluation (QualityGateEvaluator.java) returns inactive.
 */
public class QualityGate implements Comparable<QualityGate> {
    private CoverageMetric metric;
    private double limit; //if Metric is below return QualityGateStatus
    private QualityGateStatus qualityGateStatus;

    /***
     * Constructor to create a QualityGate. If it is invalid, it throws an exception.
     * @param metric the metric which is evaluated
     * @param limit the upper limit of no warning or failure, if below it returns qualityGateStatus
     * @param qualityGateStatus the status to return if the CoverageNode's percentage for the same metric is below limit
     * @throws QualityGatesInvalidException if QualityGate's metric is not FAILED or WARNING
     */
    public QualityGate(final CoverageMetric metric, final double limit, final QualityGateStatus qualityGateStatus)
            throws QualityGatesInvalidException {
        if (qualityGateStatus.equals(QualityGateStatus.FAILED) || qualityGateStatus.equals(QualityGateStatus.WARNING)) {
            this.metric = metric;
            this.limit = limit;

            this.qualityGateStatus = qualityGateStatus;
        }
        else {
            throw new QualityGatesInvalidException(
                    "Quality Gates can only have FAILED or WARNING as returning QualityGateStatus.");
        }
    }

    public CoverageMetric getMetric() {
        return metric;
    }

    public void setMetric(final CoverageMetric metric) {
        this.metric = metric;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(final double limit) {
        this.limit = limit;
    }

    public QualityGateStatus getQualityGateStatus() {
        return qualityGateStatus;
    }

    public void setQualityGateStatus(final QualityGateStatus qualityGateStatus) {
        this.qualityGateStatus = qualityGateStatus;
    }


    @Override
    public int compareTo(@NonNull final QualityGate other) {
        return Comparator.comparing(QualityGate::getQualityGateStatus)
                .thenComparing(QualityGate::getMetric)
                .compare(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof QualityGate)) {
            return false;
        }

        QualityGate otherGate = (QualityGate) o;

        return new EqualsBuilder()
                .append(metric, otherGate.metric)
                .append(qualityGateStatus, otherGate.qualityGateStatus)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(metric)
                .append(qualityGateStatus)
                .toHashCode();
    }

}
