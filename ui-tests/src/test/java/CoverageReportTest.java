import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.CoverageSummary;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

public class CoverageReportTest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    @Test
    public void createJobForPreparingFirstTestsOfCoverageReport() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        Build build2 = JobCreatorUtils.buildSuccessfully(job);
        build2.open();
        CoverageSummary summary = new CoverageSummary(build, "coverage");
        CoverageReport report = summary.openCoverageReport();

        String coverageOverview = report.getCoverageOverview();

        assertThatJson(coverageOverview)
                .inPath("$.yAxis[0].data[*]")
                .isArray()
                .hasSize(7)
                .contains("Branch")
                .contains("Instruction")
                .contains("Line")
                .contains("Method")
                .contains("Class")
                .contains("File")
                .contains("Package");

        assertThatJson(coverageOverview).inPath("series[0].data").isArray().hasSize(7)
                .contains("0.7")
                .contains("1")
                .contains("0.8333333333333334")
                .contains("0.9509803921568627")
                .contains("0.9102167182662538")
                .contains("0.9333333333333333")
                .contains("0.9396551724137931");

        assertThatJson(coverageOverview).node("series[0].name").isEqualTo("Covered");
        assertThatJson(coverageOverview).node("series[1].name").isEqualTo("Missed");

    }


    //implemented twice -> TODO: refactoring
    public void verifyTrendchart(final String trendChart) {
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

}

