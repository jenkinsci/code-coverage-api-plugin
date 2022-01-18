package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;

/**
 * Used for thresholds and global thresholds.
 */
abstract class AbstractThreshold extends PageAreaImpl {
    private final Control thresholdTarget = control("thresholdTarget");
    private final Control unhealthyThreshold = control("unhealthyThreshold");
    private final Control unstableThreshold = control("unstableThreshold");
    private final Control failUnhealthy = control("failUnhealthy");
    private final Control delete = control("repeatable-delete");
    /**
     * Constructor of an AbstractThreshold.
     *
     * @param parent
     *         of threshold
     * @param path
     *         to threshold
     */

    protected AbstractThreshold(PageArea parent, String path) {
        super(parent, path);
    }

    /**
     * Setter for unhealthy-threshold.
     *
     * @param threshold
     *         for unhealthy
     */
    public void setUnhealthyThreshold(double threshold) {
        ensureAdvancedOptionsIsActivated();
        unhealthyThreshold.set(threshold);
    }

    /**
     * Setter for unstable-threshold.
     *
     * @param threshold
     *         for unstable
     */
    public void setUnstableThreshold(double threshold) {
        ensureAdvancedOptionsIsActivated();
        unstableThreshold.set(threshold);
    }

    /**
     * Setter for fail on unhealthy.
     *
     * @param failOnUnhealthy
     *         boolean for failing on unhealthy
     */
    public void setFailUnhealthy(boolean failOnUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        failUnhealthy.check(failOnUnhealthy);
    }

    /**
     * Deletes a threshold.
     */
    public void delete() {
        ensureAdvancedOptionsIsActivated();
        delete.click();
    }

    /**
     * Ensures advanced options are activated so that Thresholds can be set.
     */
    public abstract void ensureAdvancedOptionsIsActivated();

    Control getThresholdTarget() {
        return thresholdTarget;
    }
}
