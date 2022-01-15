package io.jenkins.plugins.coverage.CoveragePublisher;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;


import org.jenkinsci.test.acceptance.po.AbstractStep;
import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.Describable;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;
import org.jenkinsci.test.acceptance.po.PostBuildStep;


import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThresholdTarget;

/**
 * Coverage Publisher which can be added in the configuration of a FreeStyle Project.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "PMD.GodClass", "PMD.TooManyFields", "PMD.ExcessivePublicCount"})
@Describable("Publish Coverage Report")
public class CoveragePublisher extends AbstractStep implements PostBuildStep {
    private List<Adapter> adapters = getAdapters();
    private List<GlobalThreshold> globalThresholds = getGlobalThresholds();


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
        this.adapters = new ArrayList<>();
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
     * Getter for all adapters applied in current CoveragePublisher configuration.
     * @return all adapters applied
     */
    public List<Adapter> getAdapters() {
       return this.adapters;
    }

    /**
     * Getter for GlobalThresholds of CoveragePublisher.
     */
    private List<GlobalThreshold> getGlobalThresholds() {
        //TODO
        return null;
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
     * Ensures advanced options of CoveragePublisher is activated,
     * so that values like {@link CoveragePublisher#setFailUnhealthy(boolean)} or
     * {@link CoveragePublisher#setFailNoReports(boolean)} are visible and can be set.
     */
    public void ensureAdvancedOptionsIsActivated() {
        if (advancedOptions.exists()) {
            advancedOptions.click();
        }
    }


    /**
     * Returns if Element is displayed.
     * @param elementId of chart
     * @return if chart is displayed
     */
    private boolean isElementDisplayed(final String elementId){
        return find(By.id(elementId))!=null;
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
        adapters.add(newAdapter);
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
    public GlobalThreshold createGlobalThresholdsPageArea(final GlobalThresholdTarget thresholdTarget, final double unhealthyThreshold,
            final double unstableThreshold, final boolean failUnhealthy) {
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
    public void setSourceFileResolver(final SourceFileResolver storingLevel) {
        ensureAdvancedOptionsIsActivated();
        sourceFileStoringLevel.select(storingLevel.getName());
    }

    //TODO: delete?
    /*void setAdapter(final String adapter) {
        this.adapter.selectDropdownMenu(adapter);
    }*/




    public enum SourceFileResolver {

        NEVER_STORE("NEVER_STORE"),
        STORE_LAST_BUIlD("STORE_LAST_BUILD"),
        STORE_ALL_BUILD("STORE_ALL_BUILD"),
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
        private CoveragePublisher parent;
        private List<AdapterThreshold> thresholds = getThresholds();
        private final Control reportFilePath = control("path");
        private final Control advancedOptions = control("advanced-button");
        private final Control mergeToOneReport = control("mergeToOneReport"); //DD
        private final Control threshold = control("repeatable-add"); //input
        private final Control delete = control("repeatable-delete"); //input

        //FIXME
        private final Control advanced = control("repeatable-add"); //input



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
        private List<AdapterThreshold> getThresholds() {
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
        public AdapterThreshold createThresholdsPageArea() {
            ensureAdvancedOptionsIsActivated();
            String path = createPageArea("thresholds", () -> this.threshold.click());
            return new AdapterThreshold(this, path);
        }

        /**
         *
         * @param thresholdTarget
         * @param unhealthyThreshold
         * @param unstableThreshold
         * @param failUnhealthy
         * @return
         */
        public AdapterThreshold createThresholdsPageArea(final AdapterThresholdTarget thresholdTarget,
                final double unhealthyThreshold,
                final double unstableThreshold, final boolean failUnhealthy) {
            ensureAdvancedOptionsIsActivated();
            String path = createPageArea("thresholds", () -> this.threshold.click());
            AdapterThreshold threshold = new AdapterThreshold(this, path);
            threshold.setThresholdTarget(thresholdTarget);
            threshold.setUnhealthyThreshold(unhealthyThreshold);
            threshold.setUnstableThreshold(unstableThreshold);
            threshold.setFailUnhealthy(failUnhealthy);
            return threshold;
        }

        /**
         *
         */
        public void ensureAdvancedOptionsIsActivated() {
            if (advancedOptions.exists()) {
                advancedOptions.click();
            }
        }

        //TODO: remove all adapters? -> add adapter.delete()?
        /**
         * Removes adapter from parent {@link CoveragePublisher}.
         */
        public void deleteAdapter() {
            this.delete.click();
            //TODO: remove from list?
        }


    }





}