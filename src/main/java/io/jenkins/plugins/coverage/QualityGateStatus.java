package io.jenkins.plugins.coverage;

import java.util.List;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import hudson.model.BallColor;
import hudson.model.Result;

import io.jenkins.plugins.coverage.QualityGateEvaluator.FormattedLogger;
import io.jenkins.plugins.coverage.model.CoverageNode;

/**
 * Result of a {@link QualityGateEvaluator#evaluate(CoverageNode, List, FormattedLogger)} call.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public enum QualityGateStatus {

    /** Quality gate is inactive, so result evaluation is not available. */
    INACTIVE(Result.NOT_BUILT),

    /** Quality gate has been passed. */
    PASSED(Result.SUCCESS),

    /** Quality gate has been missed: severity is a warning. */
    WARNING(Result.UNSTABLE),

    /** Quality gate has been missed: severity is an error. */
    FAILED(Result.FAILURE);

    private final Result result;

    QualityGateStatus(final Result result) {
        this.result = result;
    }

    /**
     * Returns the associated {@link Result}.
     *
     * @return the associated {@link Result}
     */
    public Result getResult() {
        return result;
    }

    /**
     * Returns whether this status is worse than the specified status.
     *
     * @param other
     *         the other status
     *
     * @return {@code true} if this status is worse than the other status, {@code false} otherwise
     */
    public boolean isWorseThan(final QualityGateStatus other) {
        return ordinal() > other.ordinal();
    }

    /**
     * Returns the worse status based on evaluation of {@link #isWorseThan(QualityGateStatus)}.
     * Can also be implemented as {@code status.isWorseThan(other) ? status : other}.
     *
     * @param other
     *          the other status
     *
     * @return {@code this} object, if it is worse than {@code other}, otherwise {@code other}
     */
    public QualityGateStatus getWorseStatus(final QualityGateStatus other) {
        return isWorseThan(other) ? this : other;
    }

}
