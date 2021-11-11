package io.jenkins.plugins.coverage;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import hudson.model.BallColor;
import hudson.model.Result;

/**
 *
 * @author Thomas Willeit
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


    /** Code below this point is not needed
     * Only implemented because it should resemble QualityGateStatus in warnings-plugin
     * Could be removed
     */
    private final Result result;

    QualityGateStatus(final Result result) {
        this.result = result;
    }

    @Whitelisted
    public boolean isSuccessful() {
        return this == PASSED || this == INACTIVE;
    }

    public Result getResult() {
        return result;
    }

    public boolean isWorseThan(final QualityGateStatus other) {
        return ordinal() > other.ordinal();
    }

}
