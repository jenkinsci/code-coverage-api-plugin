import java.util.HashMap;
import java.util.List;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;

import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.CoverageSummary;

import static org.assertj.core.api.Assertions.*;

public class SummaryTest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    public static void NAME(final Build build) {
        build.open();
        // TODO: Das Element existiert nicht, wie soll das getestet werden?
        CoverageSummary cs = new CoverageSummary(build, "coverage");
    }

    public static void testSummaryOnFirstSuccessfulBuild(final Build build) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        HashMap<String, Double> coverage = cs.getCoverage();

        assertThat(coverage)
                .hasSize(2)
                .containsKeys("Line", "Branch")
                .containsValues(95.52, 88.59);
    }

    public static void testSummaryOnSecondSuccessfulBuild(final Build build) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");

        HashMap<String, Double> coverage = cs.getCoverage();
        List<Double> changes = cs.getCoverageChanges();

        assertThat(coverage)
                .hasSize(2)
                .containsKeys("Line", "Branch")
                .containsValues(91.02, 93.97);
        assertThat(changes).contains(-0.045, 0.054);

        CoverageReport cr = cs.openReferenceBuild();

        assertThat(cr.getCurrentUrl()).contains("/" + (build.getNumber() - 1) + "/");
    }

    public static void testSummaryOnFailedBuild(final Build build, final float unhealthyThreshold,
            final float unstableThreshold) {
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        HashMap<String, Double> coverage = cs.getCoverage();
        String failMsg = cs.getFailMsg();

        //Expecting actual:
        //  {"Class"=83.0, "Conditional"=94.0, "File"=70.0, "Group"=100.0, "Instruction"=93.0, "Line"=91.0, "Method"=95.0, "Package"=100.0, "Report"=100.0}
        //to contain values:
        //  [99.0, 97.0, 96.0, 88.0]
        assertThat(coverage).containsKeys("Report", "Group", "Package", "File", "Class", "Method", "Instruction",
                        "Line", "Conditional")
                .containsValues(100.0, 99.0, 97.0, 96.0, 95.0, 88.0);

        assertThat(failMsg).contains("unstableThreshold=" + unstableThreshold)
                .contains("unhealthyThreshold=" + unhealthyThreshold);
        System.out.println("HI");
    }

}
