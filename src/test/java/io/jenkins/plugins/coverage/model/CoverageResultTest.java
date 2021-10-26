package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.JavaCoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * TODO: Move to corresponding test class.
 *
 * @author Ullrich Hafner
 */
class CoverageResultTest extends AbstractCoverageTest {
    private static final String EXPECTED_PACKAGE_NAME = "edu.hm.hafner.util";
    private static final String TREE_STRING_BUILDER_FILE_NAME = "edu/hm/hafner/util/TreeStringBuilder.java";
    private static final String TREE_STRING_BUILDER_CLASS = "edu.hm.hafner.util.TreeStringBuilder";

    private static final CoverageElement LINE = CoverageElement.LINE;
    private static final CoverageElement INSTRUCTION = JacocoReportAdapterDescriptor.INSTRUCTION;
    private static final CoverageElement BRANCH = CoverageElement.CONDITIONAL;
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
                "edu/hm/hafner/util/Ensure.java",
                "edu/hm/hafner/util/FilteredLog.java",
                "edu/hm/hafner/util/Generated.java",
                "edu/hm/hafner/util/NoSuchElementException.java",
                "edu/hm/hafner/util/PathUtil.java",
                "edu/hm/hafner/util/PrefixLogger.java",
                "edu/hm/hafner/util/StringContainsUtils.java",
                "edu/hm/hafner/util/TreeString.java",
                TREE_STRING_BUILDER_FILE_NAME,
                "edu/hm/hafner/util/VisibleForTesting.java");
        verifyCoverage(utilPackage, LINE, 294, 29);
        verifyCoverage(utilPackage, INSTRUCTION, 1260, 90);
        verifyCoverage(utilPackage, BRANCH, 109, 7);

        CoverageResult treeStringBuilderFile = utilPackage.getChild(TREE_STRING_BUILDER_FILE_NAME);
        assertThat(treeStringBuilderFile.getElement()).isEqualTo(SOURCE_FILE);
        assertThat(treeStringBuilderFile.getChildren()).hasSize(2).containsExactly(
                TREE_STRING_BUILDER_CLASS,
                "edu.hm.hafner.util.TreeStringBuilder.Child");
        verifyCoverage(treeStringBuilderFile, LINE, 51, 2);

        // FIXME: check why there is no INSTRUCTION result
        //        verifyCoverage(treeStringBuilderFile, INSTRUCTION, 229, 4);
        verifyCoverage(treeStringBuilderFile, BRANCH, 17, 1);

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
        assertThat(treeStringBuilderClass.getCoverageFor(BRANCH)).isEmpty();
    }

    private void verifyCoverage(final CoverageResult actualResult,
            final CoverageElement coverageElement, final int covered, final int missed) {
        assertThat(actualResult.getCoverageFor(coverageElement))
                .isNotEmpty()
                .contains(Ratio.create(covered, covered + missed));
    }
}
