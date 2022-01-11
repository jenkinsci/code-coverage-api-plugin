import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.CoverageSummary;
import io.jenkins.plugins.coverage.FileCoverageTable;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

public class CoverageReportTest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    //TODO: use or remove
    private static final String COLOR_GREEN = "#c4e4a9";
    private static final String COLOR_ORANGE = "#fbdea6";
    private static final String COLOR_RED = "#ef9a9a";


    /**
     * Test for checking the CoverageReport by verifying its CoverageTrend, CoverageOverview,
     * FileCoverageTable and CoverageTrend.
     * Uses a project with two different jacoco files, each one used in another build.
     * First build uses {@link CoverageReportTest#JACOCO_ANALYSIS_MODEL_XML},
     * Second build uses {@link CoverageReportTest#JACOCO_CODINGSTYLE_XML}.
     */
    @Test
    public void runFullCoverageReportTest() {
        FreeStyleJob job = createSuccessfulJobWithDiffererntJacocos();
        Build secondBuild = job.getLastBuild();
        CoverageReport report = new CoverageReport(secondBuild);
        report.open();
        FileCoverageTable fileCoverageTable = report.openFileCoverageTable();
        verifyFileCoverageTable(fileCoverageTable);

        String coverageTree = report.getCoverageTree();
        verifyCoverageTree(coverageTree);

        String coverageOverview = report.getCoverageOverview();
        verifyCoverageOverview(coverageOverview);

        String trendChart = report.getCoverageTrend();
        verifyTrendchart(trendChart);
    }

    /**
     * Verifies CoverageTable of CoverageReport of Job with two Builds,
     * first build with {@link CoverageReportTest#JACOCO_ANALYSIS_MODEL_XML},
     * second with  {@link CoverageReportTest#JACOCO_CODINGSTYLE_XML}.
     * @param fileCoverageTable from second build.
     */
    private void verifyFileCoverageTable(final FileCoverageTable fileCoverageTable) {
        //assertThat(fileCoverageTable).getTableRows().get(0).getCellContent("File")
    }

     /**
     * Verifies CoverageOverview of CoverageReport of Job with two Builds,
     * first build with {@link CoverageReportTest#JACOCO_ANALYSIS_MODEL_XML},
     * second with  {@link CoverageReportTest#JACOCO_CODINGSTYLE_XML}.
     * @param coverageOverview from second build.
     */
    private void verifyCoverageOverview(final String coverageOverview) {
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

    /**
     * Verifies CoverageTree of CoverageReport of Job with two Builds,
     * first build with {@link CoverageReportTest#JACOCO_ANALYSIS_MODEL_XML},
     * second with  {@link CoverageReportTest#JACOCO_CODINGSTYLE_XML}.
     * @param coverageTree from second build.
     */
    private void verifyCoverageTree(final String coverageTree) {
        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].children[*].name").isArray().hasSize(10)
                .contains("Ensure.java")
                .contains("FilteredLog.java")
                .contains("Generated.java")
                .contains("NoSuchElementException.java")
                .contains("PathUtil.java")
                .contains("PrefixLogger.java")
                .contains("StringContainsUtils.java")
                .contains("TreeString.java")
                .contains("TreeStringBuilder.java")
                .contains("VisibleForTesting.java");

        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].children[*].value").isArray().hasSize(10)
                .contains("[125, 100]")
                .contains("[34,34]")
                .contains("[0,0]")
                .contains("[2,0]")
                .contains("[43,43]")
                .contains("[12,12]")
                .contains("[8,8]")
                .contains("[46,46]")
                .contains("[53,51]")
                .contains("[0,0]");

        //TODO: pick and check items with by using its values and colors
    }

    //TODO: temporary; (also needed for coverage summary)
    /**
     * Verifies CoverageTrend of CoverageReport of Job with two Builds,
     * first build with {@link CoverageReportTest#JACOCO_ANALYSIS_MODEL_XML},
     * second with  {@link CoverageReportTest#JACOCO_CODINGSTYLE_XML}.
     * @param trendChart from second build.
     */
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

        assertThatJson(trendChart).node("series[0].name").isEqualTo("Line");
        assertThatJson(trendChart).node("series[1].name").isEqualTo("Branch");


        assertThatJson(trendChart).node("series[0].data").isArray().contains(95).contains(91);
        assertThatJson(trendChart).node("series[1].data").isArray().contains(88).contains(93);
    }


    //TODO: temporary; should later be used for a couple of tests
    /**
     *  Builds a project with two different jacoco files, each one used in another build.
     *  First build uses {@link CoverageReportTest#JACOCO_ANALYSIS_MODEL_XML},
     *  Second build uses {@link CoverageReportTest#JACOCO_CODINGSTYLE_XML}.
     * @return
     */
    private FreeStyleJob createSuccessfulJobWithDiffererntJacocos() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        JobCreatorUtils.buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        JobCreatorUtils.buildSuccessfully(job);
        return job;
    }
}

