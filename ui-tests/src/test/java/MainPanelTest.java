import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;

import io.jenkins.plugins.coverage.MainPanel;


public class MainPanelTest extends AbstractJUnitTest {

    public static void verifyTrendChartWithTwoReports(MainPanel mainPanel) {
        mainPanel.open();
        String trendChart = mainPanel.getCoverageTrendChart();
        TrendChartTest.verifyTrendChart(trendChart);
    }



}

