import java.util.List;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;

import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.FileCoverageTable;
import io.jenkins.plugins.coverage.FileCoverageTable.Header;
import io.jenkins.plugins.coverage.FileCoverageTableRow;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.Assertions.*;

public class CoverageReportTest extends AbstractJUnitTest {
    //TODO: use or remove
    private static final String COLOR_GREEN = "#c4e4a9";
    private static final String COLOR_ORANGE = "#fbdea6";
    private static final String COLOR_RED = "#ef9a9a";

    /**
     * Test for checking the CoverageReport by verifying its CoverageTrend, CoverageOverview, FileCoverageTable and
     * CoverageTrend. Uses a project with two different jacoco files, each one used in another build.
     */
    public static void verify(final CoverageReport report) {
        report.open();
        FileCoverageTable fileCoverageTable = report.openFileCoverageTable();
        CoverageReportTest.verifyFileCoverageTableContent(fileCoverageTable);

        String coverageTree = report.getCoverageTree();
        CoverageReportTest.verifyCoverageTree(coverageTree);

        String coverageOverview = report.getCoverageOverview();
        CoverageReportTest.verifyCoverageOverview(coverageOverview);

        String trendChart = report.getCoverageTrend();
        TrendChartTest.verifyTrendChart(trendChart);
    }

    /**
     * Verifies content of CoverageTable of CoverageReport of Job.
     *
     * @param fileCoverageTable
     *         of current build.
     */
    public static void verifyFileCoverageTableContent(final FileCoverageTable fileCoverageTable) {
        String shouldPackage = "edu.hm.hafner.util";
        String[] shouldFiles = {"Ensure.java", "FilteredLog.java", "Generated.java"};
        String[] shouldLineCoverages = {"80.00%", "100.00%", "n/a"};
        String[] shouldBranchCoverages = {"86.96%", "100.00%", "n/a"};
        List<FileCoverageTableRow> rows = fileCoverageTable.getTableRows();
        List<String> headers = fileCoverageTable.getHeaders();

        assertThat(headers)
                .hasSize(6)
                .contains(Header.PACKAGE.getTitle(), Header.FILE.getTitle(), Header.LINE_COVERAGE.getTitle(),
                        Header.BRANCH_COVERAGE.getTitle());

        for (int i = 0; i < rows.size(); i++) {
            FileCoverageTableRow row = rows.get(i);
            assertThat(row.getPackage()).isEqualTo(shouldPackage);
            assertThat(row.getFile()).isEqualTo(shouldFiles[i]);
            assertThat(row.getLineCoverage()).isEqualTo(shouldLineCoverages[i]);
            assertThat(row.getBranchCoverage()).isEqualTo(shouldBranchCoverages[i]);
        }
    }

    /**
     * Verifies CoverageOverview of CoverageReport of Job with two Builds.
     *
     * @param coverageOverview
     *         from second build.
     */
    public static void verifyCoverageOverview(final String coverageOverview) {
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
     * Verifies CoverageTree of CoverageReport of Job with two Builds.
     *
     * @param coverageTree
     *         from second build.
     */
    public static void verifyCoverageTree(final String coverageTree) {
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
    }

}

