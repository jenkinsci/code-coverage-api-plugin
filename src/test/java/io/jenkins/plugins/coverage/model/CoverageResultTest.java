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

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * TODO: Move to corresponding test class.
 *
 * @author Ullrich Hafner
 */
class CoverageResultTest extends ResourceTest {
    private static final String EXPECTED_PACKAGE_NAME = "edu.hm.hafner.util";
    private static final String TREE_STRING_BUILDER_FILE_NAME = "TreeStringBuilder.java";
    private static final String TREE_STRING_BUILDER_CLASS = "edu.hm.hafner.util.TreeStringBuilder";

    private static final CoverageElement LINE = CoverageElement.LINE;
    private static final CoverageElement INSTRUCTION = JacocoReportAdapterDescriptor.INSTRUCTION;
    private static final CoverageElement CONDITIONAL = CoverageElement.CONDITIONAL;
    private static final CoverageElement PACKAGE = JavaCoverageReportAdapterDescriptor.PACKAGE;
    private static final CoverageElement REPORT = CoverageElement.REPORT;
    private static final CoverageElement SOURCE_FILE = CoverageElement.FILE;
    private static final CoverageElement CLASS_NAME = JavaCoverageReportAdapterDescriptor.CLASS;

    @Test
    void shouldReadJaCoCoResult() throws CoverageException {
        CoverageResult project = readReport("jacoco-codingstyle.xml");

        assertThat(project.getElement()).isEqualTo(REPORT);
        assertThat(project.hasSingletonChild()).isTrue();
        assertThat(project.getChildren()).hasSize(1).containsExactly(EXPECTED_PACKAGE_NAME);

        CoverageResult utilPackage = project.getSingletonChild();
        assertThat(utilPackage.getElement()).isEqualTo(PACKAGE);
        assertThat(utilPackage.getChildren()).hasSize(10).containsExactly(
                "Ensure.java",
                "FilteredLog.java",
                "Generated.java",
                "NoSuchElementException.java",
                "PathUtil.java",
                "PrefixLogger.java",
                "StringContainsUtils.java",
                "TreeString.java",
                TREE_STRING_BUILDER_FILE_NAME,
                "VisibleForTesting.java");
        verifyCoverage(utilPackage, LINE, 294, 29);
        verifyCoverage(utilPackage, INSTRUCTION, 1260, 90);
        verifyCoverage(utilPackage, CONDITIONAL, 109, 7);

        CoverageResult treeStringBuilderFile = utilPackage.getChild(TREE_STRING_BUILDER_FILE_NAME);
        assertThat(treeStringBuilderFile.getElement()).isEqualTo(SOURCE_FILE);
        assertThat(treeStringBuilderFile.getChildren()).hasSize(2).containsExactly(
                TREE_STRING_BUILDER_CLASS,
                "edu.hm.hafner.util.TreeStringBuilder.Child");
        verifyCoverage(treeStringBuilderFile, LINE, 51, 2);

        // FIXME: check why there is no INSTRUCTION result
        //        verifyCoverage(treeStringBuilderFile, INSTRUCTION, 229, 4);
        verifyCoverage(treeStringBuilderFile, CONDITIONAL, 17, 1);

        CoverageResult treeStringBuilderClass = treeStringBuilderFile.getChild(TREE_STRING_BUILDER_CLASS);
        assertThat(treeStringBuilderClass.getElement()).isEqualTo(CLASS_NAME);
        assertThat(treeStringBuilderClass.getChildren()).hasSize(7)
                .containsExactly("edu.hm.hafner.util.TreeString intern(edu.hm.hafner.util.TreeString)",
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

    @Test
    void shouldConvertToNewModel() throws CoverageException {
        CoverageResult report = readReport("jacoco-codingstyle.xml");
        CoverageResult utilPackage = report.getSingletonChild();
        CoverageResult treeStringBuilderFile = utilPackage.getChild(TREE_STRING_BUILDER_FILE_NAME);
        CoverageResult treeStringBuilderClass = treeStringBuilderFile.getChild(TREE_STRING_BUILDER_CLASS);

        Ratio lineCoverage = treeStringBuilderClass.getCoverageFor(CoverageElement.LINE).get();
        Coverage coverage = new Coverage(lineCoverage);
        assertThat(coverage).hasCovered(8).hasMissed(2);

        CoverageNode root = CoverageNode.fromResult(report);
        assertThat(root.getCoverage(LINE.getName())).hasCovered(294).hasMissed(29);
        assertThat(root.getCoverage(CONDITIONAL.getName())).hasCovered(109).hasMissed(7);
        assertThat(root.getCoverage(INSTRUCTION.getName())).hasCovered(1260).hasMissed(90);
    }

    @Test
    void shouldConvertCodingStyleToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-codingstyle.xml");

        CoverageNode tree = CoverageNode.fromResult(report);
        tree.splitPackages();

        TreeChartNode root = tree.toChartTree();
        assertThat(root).hasName("Java coding style: jacoco-codingstyle.xml").hasValue(323.0, 294.0);

        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> assertThat(node).hasName("edu.hm.hafner.util").hasValue(323.0, 294.0)
        );
    }

    @Test
    void shouldConvertAnalysisModelToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-analysis-model.xml");

        CoverageNode tree = CoverageNode.fromResult(report);
        tree.splitPackages();

        TreeChartNode root = tree.toChartTree();

        assertThat(root).hasName("Static Analysis Model and Parsers: jacoco-analysis-model.xml").hasValue(6368.0, 6083.0);
        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> assertThat(node).hasName("edu.hm.hafner").hasValue(6368.0, 6083.0)
        );
    }

    private CoverageResult readReport(final String fileName) throws CoverageException {
        JacocoReportAdapter parser = new JacocoReportAdapter("Hello");
        CoverageElementRegister.addCoverageElements(new JacocoReportAdapterDescriptor().getCoverageElements());
        CoverageResult result = parser.getResult(getResourceAsFile(fileName).toFile());
        result.stripGroup();
        return result;
    }
}
