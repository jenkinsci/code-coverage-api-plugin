import java.util.HashMap;
import java.util.List;

import org.jenkinsci.test.acceptance.po.Build;

import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.CoverageSummary;

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

        CoverageReport cr = cs.openReferenceBuild();

        assertThat(cr.getCurrentUrl()).contains("/" + (build.getNumber() - 1) + "/");
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
}
