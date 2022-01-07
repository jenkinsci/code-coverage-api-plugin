package io.jenkins.plugins.coverage;

import org.jenkinsci.test.acceptance.po.AbstractStep;
import org.jenkinsci.test.acceptance.po.Control;
import org.jenkinsci.test.acceptance.po.Describable;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageAreaImpl;
import org.jenkinsci.test.acceptance.po.PostBuildStep;

@SuppressWarnings({"unused", "UnusedReturnValue", "PMD.GodClass", "PMD.TooManyFields", "PMD.ExcessivePublicCount"})
@Describable("Publish Coverage Report")
public class CoveragePublisher extends AbstractStep implements PostBuildStep {
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

    private final Control globalThresholds = control("/repeatable-add");

    private boolean advancedOptionsActivated = false;

    public CoveragePublisher(final Job parent, final String path) {
        super(parent, path);
    }

    public void setApplyThresholdRecursively(boolean applyTresholds) {
        ensureAdvancedOptionsIsActivated();
        applyThresholdRecursively.check(applyTresholds);
    }

    public void setFailUnhealthy(boolean failOnUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        failUnhealthy.check(failOnUnhealthy);
    }

    public void setFailUnstable(boolean failOnUnstable) {
        ensureAdvancedOptionsIsActivated();
        failUnstable.check(failOnUnstable);
    }

    public void setFailNoReports(boolean failOnNoReports) {
        ensureAdvancedOptionsIsActivated();
        failNoReports.check(failOnNoReports);
    }

    public void setFailBuildIfCoverageDecreasedInChangeRequest(boolean failOnCoverageDecreases) {
        ensureAdvancedOptionsIsActivated();
        failBuildIfCoverageDecreasedInChangeRequest.check(failOnCoverageDecreases);
    }

    public void setSkipPublishingChecks(boolean skipPublishing) {
        ensureAdvancedOptionsIsActivated();
        skipPublishingChecks.check(skipPublishing);
    }

    private void ensureAdvancedOptionsIsActivated() {
        if(!advancedOptionsActivated) {
            advancedOptions.click();
            advancedOptionsActivated =true;
        }
    }

    public Adapter createAdapterPageArea(String adapter) {
        String path = createPageArea("adapters", () -> this.adapter.selectDropdownMenu(adapter));
        return new Adapter(this, path);
    }


    public GlobalThreshold createGlobalThresholdsPageArea() {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalthresholds", () -> this.globalThresholds.click());
        return new GlobalThreshold(this, path);
    }


    public GlobalThreshold createGlobalThresholdsPageArea(String thresholdTarget, double unhealthyThreshold, double unstableThreshold, boolean failUnhealthy) {
        ensureAdvancedOptionsIsActivated();
        String path = createPageArea("globalThresholds", () -> this.globalThresholds.click());
        GlobalThreshold threshold = new GlobalThreshold(this, path);
        threshold.setThresholdTarget(thresholdTarget);
        threshold.setUnhealthyThreshold(unhealthyThreshold);
        threshold.setUnstableThreshold(unstableThreshold);
        threshold.setFailUnhealthy(failUnhealthy);
        return threshold;
    }

    public void setSourceFileResolver(SourceFileResolver storingLevel){
        ensureAdvancedOptionsIsActivated();
        sourceFileStoringLevel.select(storingLevel.getName());
    }


    //TODO: austauschen
    public enum SourceFileResolver {
        NEVER_SAVE_SOURCE_FILES("never save source files"),
        SAVE_LAST_BUIlD_SOURCE_FILES("save last build source files"),
        SAVE_ALL_BUILD_SOURCE_FILES("save all build source files"),
        ;

        private final String name;

        SourceFileResolver(final String name) {
            this.name=name;
        }

        public String getName() {
            return name;
        }
    }

    /*public AdvancedOptionsForPublisher createAdvancedOptionsArea() {

        String path = createPageArea("", () -> AdvancedOptionsForPublisher.click());

        AdvancedOptionsForPublisher advancedOptionsForPublisher = new AdvancedOptionsForPublisher(this, path);
        return advancedOptionsForPublisher;

    }*/

    void setAdapter(String adapter) {
        this.adapter.selectDropdownMenu(adapter);
    }

    void deleteAdapter(){
        adapter.click();
    }



    public static class Adapter extends PageAreaImpl {
        private final Control reportFilePath = control("path");
        private final Control advancedOptionsForAdapter = control("advanced-button");

        Adapter(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);

        }

        public void setReportFilePath(String reportFilePath) {
            this.reportFilePath.set(reportFilePath);
        }

        AdvancedOptionsForAdapter createAdvancedOptionsForAdapterPageArea(String adapter) {
            String path = createPageArea("", () -> advancedOptionsForAdapter.click());
            return new AdvancedOptionsForAdapter(this, path);
        }
    }



    public static class AdvancedOptionsForAdapter extends PageAreaImpl {
        AdvancedOptionsForAdapter(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);
        }

    }

    public static class GlobalThreshold extends PageAreaImpl {

        private final Control thresholdTarget = control("thresholdTarget"); //DD
        private final Control unhealthyThreshold = control("unhealthyThreshold"); //input
        private final Control unstableThreshold = control("unstableThreshold"); //input
        private final Control failUnhealthy = control("failUnhealthy"); //cbox

        GlobalThreshold(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);
        }

        public void setThresholdTarget(String target){
            thresholdTarget.select(target);
        }

        public void setUnhealthyThreshold(double threshold){
            unhealthyThreshold.set(threshold);

        }
        public void setUnstableThreshold(double threshold){
            unstableThreshold.set(threshold);
        }

        public void setFailUnhealthy(boolean failOnUnhealthy){
            failUnhealthy.check(failOnUnhealthy);
        }
    }


}