package io.jenkins.plugins.coverage.CoveragePublisher;

import org.jenkinsci.test.acceptance.po.AbstractStep;
import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.Describable;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;
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
    private final Control sourceCodeEncoding = control("sourceCodeEncoding");
    private final Control sourceDirectories = findRepeatableAddButtonFor("sourceDirectories");
    private final Control checksName = control("checksName");

    /**
     * Constructor for CoveragePublisher.
     *
     * @param parent
     *         is the job which uses the CoveragePublisher
     * @param path
     *         on the parent page
     */
    public CoveragePublisher(final Job parent, final String path) {
        super(parent, path);
    }

    private Control findRepeatableAddButtonFor(final String propertyName) {
        return control(by.xpath("//div[@id='" + propertyName + "']//button[contains(@path,'-add')]"));
    }

    /**
     * Sets the encoding when reading source code files.
     *
     * @param sourceCodeEncoding
     *         the source code encoding (e.g., UTF-8)
     *
     * @return this publisher
     */
    public CoveragePublisher setSourceCodeEncoding(final String sourceCodeEncoding) {
        ensureAdvancedOptionsIsActivated();

        this.sourceCodeEncoding.set(sourceCodeEncoding);

        return this;
    }

    /**
     * Adds the path to the folder that contains the source code. If not relative and thus not part of the workspace
     * then this folder needs to be added in Jenkins global configuration.
     *
     * @param sourceDirectory
     *         a folder containing the source code
     *
     * @return this publisher
     */
    public CoveragePublisher addSourceDirectory(final String sourceDirectory) {
        ensureAdvancedOptionsIsActivated();

        String path = createPageArea("sourceDirectories", sourceDirectories::click);
        SourceCodeDirectoryPanel panel = new SourceCodeDirectoryPanel(this, path);
        panel.setPath(sourceDirectory);

        return this;
    }

    /**
     * Setter for applying threshold recursively.
     *
     * @param applyTresholds
     *         boolean for using applying threshold recursively
     */
    public void setApplyThresholdRecursively(final boolean applyTresholds) {
        ensureAdvancedOptionsIsActivated();
        applyThresholdRecursively.check(applyTresholds);
    }

    /**
     * Setter for fail on unhealthy.
     *
     * @param failOnUnhealthy
     *         boolean for failing on unhealthy
     */
    public void setFailUnhealthy(final boolean failOnUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        failUnhealthy.check(failOnUnhealthy);
    }

    /**
     * Setter for fail on unstable.
     *
     * @param failOnUnstable
     *         boolean for failing on unstable
     */
    public void setFailUnstable(final boolean failOnUnstable) {
        ensureAdvancedOptionsIsActivated();
        failUnstable.check(failOnUnstable);
    }

    /**
     * Setter for fail on no reports.
     *
     * @param failOnNoReports
     *         boolean for fail on no reports
     */
    public void setFailNoReports(final boolean failOnNoReports) {
        ensureAdvancedOptionsIsActivated();
        failNoReports.check(failOnNoReports);
    }

    /**
     * Setter for fail build if coverage decreased in Change Request.
     *
     * @param failOnCoverageDecreases
     *         boolean for failing if coverage decreased in Change Request
     */
    public void setFailBuildIfCoverageDecreasedInChangeRequest(final boolean failOnCoverageDecreases) {
        ensureAdvancedOptionsIsActivated();
        failBuildIfCoverageDecreasedInChangeRequest.check(failOnCoverageDecreases);
    }

    /**
     * Setter for skipping publishing checks.
     *
     * @param skipPublishing
     *         boolean for skipping publishing checks
     */
    public void setSkipPublishingChecks(final boolean skipPublishing) {
        ensureAdvancedOptionsIsActivated();
        skipPublishingChecks.check(skipPublishing);
    }

    /**
     * Setter for setting the SCM checks name.
     *
     * @param checksName
     *         the SCM check name
     */
    public void setChecksName(final String checksName) {
        ensureAdvancedOptionsIsActivated();
        this.checksName.set(checksName);
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
    public Adapter createAdapterPageArea(final String adapterName) {
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
     * @param failOnUnhealthy
     *         boolean for failing build on unhealthy
     *
     * @return added {@link Adapter} with setted configuration
     */
    public GlobalThreshold createGlobalThresholdsPageArea(final GlobalThresholdTarget thresholdTarget,
            final double unhealthyThreshold,
            final double unstableThreshold, final boolean failOnUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalThresholds", () -> this.globalThreshold.click());
        GlobalThreshold threshold = new GlobalThreshold(this, path);
        threshold.setThresholdTarget(thresholdTarget);
        threshold.setUnhealthyThreshold(unhealthyThreshold);
        threshold.setUnstableThreshold(unstableThreshold);
        threshold.setFailUnhealthy(failOnUnhealthy);
        return threshold;
    }

    /**
     * Setter for Source File Resolver.
     *
     * @param storingLevel
     *         which should be applied
     */
    public void setSourceFileResolver(final SourceFileResolver storingLevel) {
        ensureAdvancedOptionsIsActivated();
        sourceFileResolver.select(storingLevel.getValue());
    }

    /**
     * Enum for Options of Source File Storing Level of {@link CoveragePublisher}.
     */
    public enum SourceFileResolver {
        NEVER_STORE("NEVER_STORE"),
        STORE_LAST_BUIlD("STORE_LAST_BUILD"),
        STORE_ALL_BUILD("STORE_ALL_BUILD");

        private final String value;

        /**
         * Constructor of enum.
         *
         * @param value
         *         is value-attribute of option-tag.
         */
        SourceFileResolver(final String value) {
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

    /**
     * Page area of a source code path configuration.
     */
    private static class SourceCodeDirectoryPanel extends PageAreaImpl {
        private final Control path = control("path");

        SourceCodeDirectoryPanel(final PageArea area, final String path) {
            super(area, path);
        }

        public void setPath(final String path) {
            this.path.set(path);
        }
    }
}





