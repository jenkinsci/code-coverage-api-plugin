import org.junit.Test;

import io.jenkins.plugins.coverage.MainPanel;
import static org.assertj.core.api.Assertions.*;

/**
 * UI-Tests for MainPanel of a project.
 */
public class MainPanelTest extends UiTest {

    @Test
    public void verifyTrendChart(){

        assertThat("").isEqualTo("");

    }

    @Test
    public void verfiyNoTrendChartIsGenerated(){

        assertThat("").isEqualTo("");

    }

    /**
     * Verifies displayed Trendchart.
     *
     * @param mainPanel
     *         of project
     */
    public static void verifyTrendChartWithTwoReports(final MainPanel mainPanel, int firstBuildInChartNumber, int lastBuildInChartNumber) {
        mainPanel.open();
        String trendChart = mainPanel.getCoverageTrendChart();
        TrendChartUtil.verifyTrendChart(trendChart, firstBuildInChartNumber, lastBuildInChartNumber);
    }


}

