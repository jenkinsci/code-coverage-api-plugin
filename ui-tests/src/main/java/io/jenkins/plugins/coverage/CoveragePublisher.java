package io.jenkins.plugins.coverage;

import java.util.List;

import org.jenkinsci.test.acceptance.po.AbstractStep;
import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.Describable;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;
import org.jenkinsci.test.acceptance.po.PostBuildStep;

/**
 * Coverage Publisher which can be added in the configuration of a FreeStyle Project.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "PMD.GodClass", "PMD.TooManyFields", "PMD.ExcessivePublicCount"})
@Describable("Publish Coverage Report")
public class CoveragePublisher extends AbstractStep implements PostBuildStep {
    private List<Adapter> adapters = getAdapters();
    private List<Threshold> globalThresholds = getGlobalThresholds();


    private final Control adapter = control("hetero-list-add[adapters]");
    private final Control deleteAdapter = control("adapters/repeatable-delete");

    private final Control advancedOptions = control("advanced-button");
    private final Control applyThresholdRecursively = control("applyThresholdRecursively");
    private final Control failUnhealthy = control("failUnhealthy");
    private final Control failUnstable = control("failUnstable");
    private final Control failNoReports = control("failNoReports");
    private final Control failBuildIfCoverageDecreasedInChangeRequest = control(
            "failBuildIfCoverageDecreasedInChangeRequest");
    private final Control skipPublishingChecks = control("skipPublishingChecks");
    private final Control sourceFileStoringLevel = control("sourceFileResolver/level");

    private final Control globalThreshold = control("/repeatable-add");

    private boolean advancedOptionsActivated = false;

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

    /**
     * Setter for applying threshold recursively.
     *
     * @param applyTresholds
     *         boolean for using applying threshold recursively
     */

    /**
     * Getter for all adapters applied in current CoveragePublisher configuration.
     * @return all adapters applied
     */
    public List<Adapter> getAdapters() {
        //TODO:
        return null;
    }

    /**
     * Getter for GlobalThresholds of CoveragePublisher.
     */
    private List<Threshold> getGlobalThresholds() {
        //TODO
        return null;
    }

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
     * Ensures advanced options of CoveragePublisher, a status which is stored in {@link
     * CoveragePublisher#advancedOptionsActivated} is activated.
     */
    private void ensureAdvancedOptionsIsActivated() {
        if (!advancedOptionsActivated) {
            //TODO: change condition, use get advanced page area by id
            advancedOptions.click();
            advancedOptionsActivated = true;
        }
    }

    /**
     * Creates an {@link Adapter} for {@link CoveragePublisher}.
     *
     * @param adapter
     *         type which should be created, f. e. jacoco or cobertura
     *
     * @return added {@link Adapter}
     */
    public Adapter createAdapterPageArea(final String adapter) {
        String path = createPageArea("adapters", () -> this.adapter.selectDropdownMenu(adapter));
        return new Adapter(this, path);
    }

    /**
     * Creates {@link Threshold} for {@link Adapter}.
     *
     * @return added {@link Adapter}
     */
    public Threshold createGlobalThresholdsPageArea() {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalthresholds", () -> this.globalThreshold.click());
        return new Threshold(this, path);
    }

    /**
     * Creates {@link Threshold} for {@link Adapter}.
     *
     * @param thresholdTarget
     *         which should be setted
     * @param unhealthyThreshold
     *         which should be setted
     * @param unstableThreshold
     *         which should be setted
     * @param failUnhealthy
     *         boolean for failing build on unhealthy
     *
     * @return added {@link Adapter} with setted configuration
     */
    public Threshold createGlobalThresholdsPageArea(final String thresholdTarget, final double unhealthyThreshold,
            final double unstableThreshold, final boolean failUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalThresholds", () -> this.globalThreshold.click());
        Threshold globalThreshold = new Threshold(this, path);
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
    public void setSourceFileResolver(final SourceFileResolver storingLevel) {
        ensureAdvancedOptionsIsActivated();
        sourceFileStoringLevel.select(storingLevel.getName());
    }

    //TODO: delete?
    /*void setAdapter(final String adapter) {
        this.adapter.selectDropdownMenu(adapter);
    }*/

    //TODO: remove all adapters? -> add adapter.delete()?
    /**
     * Removes adapter from parent {@link CoveragePublisher}.
     */
    public void deleteAdapter() {
        adapter.click();
    }

    //TODO: austauschen?
    public enum SourceFileResolver {
        //value
        NEVER_SAVE_SOURCE_FILES("never save source files"),
        SAVE_LAST_BUIlD_SOURCE_FILES("save last build source files"),
        SAVE_ALL_BUILD_SOURCE_FILES("save all build source files"),
        ;

        private final String name;

        SourceFileResolver(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Adapter which can be added in the configuration of the {@link CoveragePublisher} of a FreeStyle Project.
     */
    public static class Adapter extends PageAreaImpl {
        private List<Threshold> thresholds = getThresholds();
        private final Control reportFilePath = control("path");
        private final Control advancedOptions = control("advanced-button");
        private final Control mergeToOneReport = control("mergeToOneReport"); //DD
        private final Control threshold = control("repeatable-add"); //input

        //FIXME
        private final Control advanced = control("repeatable-add"); //input
        private boolean advancedOptionsActivated = false;


        /**
         * Constructor to create {@link Adapter} for {@link CoveragePublisher}.
         * @param reportPublisher which should be created, f. e. jacoco or cobertura
         * @param path of parent page
         */
        public Adapter(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);

        }

        /**
         * Getter for Thresholds of Adapter
         */
        private List<Threshold> getThresholds() {
            //TODO
            return null;
        }

        /**
         * Setter for path of report file.
         * @param reportFilePath path to report file.
         */
        public void setReportFilePath(final String reportFilePath) {
            this.reportFilePath.set(reportFilePath);
        }

        /**
         * Setter for merging to one report.
         * @param mergeReports boolean for merging to one report
         */
        public void setMergeToOneReport(final boolean mergeReports) {
            ensureAdvancedOptionsIsActivated();
            mergeToOneReport.check(mergeReports);
        }

        /**
         *
         * @return
         */
        public Threshold createGlobalThresholdsPageArea() {
            ensureAdvancedOptionsIsActivated();
            String path = createPageArea("thresholds", () -> this.threshold.click());
            return new Threshold(this, path);
        }

        /**
         *
         * @param thresholdTarget
         * @param unhealthyThreshold
         * @param unstableThreshold
         * @param failUnhealthy
         * @return
         */
        public Threshold createGlobalThresholdsPageArea(final String thresholdTarget,
                final double unhealthyThreshold,
                final double unstableThreshold, final boolean failUnhealthy) {
            ensureAdvancedOptionsIsActivated();
            String path = createPageArea("thresholds", () -> this.threshold.click());
            Threshold threshold = new Threshold(this, path);
            threshold.setThresholdTarget(thresholdTarget);
            threshold.setUnhealthyThreshold(unhealthyThreshold);
            threshold.setUnstableThreshold(unstableThreshold);
            threshold.setFailUnhealthy(failUnhealthy);
            return threshold;
        }

        /**
         *
         */
        private void ensureAdvancedOptionsIsActivated() {
            if (!advancedOptionsActivated) {
                advancedOptions.click();
                advancedOptionsActivated = true;
            }
        }
    }

    /**
     * Used for thresholds and global thresholds.
     */
    public static class Threshold extends PageAreaImpl {

        private final Control thresholdTarget = control("thresholdTarget");
        private final Control unhealthyThreshold = control("unhealthyThreshold");
        private final Control unstableThreshold = control("unstableThreshold");
        private final Control failUnhealthy = control("failUnhealthy");

        /**
         *
         * @param reportPublisher
         * @param path
         */
        Threshold(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);
        }

        /**
         *
         * @param target
         */
        public void setThresholdTarget(final String target) {
            thresholdTarget.select(target);
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
}