package io.jenkins.plugins.coverage;

import java.util.List;

import org.junit.Test;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.FileCoverageTable.Header;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Acceptance tests for CoverageReport, containing three charts and one table ({@link FileCoverageTable}). Contains
 * static test-methods which can also be used other classes, especially used {@link SmokeTests}.
 */
public class CoverageReportTest extends UiTest {

    /**
     * Test for CoverageReport of job with no reports does not exist. Verifies CoverageReport can't be opened.
     */
    @Test
    public void testCoverageReportNotAvailableForJobWithNoReports() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        job.save();
        Build build = buildSuccessfully(job);
        CoverageReport report = new CoverageReport(build);
        report.open();
        assertThat(driver.getCurrentUrl()).isNotEqualTo(report.url.toString());
    }

    /**
     * Test for CoverageReport after some builds with reports. Verifies some data of charts (TrendChart, CoverageTree
     * and CoverageOverview) as well as {@link FileCoverageTable}.
     */
    @Test
    public void testCoverageReportAfterSomeBuildsWithReports() {
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
        Build secondBuild = buildSuccessfully(job);

        CoverageReport report = new CoverageReport(secondBuild);
        report.open();

        FileCoverageTable coverageTable = report.getCoverageTable();
        CoverageReportTest.verifyFileCoverageTableContent(coverageTable,
                new String[] {"edu.hm.hafner.util", "edu.hm.hafner.util", "edu.hm.hafner.util"},
                new String[] {"Ensure.java", "FilteredLog.java", "Generated.java"},
                new String[] {"80.00%", "100.00%", "n/a"},
                new String[] {"86.96%", "100.00%", "n/a"});

        String coverageTree = report.getCoverageTree();
        CoverageReportTest.verifyCoverageTreeAfterSomeBuildsWithReports(coverageTree);

        String coverageOverview = report.getCoverageOverview();
        CoverageReportTest.verifyCoverageOverviewAfterSomeBuildsWithReports(coverageOverview);
    }

    /**
     * Test for CoverageReport after first build with a report. Verifies charts (TrendChart, CoverageTree and
     * CoverageOverview) as well as {@link FileCoverageTable} are being displayed and verifies some of its data.
     */
    @Test
    public void testCoverageReportAfterOneBuildWithReport() {

        Build secondBuild = buildSuccessfully(getJobWithReportInConfiguration());

        CoverageReport report = new CoverageReport(secondBuild);
        report.open();

        FileCoverageTable coverageTable = report.getCoverageTable();
        CoverageReportTest.verifyFileCoverageTableNumberOfMaxEntries(coverageTable, 307);

        String coverageTree = report.getCoverageTree();
        CoverageReportTest.verifyCoverageTreeAfterOneBuildWithReport(coverageTree);

        String coverageOverview = report.getCoverageOverview();
        CoverageReportTest.verifyCoverageOverviewAfterOneBuildWithReport(coverageOverview);

    }

    /**
     * Test for CoverageTable which should contain multiple pages. Verfies {@link FileCoverageTable} with different
     * pages works correctly, by checking some of its {@link FileCoverageTableRow}s on different pages.
     */
    @Test
    public void testCoverageTableWithMultiplePages() {

        Build secondBuild = buildSuccessfully(getJobWithReportInConfiguration());

        CoverageReport report = new CoverageReport(secondBuild);
        report.open();

        FileCoverageTable table = report.openFileCoverageTable();
        CoverageReportTest.verifyFileCoverageTableContent(table,
                new String[] {"edu.hm.hafner.analysis", "edu.hm.hafner.analysis", "edu.hm.hafner.analysis"},
                new String[] {"AbstractPackageDetector.java", "CSharpNamespaceDetector.java", "Categories.java"},
                new String[] {"88.24%", "100.00%", "100.00%"},
                new String[] {"50.00%", "n/a", "100.00%"});
        table.openTablePage(2);
        CoverageReportTest.verifyFileCoverageTableContent(table,
                new String[] {"edu.hm.hafner.analysis", "edu.hm.hafner.analysis", "edu.hm.hafner.analysis"},
                new String[] {"IssueBuilder.java", "IssueDifference.java", "IssueParser.java"},
                new String[] {"100.00%", "100.00%", "83.33%"},
                new String[] {"100.00%", "92.86%", "n/a"});
        table.openTablePage(3);
        CoverageReportTest.verifyFileCoverageTableContent(table,
                new String[] {"edu.hm.hafner.analysis", "edu.hm.hafner.analysis", "edu.hm.hafner.analysis"},
                new String[] {"PackageDetectors.java", "PackageNameResolver.java", "ParsingCanceledException.java"},
                new String[] {"92.31%", "100.00%", "0.00%"},
                new String[] {"100.00%", "83.33%", "n/a"});
    }

    /**
     * Verifies content of CoverageTable of CoverageReport of Job. Arrays must have the same length.
     *
     * @param fileCoverageTable
     *         of current build
     * @param shouldPackage
     *         string array of expected values in package column
     * @param shouldFiles
     *         string array of expected values in files column
     * @param shouldLineCoverages
     *         string array of expected values in line coverage column
     * @param shouldBranchCoverages
     *         string array of expected values in branch coverage column
     */
    public static void verifyFileCoverageTableContent(final FileCoverageTable fileCoverageTable,
            final String[] shouldPackage, final String[] shouldFiles,
            final String[] shouldLineCoverages, final String[] shouldBranchCoverages) {
        List<FileCoverageTableRow> rows = fileCoverageTable.getTableRows();
        List<String> headers = fileCoverageTable.getHeaders();

        assertThat(headers)
                .hasSize(6)
                .contains(Header.PACKAGE.getTitle(), Header.FILE.getTitle(),
                        Header.LINE_COVERAGE.getTitle(), Header.LINE_COVERAGE_DELTA.getTitle(),
                        Header.BRANCH_COVERAGE.getTitle(), Header.BRANCH_COVERAGE_DELTA.getTitle());

        for (int i = 0; i < shouldFiles.length; i++) {
            FileCoverageTableRow row = rows.get(i);
            assertThat(row.getPackage()).isEqualTo(shouldPackage[i]);
            assertThat(row.getFile()).isEqualTo(shouldFiles[i]);
            assertThat(row.getLineCoverage()).isEqualTo(shouldLineCoverages[i]);
            assertThat(row.getBranchCoverage()).isEqualTo(shouldBranchCoverages[i]);
        }
    }

    /**
     * Verifies number of maximal entries in {@link FileCoverageTable}.
     *
     * @param fileCoverageTable
     *         of build
     * @param shouldValue
     *         number of expected maximal entries
     */
    public static void verifyFileCoverageTableNumberOfMaxEntries(final FileCoverageTable fileCoverageTable,
            final int shouldValue) {
        assertThat(fileCoverageTable.getTotals()).isEqualTo(shouldValue);

    }

    /**
     * Verifies CoverageOverview of CoverageReport of Job with two Builds.
     *
     * @param coverageOverview
     *         from second build.
     */
    public static void verifyCoverageOverviewAfterSomeBuildsWithReports(final String coverageOverview) {
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
     * Verifies CoverageOverview of CoverageReport of Job after one build.
     *
     * @param coverageOverview
     *         from first build.
     */
    public static void verifyCoverageOverviewAfterOneBuildWithReport(final String coverageOverview) {
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
                .contains("1")
                .contains("0.996742671009772")
                .contains("0.9856733524355301")
                .contains("0.9740400216333153")
                .contains("0.9552449748743719")
                .contains("0.9620776748782899")
                .contains("0.8858666666666667");

        assertThatJson(coverageOverview).node("series[0].name").isEqualTo("Covered");
        assertThatJson(coverageOverview).node("series[1].name").isEqualTo("Missed");
    }

    /**
     * Verifies CoverageTree of CoverageReport of Job with two Builds.
     *
     * @param coverageTree
     *         from second build.
     */
    public static void verifyCoverageTreeAfterSomeBuildsWithReports(final String coverageTree) {
        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].name")
                .isArray()
                .hasSize(1)
                .contains("edu.hm.hafner.util");
        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].value").isArray().hasSize(1)
                .contains("[323, 294]");

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

    /**
     * Verifies CoverageTree of CoverageReport of Job after one build.
     *
     * @param coverageTree
     *         from second build.
     */
    public static void verifyCoverageTreeAfterOneBuildWithReport(final String coverageTree) {
        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].name")
                .isArray()
                .hasSize(1)
                .contains("edu.hm.hafner");
        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].value").isArray().hasSize(1)
                .contains("[6368, 6083]");

        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].children[*].name").isArray().hasSize(2)
                .contains("analysis")
                .contains("util");
        assertThatJson(coverageTree).inPath("series[*].data[*].children[*].children[*].value").isArray().hasSize(2)
                .contains("[6306, 6023]")
                .contains("[62, 60]");
    }

}

