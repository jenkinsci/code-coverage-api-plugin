import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.GlobalThresholdTarget;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.FileCoverageTable;
import io.jenkins.plugins.coverage.MainPanel;

/**
 * Should in the end contain all tests.
 */
public class SmokeTests extends UiTest {

    /**
     * Test for verifying CoveragePlugin by checking its behaviour in different sitauations, using a project with two
     * different jacoco files.
     * <p>
     * Different scenarios are used in each build, see javadoc.
     * <p>
     * Verifies correct build-status depending on its configuration like used thresholds, global thresholds, fail on no
     * report, etc. Verifies CoverageSummary. Verifies MainPanel (CoverageTrend). Verifies CoverageReport
     * (CoverageTrend, CoverageOverview, FileCoverageTable and CoverageTree and its pages).
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

        HashMap<String, Double> expectedCoverageOnFirstSuccessfulBuild = new HashMap<>();
        expectedCoverageOnFirstSuccessfulBuild.put("Line", 95.52);
        expectedCoverageOnFirstSuccessfulBuild.put("Branch", 88.59);
        SummaryTest.verifySummaryOnSuccessfulBuild(firstSuccessfulBuild, expectedCoverageOnFirstSuccessfulBuild);

        CoverageReport reportOfFirstSuccessfulBuild = new CoverageReport(firstSuccessfulBuild);
        reportOfFirstSuccessfulBuild.open();

        FileCoverageTable fileCoverageTableOnFirstSuccessfulBuild = reportOfFirstSuccessfulBuild.openFileCoverageTable();
        CoverageReportTest.verifyFileCoverageTableNumberOfMaxEntries(fileCoverageTableOnFirstSuccessfulBuild, 307);
        CoverageReportTest.verifyFileCoverageTableContent(fileCoverageTableOnFirstSuccessfulBuild,
                new String[] {"edu.hm.hafner.analysis.parser.dry", "edu.hm.hafner.analysis", "edu.hm.hafner.analysis.parser.violations"},
                new String[] {"AbstractDryParser.java", "AbstractPackageDetector.java", "AbstractViolationAdapter.java"},
                new String[] {"85.71%", "88.24%", "91.67%"},
                new String[] {"83.33%", "50.00%", "100.00%"});
        fileCoverageTableOnFirstSuccessfulBuild.openTablePage(2);
        CoverageReportTest.verifyFileCoverageTableContent(fileCoverageTableOnFirstSuccessfulBuild,
                new String[] {"edu.hm.hafner.analysis.registry", "edu.hm.hafner.analysis.parser", "edu.hm.hafner.analysis.parser"},
                new String[] {"AnsibleLintDescriptor.java", "AnsibleLintParser.java", "AntJavacParser.java"},
                new String[] {"100.00%", "100.00%", "100.00%"},
                new String[] {"n/a", "n/a", "100.00%"});
        fileCoverageTableOnFirstSuccessfulBuild.openTablePage(3);
        CoverageReportTest.verifyFileCoverageTableContent(fileCoverageTableOnFirstSuccessfulBuild,
                new String[] {"edu.hm.hafner.analysis.parser", "edu.hm.hafner.analysis.registry", "edu.hm.hafner.analysis.parser"},
                new String[] {"BuckminsterParser.java", "CadenceIncisiveDescriptor.java", "CadenceIncisiveParser.java"},
                new String[] {"100.00%", "100.00%", "86.49%"},
                new String[] {"100.00%", "n/a", "66.67%"});

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
        HashMap<String, Double> expectedCoverageThirdBuildFailed = new HashMap<>();
        expectedCoverageThirdBuildFailed.put("Report", 100.00);
        expectedCoverageThirdBuildFailed.put("Group", 100.00);
        expectedCoverageThirdBuildFailed.put("Package", 100.00);
        expectedCoverageThirdBuildFailed.put("File", 70.00);
        expectedCoverageThirdBuildFailed.put("Class", 83.00);
        expectedCoverageThirdBuildFailed.put("Method", 95.00);
        expectedCoverageThirdBuildFailed.put("Instruction", 93.00);
        expectedCoverageThirdBuildFailed.put("Line", 91.00);
        expectedCoverageThirdBuildFailed.put("Conditional", 94.00);
        SummaryTest.verifySummaryOnFailedBuild(thirdBuildFailed, expectedCoverageThirdBuildFailed);

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

        HashMap<String, Double> expectedCoverageOnFourthBuild = new HashMap<>();
        expectedCoverageOnFourthBuild.put("Line", 91.02);
        expectedCoverageOnFourthBuild.put("Branch", 93.97);
        SummaryTest.verifySummaryOnSuccessfulBuild(fourthBuildSuccessful, expectedCoverageOnFourthBuild);

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
        HashMap<String, Double> expectedCoverageFifthBuild = new HashMap<>();
        expectedCoverageFifthBuild.put("Line", 91.02);
        expectedCoverageFifthBuild.put("Branch", 93.97);
        List<Double> expectedReferenceCoverageFifthBuild = new LinkedList<>();
        expectedReferenceCoverageFifthBuild.add(0.00);
        expectedReferenceCoverageFifthBuild.add(0.00);

        SummaryTest.verifySummaryWithReferenceBuild(fifthBuildUnstable, expectedCoverageFifthBuild,
                expectedReferenceCoverageFifthBuild);

        CoverageReport report = new CoverageReport(fifthBuildUnstable);
        report.open();

        FileCoverageTable fileCoverageTable = report.openFileCoverageTable();
        CoverageReportTest.verifyFileCoverageTableContent(fileCoverageTable,
                new String[] {"edu.hm.hafner.util", "edu.hm.hafner.util", "edu.hm.hafner.util"},
                new String[] {"Ensure.java", "FilteredLog.java", "Generated.java"},
                new String[] {"80.00%", "100.00%", "n/a"},
                new String[] {"86.96%", "100.00%", "n/a"});
        CoverageReportTest.verifyFileCoverageTableNumberOfMaxEntries(fileCoverageTable, 10);

        String coverageTree = report.getCoverageTree();
        CoverageReportTest.verifyCoverageTree(coverageTree);

        String coverageOverview = report.getCoverageOverview();
        CoverageReportTest.verifyCoverageOverview(coverageOverview);

        String trendChart = report.getCoverageTrend();
        TrendChartTestUtil.verifyTrendChart(trendChart, 2, 5);

        MainPanel mainPanel = new MainPanel(job);
        MainPanelTest.verifyTrendChartWithTwoReports(mainPanel, 2, 5);

        /**
         * 6th build: change threshold, setFailUnhealthy(true) so that build should fail.
         * Check if build failed.
         */
        job.configure();
        int unhealthyThresholdForSixthBuild = 99;
        int unstableThresholdForSixthBuild = 5;
        jacocoAdapter.ensureAdvancedOptionsIsActivated();
        threshold.setThresholdTarget(AdapterThresholdTarget.CLASS);
        threshold.setUnhealthyThreshold(99);
        threshold.setUnstableThreshold(5);
        threshold.setFailUnhealthy(true);
        job.save();
        Build sixthBuildFailing = buildWithErrors(job);
        SummaryTest.verifyFailMessage(sixthBuildFailing, unhealthyThresholdForSixthBuild,
                unstableThresholdForSixthBuild);

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

