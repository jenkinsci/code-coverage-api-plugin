package io.jenkins.plugins.coverage;

import org.junit.Ignore;
import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.util.ChartUtil;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;

/**
 * Acceptance tests for CoveragePublisher. Verifies if set options in CoveragePublisher are used and lead to excepted
 * results.
 */
public class CoveragePublisherTest extends UiTest {
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
    @Test @Ignore("This bug needs to be fixed")
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
    @Test @WithPlugins("git")
    public void testSourceFileStoringLevelAllBuilds() {
        FreeStyleJob job = getJobWithReportAndSourceCode(SourceFileResolver.STORE_ALL_BUILD);
        Build build = buildSuccessfully(job);
        buildSuccessfully(job);
        buildSuccessfully(job);
        buildSuccessfully(job);
        buildSuccessfully(job);
        FileCoverageTableRow fileCoverageTableRow = verifyClickableFileLink(build, true);

        SourceCodeView sourceCodeView = fileCoverageTableRow.openFileLink(build);
        String json = ChartUtil.getChartDataById(sourceCodeView, "coverage-overview");

        assertThatJson(json)
                .inPath("$.yAxis[0].data[*]")
                .isArray()
                .hasSize(4)
                .contains("Class", "Method", "Line", "Instruction");

        assertThatJson(json)
                .inPath("$.series[0].data")
                .isArray()
                .hasSize(4)
                .contains(1, 0.8461538461538461, 0.88);

        assertThatJson(json).node("series[0].name").isEqualTo("Covered");
        assertThatJson(json).node("series[1].name").isEqualTo("Missed");

        assertThat(sourceCodeView.isFileTableDisplayed()).isTrue();
    }

    /**
     * Tests if source file is only available for last build.
     */
    @Test @WithPlugins("git")
    public void testSourceFileStoringLevelLastBuild() {
        FreeStyleJob job = getJobWithReportAndSourceCode(SourceFileResolver.STORE_LAST_BUIlD);
        Build firstBuild = buildSuccessfully(job);
        Build secondBuild = buildSuccessfully(job);
        Build thirdBuild = buildSuccessfully(job);

        verifyClickableFileLink(firstBuild, false);
        verifyClickableFileLink(secondBuild, true);
        verifyClickableFileLink(thirdBuild, true);
    }

    /**
     * Tests if source file storing is off.
     */
    @Test @WithPlugins("git")
    public void testSourceFileStoringLevelNever() {
        FreeStyleJob job = getJobWithReportAndSourceCode(SourceFileResolver.NEVER_STORE);
        Build firstBuild = buildSuccessfully(job);

        verifyClickableFileLink(firstBuild, false);
    }

    /**
     * Verifies if a file in a {@link FileCoverageTableRow} is clickable.
     *
     * @param build
     *         current build with file coverage
     * @param expected
     *         value if file should be clickable
     *
     * @return row of the file
     */
    private FileCoverageTableRow verifyClickableFileLink(final Build build, final boolean expected) {
        CoverageReport report = new CoverageReport(build);
        report.open();
        FileCoverageTable fileCoverageTable = report.openFileCoverageTable();
        FileCoverageTableRow fileCoverageTableRow = fileCoverageTable.getRow(0);
        assertThat(fileCoverageTableRow.hasFileLink()).isEqualTo(expected);
        return fileCoverageTableRow;
    }
}
