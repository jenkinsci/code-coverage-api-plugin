package io.jenkins.plugins.coverage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoverageSummary;
import io.jenkins.plugins.coverage.UiTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Acceptance tests for Summary. Verifies if  ...//TODO: javadoc
 */
public class SummaryTest extends UiTest {

    /**
     * Verifies if summary is not visible if build with no report is enabled.
     *
     * @param build
     *         Build of Project
     */
    public static void verifySummaryOnNoReport(final Build build) {
        build.open();
        // TODO: Das Element existiert nicht, wie soll das getestet werden?
        CoverageSummary cs = new CoverageSummary(build, "coverage");
    }

    /**
     * Verifies if summary of first successful build of Project is correct.
     *
     * @param build
     *         Build of Project
     * @param expectedCoverage
     *         map of expected values to be present in summary
     */
    public static void verifySummaryOnSuccessfulBuild(final Build build,
            final HashMap<String, Double> expectedCoverage) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        HashMap<String, Double> coverage = cs.getCoverage();

        assertThat(coverage).isEqualTo(expectedCoverage);
    }

    /**
     * Verifies if summary of second successful build of Project is correct and has reference.
     *
     * @param build
     *         Build of Project
     * @param expectedCoverage
     *         HashMap of expected values for coverage
     * @param expectedChanges
     *         List of expected values for coverage changes
     */
    public static void verifySummaryWithReferenceBuild(final Build build,
            final HashMap<String, Double> expectedCoverage, final List<Double> expectedChanges) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");

        HashMap<String, Double> coverage = cs.getCoverage();
        List<Double> changes = cs.getCoverageChanges();

        assertThat(coverage).isEqualTo(expectedCoverage);
        assertThat(changes).isEqualTo(expectedChanges);

        String referenceBuild = cs.getReferenceBuild();

        assertThat(referenceBuild).contains("/" + (build.getNumber() - 1) + "/");
    }

    /**
     * Verifies if summary of failed build of Project is correct.
     *
     * @param build
     *         Build of Project
     * @param expectedCoverage
     *         expected coverage of build
     */
    public static void verifySummaryOnFailedBuild(final Build build, final HashMap<String, Double> expectedCoverage) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        HashMap<String, Double> coverage = cs.getCoverage();

        assertThat(coverage).isEqualTo(expectedCoverage);

    }

    /**
     * Verifies fail message on summary.
     *
     * @param build
     *         current build of project
     * @param unhealthyThreshold
     *         of project
     * @param unstableThreshold
     *         of project
     */
    public static void verifyFailMessage(final Build build, final float unhealthyThreshold,
            final float unstableThreshold) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        String failMsg = cs.getFailMsg();
        assertThat(failMsg).contains("unstableThreshold=" + unstableThreshold)
                .contains("unhealthyThreshold=" + unhealthyThreshold);
    }

    /**
     * Tests if coverage is correct if build is successful.
     */
    @Test
    public void testSuccessfulBuild() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build build = buildSuccessfully(job);

        HashMap<String, Double> expectedCoverage = new HashMap<>();
        expectedCoverage.put("Line", 95.52);
        expectedCoverage.put("Branch", 88.59);

        verifySummaryOnSuccessfulBuild(build, expectedCoverage);
    }

    /**
     * Test if coverage reference is correct if both builds are successful.
     */
    @Test
    public void testReferenceBuild() {
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
        Build build = buildSuccessfully(job);

        HashMap<String, Double> expectedCoverage = new HashMap<>();
        expectedCoverage.put("Line", 91.02);
        expectedCoverage.put("Branch", 93.97);
        List<Double> expectedReferenceCoverage = new LinkedList<>();
        expectedReferenceCoverage.add(-0.045);
        expectedReferenceCoverage.add(0.054);

        verifySummaryWithReferenceBuild(build, expectedCoverage, expectedReferenceCoverage);
    }
}
