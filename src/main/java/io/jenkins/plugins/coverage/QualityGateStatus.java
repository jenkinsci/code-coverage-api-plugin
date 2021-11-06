package io.jenkins.plugins.coverage;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import hudson.model.BallColor;
import hudson.model.Result;

public enum QualityGateStatus {

    INACTIVE(Result.NOT_BUILT),
    PASSED(Result.SUCCESS),
    WARNING(Result.UNSTABLE),
    FAILED(Result.FAILURE);

    private final Result result;

    QualityGateStatus(final Result result) {
        this.result = result;
    }

    /**
     * Returns the associated {@link Result} color.
     *
     * @return Jenkins' {@link Result} color
     */
    public BallColor getColor() {
        return result.color;
    }

    /**
     * Returns the associated {@link Result} icon class to be used in the UI.
     *
     * @return Jenkins' {@link Result} icon class
     */
    public String getIconClass() {
        return getColor().getIconClassName();
    }

    /**
     * Returns whether the quality gate has been passed (or has not been activated at all).
     *
     * @return {@code true} if the quality gate has been passed, {@code false}  otherwise
     */
    @Whitelisted
    public boolean isSuccessful() {
        return this == PASSED || this == INACTIVE;
    }

    /**
     * Returns whether the quality gate has not been passed successful.
     *
     * @return {@code true} if the quality gate has been passed, {@code false}  otherwise
     */
    @Whitelisted
    public boolean isNotSuccessful() {
        return this == WARNING || this == FAILED;
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
}
