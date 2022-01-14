package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * Used for thresholds and global thresholds.
 */
public abstract class AbstractThreshold extends PageAreaImpl {

    final Control thresholdTarget = control("thresholdTarget");
    private final Control unhealthyThreshold = control("unhealthyThreshold");
    private final Control unstableThreshold = control("unstableThreshold");
    private final Control failUnhealthy = control("failUnhealthy");

    protected AbstractThreshold(final PageObject context, final String path) {
        super(context, path);
    }

    protected AbstractThreshold(final PageArea area, final String path) {
        super(area, path);
    }

    /**
     *
     * @param threshold
     */
    public void setUnhealthyThreshold(final double threshold) {
        unhealthyThreshold.set(threshold);
    }

    /**
     *
     * @param threshold
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
