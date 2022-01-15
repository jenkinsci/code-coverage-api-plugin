import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThresholdTarget;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.FileCoverageTable;
import io.jenkins.plugins.coverage.MainPanel;

/**
 * Should in the end contain all tests.
 */
public class UITest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    @SuppressFBWarnings("BC")
    private static final float UNHEALTHY_THRESHOLD = 80;
    private static final float UNSTABLE_THRESHOLD = 90;

    /**
     * Test for checking the CoverageReport by verifying its CoverageTrend, CoverageOverview, FileCoverageTable and
     * CoverageTrend. Uses a project with two different jacoco files, each one used in another build. Second build uses
     * {@link UITest#JACOCO_ANALYSIS_MODEL_XML}, Third build uses {@link UITest#JACOCO_CODINGSTYLE_XML}.
     */
    @Test
    public void verifyingCoveragePlugin() {
        //create project with first build failing due to no reports
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);

        //1st build: fail no reports found
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setFailNoReports(true);
        job.save();
        Build buildWithErrors = JobCreatorUtils.buildWithErrors(job);

        //TODO: tests here for fail on no reports (CoverageSummary?)
        //SummaryTest.NAME(build);

        //2nd build: fail if no reports found but one report
        job.configure();
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build firstSuccessfulBuild = JobCreatorUtils.buildSuccessfully(job);

        SummaryTest.testSummaryOnFirstSuccessfulBuild(firstSuccessfulBuild);

        //verify mainPanel not containing trendchart
        MainPanel mainPanelShouldNotContainTrendchart = new MainPanel(job);
        MainPanelTest.verifyTrendChartNotDisplayed(mainPanelShouldNotContainTrendchart);

        //3rd build:
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        job.save();
        Build thirdBuildFailed = JobCreatorUtils.buildWithErrors(job);

        //4th build:
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(false);
        job.save();
        Build fourthBuildSuccessful = JobCreatorUtils.buildSuccessfully(job);
        //SummaryTest.testSummaryOnSecondSuccessfulBuild(fourthBuildSuccessful);

        job.configure();
        AdapterThreshold threshold = jacocoAdapter.createThresholdsPageArea(AdapterThresholdTarget.FILE,
                UNHEALTHY_THRESHOLD,
                UNSTABLE_THRESHOLD, false);
        job.save();
        Build fifthBuildUnstable = JobCreatorUtils.buildUnstable(job);

        CoverageReport report = new CoverageReport(fifthBuildUnstable);
        report.open();

        FileCoverageTable fileCoverageTable = report.openFileCoverageTable();
        CoverageReportTest.verifyFileCoverageTableContent(fileCoverageTable);
        CoverageReportTest.verifyFileCoverageTableNumberOfMaxEntries(fileCoverageTable, 10);

        String coverageTree = report.getCoverageTree();
        CoverageReportTest.verifyCoverageTree(coverageTree);

        String coverageOverview = report.getCoverageOverview();
        CoverageReportTest.verifyCoverageOverview(coverageOverview);

        String trendChart = report.getCoverageTrend();
        TrendChartTest.verifyTrendChart(trendChart);

        job.configure();
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        threshold.setThresholdTarget(AdapterThresholdTarget.CLASS);
        threshold.setUnhealthyThreshold(99);
        threshold.setUnstableThreshold(5);
        threshold.setFailUnhealthy(true);
        job.save();
        Build sixthBuildFailing = JobCreatorUtils.buildWithErrors(job);

        job.configure();
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        threshold.delete();
        GlobalThreshold globalThreshold = coveragePublisher.createGlobalThresholdsPageArea(GlobalThresholdTarget.FILE,
                59, 59, false);
        job.save();
        Build seventhBuildSuccessfully = JobCreatorUtils.buildSuccessfully(job);

        job.configure();

        coveragePublisher.ensureAdvancedOptionsIsActivated();
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.FILE);
        globalThreshold.setUnhealthyThreshold(60);
        globalThreshold.setUnstableThreshold(60);
        globalThreshold.setFailUnhealthy(false);
        job.save();
        Build eighthBuildSuccessfully = JobCreatorUtils.buildSuccessfully(job);

        job.configure();
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.CLASS);
        globalThreshold.setUnhealthyThreshold(99);
        globalThreshold.setUnstableThreshold(5);
        globalThreshold.setFailUnhealthy(true);
        job.save();
        Build ninthBuildFailing = JobCreatorUtils.buildWithErrors(job);

        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setFailUnhealthy(true);
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.CLASS);
        globalThreshold.setUnhealthyThreshold(99);
        globalThreshold.setUnstableThreshold(5);
        job.save();
        Build tenthBuildFailing = JobCreatorUtils.buildWithErrors(job);

        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.CLASS);
        globalThreshold.setUnhealthyThreshold(0);
        globalThreshold.setUnstableThreshold(95);
        job.save();
        Build eleventhBuildFailing = JobCreatorUtils.buildUnstable(job);

        /**
         * Same build as before but due to setFailUnstable(true) should fail.
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setFailUnstable(true);
        job.save();
        Build twelfthBuildFailing = JobCreatorUtils.buildWithErrors(job);

        /**
         * 5) normale thresholds ohne fail setter
         * 6) normae thresholds mit fail on ...
         * 7) global thresholds ohne setter
         * 8) globale mit set fail unhealthy
         * 9) globale mit set fail unstable
         * 10/11/12) oder nur 10) set storing level
         * 13) publishing checks :(
         */

        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setFailUnstable(false);
        globalThreshold.delete();

        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSourceFileResolver(SourceFileResolver.STORE_ALL_BUILD);
        job.save();
        Build thirteenthBuildFailing = JobCreatorUtils.buildSuccessfully(job);

        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSourceFileResolver(SourceFileResolver.STORE_LAST_BUIlD);
        job.save();
        Build fourteenthBuildFailing = JobCreatorUtils.buildSuccessfully(job);

        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSourceFileResolver(SourceFileResolver.NEVER_STORE);
        job.save();
        Build fifteenthBuildFailing = JobCreatorUtils.buildSuccessfully(job);

        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSkipPublishingChecks(true);
        job.save();
        Build sixteenthBuildFailing = JobCreatorUtils.buildSuccessfully(job);

    }

    @Test
    @Deprecated
    public void oldVerifyingCoveragePlugin() {
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
        jacocoAdapter.createThresholdsPageArea(AdapterThresholdTarget.INSTRUCTION, UNHEALTHY_THRESHOLD,
                UNSTABLE_THRESHOLD, false);
        coveragePublisher.setApplyThresholdRecursively(true);
        coveragePublisher.setFailUnhealthy(true);
        coveragePublisher.setFailUnstable(true);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        job.save();
        Build failedBuild = JobCreatorUtils.buildWithErrors(job);

        SummaryTest.testSummaryOnFailedBuild(failedBuild, UNHEALTHY_THRESHOLD, UNSTABLE_THRESHOLD);
    }

}

