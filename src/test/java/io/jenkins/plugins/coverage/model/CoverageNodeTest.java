package io.jenkins.plugins.coverage.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.CoverageNodeConverter;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
class CoverageNodeTest extends AbstractCoverageTest {
    @Test
    void shouldConvertCodingStyleToTree() {
        CoverageNode tree = readExampleReport();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(REPORT)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        List<CoverageNode> files = tree.getAll(SOURCE_FILE);
        assertThat(files).hasSize(10);
        assertThat(tree.getAll(CLASS_NAME)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyElements(REPORT, PACKAGE, SOURCE_FILE, CLASS_NAME, METHOD, LINE, BRANCH, INSTRUCTION);
        assertThat(tree.getElementDistribution()).containsExactly(
                entry(REPORT, new Coverage(1, 0)),
                entry(PACKAGE, new Coverage(1, 0)),
                entry(SOURCE_FILE, new Coverage(7, 3)),
                entry(CLASS_NAME, new Coverage(15, 3)),
                entry(METHOD, new Coverage(97, 5)),
                entry(INSTRUCTION, new Coverage(1260, 90)),
                entry(LINE, new Coverage(294, 29)),
                entry(BRANCH, new Coverage(109, 7)));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util")
        );
    }

    private void verifyCoverageMetrics(final CoverageNode tree) {
        assertThat(tree.getCoverage(LINE)).isSet()
                .hasCovered(294)
                .hasCoveredPercentageCloseTo(0.91, PRECISION)
                .hasMissed(29)
                .hasMissedPercentageCloseTo(0.09, PRECISION)
                .hasTotal(294 + 29);
        assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(109)
                .hasCoveredPercentageCloseTo(0.93, PRECISION)
                .hasMissed(7)
                .hasMissedPercentageCloseTo(0.07, PRECISION)
                .hasTotal(109 + 7);
        assertThat(tree.getCoverage(INSTRUCTION)).isSet()
                .hasCovered(1260)
                .hasCoveredPercentageCloseTo(0.93, PRECISION)
                .hasMissed(90)
                .hasMissedPercentageCloseTo(0.07, PRECISION)
                .hasTotal(1260 + 90);
        assertThat(tree.getCoverage(REPORT)).isSet()
                .hasCovered(1)
                .hasCoveredPercentageCloseTo(1, PRECISION)
                .hasMissed(0)
                .hasMissedPercentageCloseTo(0, PRECISION)
                .hasTotal(1);

        assertThat(tree).hasName("Java coding style: jacoco-codingstyle.xml")
                .doesNotHaveParent()
                .isRoot()
                .hasElement(REPORT).hasParentName(CoverageNode.ROOT);
    }

    @Test
    void shouldSplitPackages() {
        CoverageNode tree = readExampleReport();
        tree.splitPackages();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        assertThat(tree.getElementDistribution()).contains(
                entry(PACKAGE, new Coverage(4, 0)));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu")
        );
    }

    @Test
    void shouldThrowExceptionWhenObtainingAllBasicBlocks() {
        CoverageNode tree = readExampleReport();

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(LINE));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(BRANCH));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(INSTRUCTION));
    }

    @Test
    void shouldFindFiles() {
        CoverageNode tree = readExampleReport();

        String fileName = "Ensure.java";
        assertThat(tree.findByHashCode(SOURCE_FILE, fileName.hashCode())).isNotEmpty().hasValueSatisfying(
                node -> {
                    assertThat(node).hasName(fileName);
                    assertThat(node.getUncoveredLines()).containsExactly(
                            78, 138, 139, 153, 154, 240, 245, 303, 340, 390, 395, 444, 476,
                            483, 555, 559, 568, 600, 626, 627, 628, 650, 653, 690, 720);
                }
        );
        assertThat(tree.findByHashCode(PACKAGE, fileName.hashCode())).isEmpty();
        assertThat(tree.findByHashCode(SOURCE_FILE, "not-found".hashCode())).isEmpty();
    }

    private CoverageNode readExampleReport() {
        return CoverageNodeConverter.convert(readResult("jacoco-codingstyle.xml"));
    }
}
