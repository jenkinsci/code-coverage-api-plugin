package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;

/**
 * Used for thresholds and global thresholds.
 */
public abstract class AbstractThreshold extends PageAreaImpl {

    final Control thresholdTarget = control("thresholdTarget");
    private final Control unhealthyThreshold = control("unhealthyThreshold");
    private final Control unstableThreshold = control("unstableThreshold");
    private final Control failUnhealthy = control("failUnhealthy");

    /**
     * Constructor of an AbstractThreshold.
     * @param parent of threshold
     * @param path to threshold
     */
    protected AbstractThreshold(final PageArea parent, final String path) {
        super(parent, path);
    }

    /**
     * Setter for unhealthy-threshold.
     * @param threshold
     *         for unhealthy
     */
    public void setUnhealthyThreshold(final double threshold) {
        unhealthyThreshold.set(threshold);
    }

    /**
     * Setter for unstable-threshold.
     * @param threshold
     *         for unstable
     */
    public void setUnstableThreshold(final double threshold) {
        unstableThreshold.set(threshold);
    }

    /**
     * Setter for fail on unhealthy.
     *
     * @param failOnUnhealthy
     *         boolean for failing on unhealthy
     */
    public void setFailUnhealthy(final boolean failOnUnhealthy) {
        failUnhealthy.check(failOnUnhealthy);
    }

}
