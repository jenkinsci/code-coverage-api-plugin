import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.MainPanel;

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
     * Test for checking the CoverageReport by verifying its CoverageTrend, CoverageOverview, FileCoverageTable and
     * CoverageTrend. Uses a project with two different jacoco files, each one used in another build. Second build uses
     * {@link UITest#JACOCO_ANALYSIS_MODEL_XML}, Third build uses {@link UITest#JACOCO_CODINGSTYLE_XML}.
     */
    @Test
    public void verifyingCoveragePlugin() {
        //create project with first build failing due to no reports
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setFailNoReports(true);
        job.save();
        Build buildWithErrors = JobCreatorUtils.buildWithErrors(job);

        //TODO: tests here for fail on no reports (CoverageSummary?)
        //SummaryTest.NAME(build);

        //create second and third build (successfully), each one containing another jacoco file
        job.configure();
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build firstSuccessfulBuild = JobCreatorUtils.buildSuccessfully(job);

        SummaryTest.testSummaryOnFirstSuccessfulBuild(firstSuccessfulBuild);

        //verify mainPanel not containing trendchart
        MainPanel mainPanelShouldNotContainTrendchart = new MainPanel(job);
        MainPanelTest.verifyTrendChartNotDisplayed(mainPanelShouldNotContainTrendchart);

        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        Build secondSuccessfulBuild = JobCreatorUtils.buildSuccessfully(job);

        SummaryTest.testSummaryOnSecondSuccessfulBuild(secondSuccessfulBuild);

        //verify mainPanel's trendchart
        MainPanel mainPanelShouldContainTrendchart = new MainPanel(job);
        MainPanelTest.verifyTrendChartWithTwoReports(mainPanelShouldContainTrendchart);

        //verify coverageReport (three charts, one table)
        Build buildContainingTwoCoverageReports = job.getLastBuild();
        CoverageReport report = new CoverageReport(buildContainingTwoCoverageReports);
        CoverageReportTest.verify(report);

        //create fourth build failing due to tresholds not achieved
        //TODO: überarbeiten und splitten in 4/5/6/7ten build (failUnhealty, failUnstable, skipPublishingChecks, failDecreased, appyrecursively?
        job.configure();
        jacocoAdapter.createThresholdsPageArea(AdapterThresholdTarget.INSTRUCTION, 4, 4, false);
        coveragePublisher.setApplyThresholdRecursively(true);
        coveragePublisher.setFailUnhealthy(true);
        coveragePublisher.setFailUnstable(true);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        job.save();
        Build failedBuild = JobCreatorUtils.buildWithErrors(job);

        SummaryTest.testSummaryOnFailedBuild(failedBuild);
    }

    /**
     * Test for checking the CoverageReport by verifying its CoverageTrend, CoverageOverview, FileCoverageTable and
     * CoverageTrend. Uses a project with two different jacoco files, each one used in another build. Second build uses
     * {@link UITest#JACOCO_ANALYSIS_MODEL_XML}, Third build uses {@link UITest#JACOCO_CODINGSTYLE_XML}.
     */
    @Test
    public void test() {
        //create project with first build failing due to no reports
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setSourceFileResolver(SourceFileResolver.STORE_ALL_BUILD);

        job.save();
        JobCreatorUtils.buildWithErrors(job);

        //SummaryTest.verify()
    }

}

