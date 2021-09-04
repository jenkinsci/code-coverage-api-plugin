package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.ResourceTest;

import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.JavaCoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

import static org.assertj.core.api.Assertions.*;

/**
 * TODO: Move to corresponding test class.
 *
 * @author Ullrich Hafner
 */
class CoverageResultTest extends ResourceTest {
    private static final String EXPECTED_PACKAGE_NAME = "edu.hm.hafner.util";
    private static final String TREE_STRING_BUILDER = "TreeStringBuilder.java";

    private static final CoverageElement LINE = CoverageElement.LINE;
    private static final CoverageElement INSTRUCTION = JacocoReportAdapterDescriptor.INSTRUCTION;
    private static final CoverageElement CONDITIONAL = CoverageElement.CONDITIONAL;
    private static final CoverageElement PACKAGE = JavaCoverageReportAdapterDescriptor.PACKAGE;

    @Test
    void shouldReadJaCoCoResult() throws CoverageException {
        JacocoReportAdapter parser = new JacocoReportAdapter("Hello");
        CoverageElementRegister.addCoverageElements(new JacocoReportAdapterDescriptor().getCoverageElements());
        CoverageResult report = parser.getResult(getResourceAsFile("jacoco.xml").toFile());

        assertThat(report.getElement()).isEqualTo(CoverageElement.REPORT);
        assertThat(report.hasSingletonChild()).isTrue();
        assertThat(report.getChildren()).hasSize(1).containsExactly("project");

        CoverageResult project = report.getSingletonChild();
        assertThat(project.hasSingletonChild()).isTrue();
        assertThat(project.getChildren()).hasSize(1).containsExactly(EXPECTED_PACKAGE_NAME);

        CoverageResult utilPackage = project.getSingletonChild();
        assertThat(utilPackage.getElement()).isEqualTo(PACKAGE);
        assertThat(utilPackage.getChildren()).hasSize(10).containsExactly("Ensure.java",
                "FilteredLog.java",
                "Generated.java",
                "NoSuchElementException.java",
                "PathUtil.java",
                "PrefixLogger.java",
                "StringContainsUtils.java",
                "TreeString.java",
                TREE_STRING_BUILDER,
                "VisibleForTesting.java");
        verifyCoverage(utilPackage, LINE, 294, 29);
        verifyCoverage(utilPackage, INSTRUCTION, 1260, 90);
        verifyCoverage(utilPackage, CONDITIONAL, 109, 7);

        CoverageResult treeStringBuilderFile = utilPackage.getChild(TREE_STRING_BUILDER);
        assertThat(treeStringBuilderFile.getElement()).isEqualTo(CoverageElement.FILE);
        assertThat(treeStringBuilderFile.getChildren()).hasSize(2).containsExactly(
                "edu/hm/hafner/util/TreeStringBuilder",
                "edu/hm/hafner/util/TreeStringBuilder$Child");
        verifyCoverage(treeStringBuilderFile, LINE, 51, 2);

        // FIXME: check why there is no INSTRUCTION result
        //        verifyCoverage(treeStringBuilderFile, INSTRUCTION, 229, 4);
        verifyCoverage(treeStringBuilderFile, CONDITIONAL, 17, 1);

        CoverageResult treeStringBuilderClass = treeStringBuilderFile.getChild("edu/hm/hafner/util/TreeStringBuilder");
        assertThat(treeStringBuilderClass.getElement()).isEqualTo(JavaCoverageReportAdapterDescriptor.CLASS);
        assertThat(treeStringBuilderClass.getChildren()).hasSize(7).containsExactly("edu.hm.hafner.util.TreeString intern(edu.hm.hafner.util.TreeString)",
                "edu.hm.hafner.util.TreeString intern(java.lang.String)",
                "edu.hm.hafner.util.TreeStringBuilder$Child getRoot()",
                "void <clinit>()",
                "void <init>()",
                "void dedup()",
                "void setRoot(edu.hm.hafner.util.TreeStringBuilder$Child)");
        verifyCoverage(treeStringBuilderClass, LINE, 8, 2);
        verifyCoverage(treeStringBuilderClass, INSTRUCTION, 37, 4);
        assertThat(treeStringBuilderClass.getCoverageFor(CONDITIONAL)).isEmpty();
    }

    private void verifyCoverage(final CoverageResult actualResult,
            final CoverageElement coverageElement, final int covered, final int missed) {
        assertThat(actualResult.getCoverageFor(coverageElement))
                .isNotEmpty()
                .contains(Ratio.create(covered, covered + missed));
    }
}
