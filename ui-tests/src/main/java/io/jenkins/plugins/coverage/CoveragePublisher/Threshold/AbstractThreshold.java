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
        //checkControlsAreAvailable()
        unhealthyThreshold.set(threshold);
    }

    /**
     * Setter for unstable-threshold.
     *
     * @param threshold
     *         for unstable
     */
    public void setUnstableThreshold(double threshold) {
        //checkControlsAreAvailable()
        unstableThreshold.set(threshold);
    }

    /**
     * Setter for fail on unhealthy.
     *
     * @param failOnUnhealthy
     *         boolean for failing on unhealthy
     */
    public void setFailUnhealthy(boolean failOnUnhealthy) {
        //checkControlsAreAvailable()
        failUnhealthy.check(failOnUnhealthy);
    }

    public void delete() {
        delete.click();
    }

    /**
     * Ensures CoverageReport Page is opened.

     public void checkControlsAreAvailable() {
     MatcherAssert.assertThat("threshold controls need to be opened", (thresholdTarget.exists() || unhealthyThreshold.exists() || failUnhealthy.exists()));
     } */
}
