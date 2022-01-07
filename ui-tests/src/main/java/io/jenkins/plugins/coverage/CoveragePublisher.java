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

            AdvancedOptionsForAdapter advancedOptionsForAdapter = new AdvancedOptionsForAdapter(this, path);
            return advancedOptionsForAdapter;

        }

    }



    public static class AdvancedOptionsForAdapter extends PageAreaImpl {
        AdvancedOptionsForAdapter(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);
        }

    }

}