import org.junit.Test;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold.*;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.*;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.FileCoverageTable;
import io.jenkins.plugins.coverage.MainPanel;

/**
 * Should in the end contain all tests.
 */
public class SmokeTests extends UiTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";


    /**
     * Test for verifying CoveragePlugin by checking its behaviour in different sitauations, using a project
     * with two different jacoco files.
     *
     * Different scenarios are used in each build, see javadoc.
     *
     * Verifies correct build-status depending on its configuration like used thresholds, global thresholds, fail on no report, etc.
     * Verifies CoverageSummary.
     * Verifies MainPanel (CoverageTrend).
     * Verifies CoverageReport (CoverageTrend, CoverageOverview, FileCoverageTable and CoverageTree and its pages).
     */
    @Test
    public void verifyingCoveragePlugin() {
        //create project with first build failing due to no reports
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);


        //FIXME: uberlegen wo welcher test sinnvoll ist, dann testen und in javadoc ueber build ergaenzen was getestet wird, ggf auch warum

        /**
         * 1st build: Set setFailNoReports(true) and don't add any report, so that build should fail.
         * Check if build fails.
         */
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setFailNoReports(true);
        job.save();
        Build buildWithErrors = buildWithErrors(job);

        //TODO: tests here for fail on no reports (CoverageSummary?)
        //SummaryTest.NAME(build);

        /**
         * 2nd build: Set setFailNoReports(true) and add a report, so that build should succeed.
         * Check if build is successful.
         * Check SummaryTest if ....
         */
        job.configure();
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build firstSuccessfulBuild = buildSuccessfully(job);

        SummaryTest.testSummaryOnFirstSuccessfulBuild(firstSuccessfulBuild);

        //verify mainPanel not containing trendchart
        /**
         * 3rd build: Replace report-file in build configuration, set setFailBuildIfCoverageDecreasedInChangeRequest(true),
         * so that build should fail.
         * Check if build failed.
         * Check if ...
         */
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        job.save();
        Build thirdBuildFailed = buildWithErrors(job);
        //gibt die summary hier was her?

        /**
         * 4th build: Set setFailBuildIfCoverageDecreasedInChangeRequest(false), so that build should now succeed.
         * Check if build is successful.
         * Check if...
         */
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(false);
        job.save();
        Build fourthBuildSuccessful = buildSuccessfully(job);
        //SummaryTest.testSummaryOnSecondSuccessfulBuild(fourthBuildSuccessful);

        /**
         * 5th build: Add threshold so that build should be unstable.
         * Check CoverageReport (FileCoverageTable, CoverageTree, CoverageOverview, TrendChart)
         * Check MainPanel
         */
        job.configure();
        AdapterThreshold threshold = jacocoAdapter.createThresholdsPageArea(AdapterThresholdTarget.FILE,
                80,
                90, false);
        job.save();
        Build fifthBuildUnstable = buildUnstable(job);

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

        MainPanel mainPanel = new MainPanel(job);
        //String mainPanelCoverageTrendChart = mainPanel.getCoverageTrendChart();
        MainPanelTest.verifyTrendChartWithTwoReports(mainPanel);


        /**
         * 6th build: change threshold, setFailUnhealthy(true) so that build should fail.
         * Check if build failed.
         */
        job.configure();
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        threshold.setThresholdTarget(AdapterThresholdTarget.CLASS);
        threshold.setUnhealthyThreshold(99);
        threshold.setUnstableThreshold(5);
        threshold.setFailUnhealthy(true);
        job.save();
        Build sixthBuildFailing = buildWithErrors(job);

        /**
         * 7th build: Remove thresholds. Set GlobalThresholds, but so that build should still succeed.
         * Check if build is successful.
         */
        job.configure();
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        threshold.delete();
        GlobalThreshold globalThreshold = coveragePublisher.createGlobalThresholdsPageArea(GlobalThresholdTarget.FILE,
                59, 59, false);
        job.save();
        Build seventhBuildSuccessfully = buildSuccessfully(job);

        /**
         * 8th build: Change GlobalThresholds, setFailUnhealthy(false), should still succeed.
         * Check if build is successful.
         * //TODO: ggf. rauswerfen, macht der sinn?
         */
        job.configure();

        coveragePublisher.ensureAdvancedOptionsIsActivated();
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.FILE);
        globalThreshold.setUnhealthyThreshold(60);
        globalThreshold.setUnstableThreshold(60);
        globalThreshold.setFailUnhealthy(false);
        job.save();
        Build eighthBuildSuccessfully = buildSuccessfully(job);

        /**
         * 9th build  (preperation for 10th build): Change GlobalThresholds, set..
         * //TODO: build configuration ueberarbeiten (sinnvolle confi?)
         */
        job.configure();
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.CLASS);
        globalThreshold.setUnhealthyThreshold(99);
        globalThreshold.setUnstableThreshold(5);
        globalThreshold.setFailUnhealthy(true);
        job.save();
        Build ninthBuildFailing = buildWithErrors(job);

        /**
         * 10th build: Set globalThresholds and failUnhealthy(true) so that build should fail.
         * Check if build failed.
         * //TODO: build configuration ueberarbeiten (sinnvolle confi?)
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setFailUnhealthy(true);
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.CLASS);
        globalThreshold.setUnhealthyThreshold(99);
        globalThreshold.setUnstableThreshold(5);
        job.save();
        Build tenthBuildFailing = buildWithErrors(job);

        /**
         * 11th build: Set global Thresholds so that build is unstable.
         * Check if build is unstable.
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        globalThreshold.setThresholdTarget(GlobalThresholdTarget.CLASS);
        globalThreshold.setUnhealthyThreshold(0);
        globalThreshold.setUnstableThreshold(95);
        job.save();
        Build eleventhBuildFailing = buildUnstable(job);

        /**
         * 12th build: Set fail if build would be unstable.
         * Check if build fails due to setFailUnstable(true) and same configuration as build before which was unstable.
         * */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setFailUnstable(true);
        job.save();
        Build twelfthBuildFailing = buildWithErrors(job);


        /**
         * 13th build: Set SourceFileResolver to {#SourceFileResolver.STORE_ALL_BUILD}
         * Check ..
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setFailUnstable(false);
        globalThreshold.delete();

        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSourceFileResolver(SourceFileResolver.STORE_ALL_BUILD);
        job.save();
        Build thirteenthBuildFailing = buildSuccessfully(job);

        /**
         * 14th build: Set SourceFileResolver to {#SourceFileResolver.STORE_ALL_BUILD}
         * Check ..
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSourceFileResolver(SourceFileResolver.STORE_LAST_BUIlD);
        job.save();
        Build fourteenthBuildFailing = buildSuccessfully(job);

        /**
         * 15th build: Set SourceFileResolver to {#SourceFileResolver.NEVER_STORE}
         * Check ....
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSourceFileResolver(SourceFileResolver.NEVER_STORE);
        job.save();
        Build fifteenthBuildFailing = buildSuccessfully(job);

        /**
         * 16th build: should skip publishing checks
         */
        job.configure();
        coveragePublisher.ensureAdvancedOptionsIsActivated();
        coveragePublisher.setSkipPublishingChecks(true);
        job.save();
        Build sixteenthBuildFailing = buildSuccessfully(job);

        /* some old notes:
         * 5) normale thresholds ohne fail setter
         * 6) normae thresholds mit fail on ...
         * 7) global thresholds ohne setter
         * 8) globale mit set fail unhealthy
         * 9) globale mit set fail unstable
         * 10/11/12) oder nur 10) set storing level
         * 13) publishing checks :(
         */

    }



}

