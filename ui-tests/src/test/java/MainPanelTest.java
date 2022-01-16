import org.junit.Test;

import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.MainPanel;

import static org.assertj.core.api.Assertions.*;

/**
 * UI-Tests for MainPanel of a project.
 */
public class MainPanelTest extends UiTest {

    /**
     * Verifies TrendChart in MainPanel is displayed and has correct values.
     */
    @Test
    public void verifyTrendChartAfterSomeBuildsWithReports() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        buildSuccessfully(job);

        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        buildSuccessfully(job);

        MainPanel mainPanel = new MainPanel(job);

        MainPanelTest.verifyTrendChartWithTwoReports(mainPanel, 1, 2);
    }

    /**
     * Verifies no trendchart is displayed, due to trendchart is not displayed in MainPanel if job has less than two
     * builds with reports.
     */
    @Test
    public void verifyTrendChartIsNotDisplayedAfterOneBuildContainingReport() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        buildSuccessfully(job);
        MainPanel mainPanel = new MainPanel(job);
        mainPanel.open();
        assertThat(mainPanel.isChartDisplayed()).isFalse();
    }

    /**
     * Verifies displayed TrendChart.
     *
     * @param mainPanel
     *         of project
     */
    public static void verifyTrendChartWithTwoReports(final MainPanel mainPanel, final int firstBuildInChartNumber,
            final int lastBuildInChartNumber) {
        mainPanel.open();
        assertThat(mainPanel.isChartDisplayed()).isTrue();
        String trendChart = mainPanel.getCoverageTrendChart();
        TrendChartTestUtil.verifyTrendChart(trendChart, firstBuildInChartNumber, lastBuildInChartNumber);
    }

}

