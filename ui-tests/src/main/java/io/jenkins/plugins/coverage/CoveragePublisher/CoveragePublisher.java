package io.jenkins.plugins.coverage.CoveragePublisher;

import org.jenkinsci.test.acceptance.po.AbstractStep;
import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.Describable;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PostBuildStep;

import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.GlobalThresholdTarget;

/**
 * Coverage Publisher which can be added in the configuration of a FreeStyle Project.
 */
@Describable("Publish Coverage Report")
public class CoveragePublisher extends AbstractStep implements PostBuildStep {

    private final Control adapter = control("hetero-list-add[adapters]");
    private final Control advancedOptions = control("advanced-button");
    private final Control applyThresholdRecursively = control("applyThresholdRecursively");
    private final Control failUnhealthy = control("failUnhealthy");
    private final Control failUnstable = control("failUnstable");
    private final Control failNoReports = control("failNoReports");
    private final Control failBuildIfCoverageDecreasedInChangeRequest = control(
            "failBuildIfCoverageDecreasedInChangeRequest");
    private final Control skipPublishingChecks = control("skipPublishingChecks");
    private final Control sourceFileResolver = control("sourceFileResolver/level");
    private final Control globalThreshold = control("repeatable-add");

    /**
     * Constructor for CoveragePublisher.
     *
     * @param parent
     *         is the job which uses the CoveragePublisher
     * @param path
     *         on the parent page
     */
    public CoveragePublisher(Job parent, String path) {
        super(parent, path);
    }

    /**
     * Setter for applying threshold recursively.
     *
     * @param applyTresholds
     *         boolean for using applying threshold recursively
     */
    public void setApplyThresholdRecursively(boolean applyTresholds) {
        ensureAdvancedOptionsIsActivated();
        applyThresholdRecursively.check(applyTresholds);
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
     * Setter for fail on unstable.
     *
     * @param failOnUnstable
     *         boolean for failing on unstable
     */
    public void setFailUnstable(boolean failOnUnstable) {
        ensureAdvancedOptionsIsActivated();
        failUnstable.check(failOnUnstable);
    }

    /**
     * Setter for fail on no reports.
     *
     * @param failOnNoReports
     *         boolean for fail on no reports
     */
    public void setFailNoReports(boolean failOnNoReports) {
        ensureAdvancedOptionsIsActivated();
        failNoReports.check(failOnNoReports);
    }

    /**
     * Setter for fail build if coverage decreased in Change Request.
     *
     * @param failOnCoverageDecreases
     *         boolean for failing if coverage decreased in Change Request
     */
    public void setFailBuildIfCoverageDecreasedInChangeRequest(boolean failOnCoverageDecreases) {
        ensureAdvancedOptionsIsActivated();
        failBuildIfCoverageDecreasedInChangeRequest.check(failOnCoverageDecreases);
    }

    /**
     * Setter for skipping publishing checks.
     *
     * @param skipPublishing
     *         boolean for skipping publishing checks
     */
    public void setSkipPublishingChecks(boolean skipPublishing) {
        ensureAdvancedOptionsIsActivated();
        skipPublishingChecks.check(skipPublishing);
    }

    /**
     * Ensures advanced options of CoveragePublisher is activated, so that values like {@link
     * CoveragePublisher#setFailUnhealthy(boolean)} or {@link CoveragePublisher#setFailNoReports(boolean)} are visible
     * and can be set.
     */
    public void ensureAdvancedOptionsIsActivated() {
        if (advancedOptions.exists()) {
            advancedOptions.click();
        }
    }

    /**
     * Creates an {@link Adapter} for {@link CoveragePublisher}.
     *
     * @param adapterName
     *         type which should be created, f. e. jacoco or cobertura
     *
     * @return added {@link Adapter}
     */
    public Adapter createAdapterPageArea(String adapterName) {
        String path = createPageArea("adapters", () -> this.adapter.selectDropdownMenu(adapterName));
        Adapter newAdapter = new Adapter(this, path);
        return newAdapter;
    }

    /**
     * Creates {@link GlobalThreshold} for {@link Adapter}.
     *
     * @return added {@link Adapter}
     */
    public GlobalThreshold createGlobalThresholdsPageArea() {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalthresholds", () -> this.globalThreshold.click());
        return new GlobalThreshold(this, path);
    }

    /**
     * Creates {@link GlobalThreshold} for {@link Adapter}.
     *
     * @param thresholdTarget
     *         which should be set
     * @param unhealthyThreshold
     *         which should be set
     * @param unstableThreshold
     *         which should be set
     * @param failUnhealthy
     *         boolean for failing build on unhealthy
     *
     * @return added {@link Adapter} with setted configuration
     */
    public GlobalThreshold createGlobalThresholdsPageArea(GlobalThresholdTarget thresholdTarget,
            double unhealthyThreshold,
            double unstableThreshold, boolean failUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalThresholds", () -> this.globalThreshold.click());
        GlobalThreshold globalThreshold = new GlobalThreshold(this, path);

        globalThreshold.setThresholdTarget(thresholdTarget);
        globalThreshold.setUnhealthyThreshold(unhealthyThreshold);
        globalThreshold.setUnstableThreshold(unstableThreshold);
        globalThreshold.setFailUnhealthy(failUnhealthy);
        return globalThreshold;
    }

    /**
     * Setter for Source File Resolver.
     *
     * @param storingLevel
     *         which should be applied
     */
    public void setSourceFileResolver(SourceFileResolver storingLevel) {
        ensureAdvancedOptionsIsActivated();
        sourceFileResolver.select(storingLevel.getValue());
    }

    /**
     * Enum for Options of Source File Storing Level of {@link CoveragePublisher}.
     */
    public enum SourceFileResolver {
        NEVER_STORE("NEVER_STORE"),
        STORE_LAST_BUIlD("STORE_LAST_BUILD"),
        STORE_ALL_BUILD("STORE_ALL_BUILD"),
        ;

        private final String value;

        /**
         * Constructor of enum.
         *
         * @param value
         *         is value-attribute of option-tag.
         */
        SourceFileResolver(String value) {
            this.value = value;
        }

        /**
         * Get value of option-tag which should be selected.
         *
         * @return value of option-tag to select.
         */
        public String getValue() {
            return value;
        }
    }

}





