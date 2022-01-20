package io.jenkins.plugins.coverage;

import org.junit.Test;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.GlobalThresholdTarget;
import io.jenkins.plugins.coverage.util.ChartUtil;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;

//TODO: ueberdenken ob tests so sinn machen & ausreichen

/**
 * Acceptance tests for CoveragePublisher. Verifies if set options in CoveragePublisher are used and lead to excepted
 * results.
 */
public class CoveragePublisherTest extends UiTest {
    private static final String USERNAME = "gitplugin";

    @Test
    public void testApplyThresholdRecursively() {
        //TODO
    }

    /**
     * Verifies that job with no report fails when setFailNoReports(true).
     */
    @Test
    public void testFailOnNoReport() {
        FreeStyleJob job = getJobWithoutAnyReports(InCaseNoReportsConfiguration.FAIL);
        buildWithErrors(job);
    }

    /**
     * Verifies that job with decreased coverage fails when setFailBuildIfCoverageDecreasedInChangeRequest(true).
     */
    @Test
    public void testFailOnDecreasedCoverage() {
        FreeStyleJob job = getJobWithFirstBuildAndDifferentReports(InCaseCoverageDecreasedConfiguration.FAIL);
        buildWithErrors(job);
    }

    /**
     * Test if build fails if setFailUnhealthy is true and thresholds set.
     */
    @Test
    public void testAdapterThresholdsAndFailOnUnhealthySetter() {
        FreeStyleJob job = getJobWithAdapterThresholdAndFailOnUnhealthySetter(97, 99, true, ThresholdLevel.ADAPTER);
        buildWithErrors(job);
    }

    /**
     * Test if global thresholds are set.
     */
    @Test
    public void testGlobalThresholdsAndFailSetter() {
        FreeStyleJob job = getJobWithAdapterThresholdAndFailOnUnhealthySetter(97, 99, true, ThresholdLevel.GLOBAL);
        buildUnstable(job);
    }

    /**
     * Tests if source file storing level and display is correct.
     */
    @Test
    public void testSourceFileStoringLevel() {
        String repoUrl = "https://github.com/jenkinsci/code-coverage-api-plugin.git";
        String commitID = "f52a51691598600e2f42eee354ee6a540e008a72";
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setSkipPublishingChecks(false);
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_OLD_COMMIT_XML);
        coveragePublisher.setSourceFileResolver(SourceFileResolver.STORE_ALL_BUILD);
        //job.save();
        Build build = buildSuccessfully(job);
        CoverageReport report = new CoverageReport(build);
        report.open();
        FileCoverageTable fileCoverageTable = report.openFileCoverageTable();
        FileCoverageTableRow fileCoverageTableRow = fileCoverageTable.getRow(1);

        assertThat(fileCoverageTableRow.hasFileLink()).isTrue();

        SourceCodeView sourceCodeView = fileCoverageTableRow.openFileLink(build);
        String json = ChartUtil.getChartsDataById(sourceCodeView, "coverage-overview");

        assertThatJson(json)
                .inPath("$.yAxis[0].data[*]")
                .isArray()
                .hasSize(5)
                .contains("Class", "Method", "Line", "Instruction", "Branch");

        assertThatJson(json)
                .inPath("$.series[0].data")
                .isArray()
                .hasSize(5)
                .contains(1, 0.6, 0.5833333333333334, 0.25);

        assertThatJson(json).node("series[0].name").isEqualTo("Covered");
        assertThatJson(json).node("series[1].name").isEqualTo("Missed");

        assertThat(sourceCodeView.isFileTableDisplayed()).isTrue();

    }
}
