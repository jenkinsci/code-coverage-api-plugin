import java.net.URL;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Resource;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.CoverageSummary;
import io.jenkins.plugins.coverage.MainPanel;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/**
 * Should in the end contain all tests.
 */
public class UITest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    @SuppressFBWarnings("BC")
    private static final String FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Creates a first test which returns ... (-> siehe jelly) //TODO: javadoc hier später anpassen
     */
    @Test
    public void createJob() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage/jacoco-analysis-model.xml");
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(FILE_NAME);
        /*jacocoAdapter.setMergeToOneReport(true);
        jacocoAdapter.createGlobalThresholdsPageArea("Instruction", 4, 4, false);
        coveragePublisher.setApplyThresholdRecursively(true);
        coveragePublisher.setFailUnhealthy(true);
        coveragePublisher.setFailUnstable(true);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        coveragePublisher.setFailNoReports(true);
        coveragePublisher.setFailNoReports(true);
        coveragePublisher.setSourceFileResolver(SourceFileResolver.NEVER_SAVE_SOURCE_FILES);*/
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        build.open();
        CoverageSummary summary = new CoverageSummary(build, "coverage");
        //cs.openCoverageReport();
        CoverageReport report = summary.openCoverageReport();
        report.getActiveTab();
        report.openTabCoverageTable();
        //report.openCoverageTree();

        //report.verfiesOverview();
        //String coverageTable = report.getCoverageTable();
        //String coverageDetails = report.getCoverageDetails();

        //String coverageTrend = report.getCoverageTrend();
        //String coverageOverview = report.getCoverageOverview();

        //boolean coverageTreeVisible = report.isCoverageTreeVisible();

        // cr.verfiesOverview();

        //Irgendwie CodeCoverage = new Irgendwie(build, "coverage");
        //CodeCoverage.open();
    }


    @Test
    public void createJobForForGettingProjectStatus() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage/jacoco-analysis-model.xml");
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(FILE_NAME);
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        Build build2 = JobCreatorUtils.buildSuccessfully(job);
        Build build3 = JobCreatorUtils.buildSuccessfully(job);

        job.open();
        MainPanel mp = new MainPanel(build3, "");
        mp.getTrendChart();

    }



}

