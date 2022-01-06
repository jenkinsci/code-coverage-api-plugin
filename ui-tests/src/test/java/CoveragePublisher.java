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
    private final Control adaptertype = control("hetero-list-add[adapters]");
    private final Control deleteAdapter = control("adapters/repeatable-delete");
    private final Control AdvancedOptionsForAdapter = control("advanced-button");

    public CoveragePublisher(final Job parent, final String path) {
        super(parent, path);
    }



    Adapter createAdapterPageArea(String adapter) {

        String path = createPageArea("adapters", () -> adaptertype.selectDropdownMenu(adapter));

        Adapter adapterp = new Adapter(this, path);
        return adapterp;

    }

    void setAdapter(String adapter) {
        adaptertype.selectDropdownMenu(adapter);
    }

    void deleteAdapter(){
        adaptertype.click();
    }

    public static class Adapter extends PageAreaImpl {
        private final Control reportFilePath = control("adapters/path");
        private final Control advancedOptionsForAdapter = control("adapters/advanced-button");

        Adapter(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);

        }

        void setReportFilePath(String reportFilePath) {
            this.reportFilePath.set(reportFilePath);
        }

        AdvancedOptionsForAdapter createAdvancedOptionsForAdapterPageArea(String adapter) {

            String path = createPageArea("", () -> advancedOptionsForAdapter.click());

            AdvancedOptionsForAdapter advancedOptionsForAdapter = new AdvancedOptionsForAdapter(this, path);
            return advancedOptionsForAdapter;

        }

    }

    public static class AdvancedOptionsForAdapter extends PageAreaImpl {
        private final Control failUnhealthy = control("adapters/failUnhealthy");
        private final Control failUnstable = control("adapters/failNoReports");
        private final Control failNoReports = control("adapters/failBuildIfCoverageDecreasedInChangeRequest");
        private final Control failBuildIfCoverageDecreasedInChangeRequest = control(
                "adapters/failBuildIfCoverageDecreasedInChangeRequest");
        private final Control skipPublishingChecks = control("adapters/skipPublishingChecks");

        AdvancedOptionsForAdapter(final PageArea reportPublisher, final String path) {
            super(reportPublisher, path);
        }

        void setFailUnhealthy(boolean failOnUnhealthy) {
            failUnhealthy.check(failOnUnhealthy);
        }

        void setFailUnstable(boolean failOnUnstable) {
            failUnstable.check(failOnUnstable);
        }

        void setFailNoReports(boolean failOnNoReports) {
            failNoReports.check(failOnNoReports);
        }

        void setFailBuildIfCoverageDecreasedInChangeRequest(boolean failOnCoverageDecreases) {
            failBuildIfCoverageDecreasedInChangeRequest.check(failOnCoverageDecreases);
        }

        void setSkipPublishingChecks(boolean skipPublishing) {
            skipPublishingChecks.check(skipPublishing);
        }

    }

}