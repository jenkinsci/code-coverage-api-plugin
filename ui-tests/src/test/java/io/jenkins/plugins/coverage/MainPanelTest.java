package io.jenkins.plugins.coverage;

import org.junit.Test;

import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.util.TrendChartTestUtil;

import static org.assertj.core.api.Assertions.*;

/**
 * Acceptance tests for MainPanel of a project.
 * Contains static test-methods which can also be used other classes, especially used {@link SmokeTests}.
 */
public class MainPanelTest extends UiTest {

    /**
     * Test for MainPanel of job with some builds with reports.
     * Verifies TrendChart in MainPanel is displayed and has correct values.
     */
    @Test
    public void testTrendChartAfterSomeBuildsWithReports() {
        FreeStyleJob job = getJobWithFirstBuildAndDifferentReports(InCaseCoverageDecreasedConfiguration.DONT_FAIL);
        buildSuccessfully(job);
        MainPanel mainPanel = new MainPanel(job);
        MainPanelTest.verifyTrendChartWithTwoReports(mainPanel, 1, 2);
    }

    /**
     * Test for MainPanel of job with only one build containing report.
     * Verifies no TrendChart is displayed, due to TrendChart is not displayed in MainPanel if job has less than two
     * builds with reports.
     */
    @Test
    public void testTrendChartShouldNotBeDisplayed() {
        FreeStyleJob job = getJobWithReportInConfiguration();
        MainPanel mainPanel = new MainPanel(job);
        mainPanel.open();
        assertThat(mainPanel.isChartDisplayed()).isFalse();
    }

    /**
     * Verifies a specific displayed TrendChart.
     *
     * @param mainPanel
     *         of project
     * @param firstBuildInChartNumber
     *         of build visualized in TrendChart
     * @param lastBuildInChartNumber
     *         of build visualized in TrendChart
     */
    public static void verifyTrendChartWithTwoReports(final MainPanel mainPanel, final int firstBuildInChartNumber,
            final int lastBuildInChartNumber) {
        mainPanel.open();
        assertThat(mainPanel.isChartDisplayed()).isTrue();
        String trendChart = mainPanel.getCoverageTrendChart();
        TrendChartTestUtil.verifyTrendChart(trendChart, firstBuildInChartNumber, lastBuildInChartNumber);
    }

}

