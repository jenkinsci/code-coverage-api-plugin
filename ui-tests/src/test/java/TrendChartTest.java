import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.MainPanel;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.Assertions.*;

public class TrendChartTest  extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";

    /**
     * Check if the generated TrendChart has the right axis and the right values for its builds.
     */
    @Test
    public void verifyGeneratedTrendChart() {
        String trendChart = getTrendChartFromJobWithMultipleBuilds();

        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .hasSize(2)
                .contains("#1")
                .contains("#2");


        assertThatJson(trendChart)
                .node("series")
                .isArray()
                .hasSize(2);

        assertThatJson(trendChart)
                .and(
                        a -> a.node("series[0].name").isEqualTo("Line"),
                        a -> a.node("series[1].name").isEqualTo("Branch")
                );

        assertThatJson(trendChart)
                .and(
                        a -> a.node("series[0].data").isArray().contains(95).contains(91),
                        a -> a.node("series[1].data").isArray().contains(88).contains(93)
                );
    }


    /**
     * Trend-charts are available for a project with two or more builds.
     * A Project with only one build should not generate a Trend-chart.
     */
    @Test
    public void verifyNoTrendChartIsGenerated(){
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage/jacoco-analysis-model.xml");
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        job.open();
        MainPanel mp = new MainPanel(build, "");
        assertThat(mp.trendChartIsDisplayed()).isFalse();
    }



    private String getTrendChartFromJobWithMultipleBuilds() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage");
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        Build build2 = JobCreatorUtils.buildSuccessfully(job);
        job.open();
        MainPanel mp = new MainPanel(build2, "");
        return mp.getTrendChart();
    }





}
