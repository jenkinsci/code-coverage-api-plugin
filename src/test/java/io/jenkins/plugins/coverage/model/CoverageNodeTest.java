package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.Optional;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.CoverageNodeConverter;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
class CoverageNodeTest extends AbstractCoverageTest {
    private static final String PROJECT_NAME = "Java coding style: jacoco-codingstyle.xml";

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, ""));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, "."));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldConvertCodingStyleToTree() {
        CoverageNode tree = readExampleReport();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        List<CoverageNode> files = tree.getAll(FILE);
        assertThat(files).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, INSTRUCTION)
                .hasToString("[Module] " + PROJECT_NAME);
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(1, 0)),
                entry(PACKAGE, new Coverage(1, 0)),
                entry(FILE, new Coverage(7, 3)),
                entry(CLASS, new Coverage(15, 3)),
                entry(METHOD, new Coverage(97, 5)),
                entry(LINE, new Coverage(294, 29)),
                entry(INSTRUCTION, new Coverage(1260, 90)),
                entry(BRANCH, new Coverage(109, 7)));
        assertThat(tree.getMetricPercentages()).containsExactly(
                entry(MODULE, 1.0),
                entry(PACKAGE, 1.0),
                entry(FILE, 0.7),
                entry(CLASS, 15.0 / 18),
                entry(METHOD, 97.0 / 102),
                entry(LINE, 294.0 / 323),
                entry(INSTRUCTION, 1260.0 / 1350),
                entry(BRANCH, 109.0 / 116));

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
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("91.02%");

        assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(109)
                .hasCoveredPercentageCloseTo(0.93, PRECISION)
                .hasMissed(7)
                .hasMissedPercentageCloseTo(0.07, PRECISION)
                .hasTotal(109 + 7);
        assertThat(tree.printCoverageFor(BRANCH)).isEqualTo("93.97%");

        assertThat(tree.getCoverage(INSTRUCTION)).isSet()
                .hasCovered(1260)
                .hasCoveredPercentageCloseTo(0.93, PRECISION)
                .hasMissed(90)
                .hasMissedPercentageCloseTo(0.07, PRECISION)
                .hasTotal(1260 + 90);
        assertThat(tree.printCoverageFor(INSTRUCTION)).isEqualTo("93.33%");

        assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredPercentageCloseTo(1, PRECISION)
                .hasMissed(0)
                .hasMissedPercentageCloseTo(0, PRECISION)
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");

        assertThat(tree).hasName(PROJECT_NAME)
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName(CoverageNode.ROOT);
    }

    @Test
    void shouldSplitPackages() {
        CoverageNode tree = readExampleReport();

        tree.splitPackages();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        assertThat(tree.getMetricsDistribution()).contains(
                entry(PACKAGE, new Coverage(4, 0)));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu")
                        .hasParent()
                        .hasParentName(PROJECT_NAME)
        );
    }

    @Test
    void shouldComputeDelta() {
        CoverageNode tree = CoverageNodeConverter.convert(readResult("jacoco-analysis-model.xml"));

        String checkStyleParser = "CheckStyleParser.java";
        Optional<CoverageNode> wrappedCheckStyle = tree.find(CoverageMetric.FILE, checkStyleParser);
        assertThat(wrappedCheckStyle).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(checkStyleParser)
        );

        CoverageNode checkStyle = wrappedCheckStyle.get();
        assertThat(checkStyle.getMetricPercentages())
                .containsEntry(FILE, 1.0)
                .containsEntry(CLASS, 1.0)
                .containsEntry(METHOD, 1.0)
                .extractingByKey(LINE).satisfies(
                        p -> assertThat(p).isEqualTo(0.97, Offset.offset(0.01)));

        String pmdParser = "PmdParser.java";
        Optional<CoverageNode> wrappedPmd = tree.find(CoverageMetric.FILE, pmdParser);
        assertThat(wrappedPmd).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(pmdParser)
        );

        CoverageNode pmd = wrappedPmd.get();
        assertThat(pmd.getMetricPercentages())
                .containsEntry(FILE, 1.0)
                .containsEntry(CLASS, 1.0)
                .containsEntry(METHOD, 1.0)
                .extractingByKey(LINE).satisfies(
                        p -> assertThat(p).isEqualTo(0.91, Offset.offset(0.01)));

        assertThat(checkStyle.computeDelta(pmd))
                .containsEntry(FILE, 0.0)
                .containsEntry(CLASS, 0.0)
                .containsEntry(METHOD, 0.0)
                .extractingByKey(LINE).satisfies(
                        p -> assertThat(p).isEqualTo(0.06, Offset.offset(0.01)));

        assertThat(pmd.computeDelta(checkStyle))
                .containsEntry(FILE, 0.0)
                .containsEntry(CLASS, 0.0)
                .containsEntry(METHOD, 0.0)
                .extractingByKey(LINE).satisfies(
                        p -> assertThat(p).isEqualTo(-0.06, Offset.offset(0.01)));
    }

    @Test
    void shouldSplitComplexPackageStructure() {
        CoverageNode tree = CoverageNodeConverter.convert(readResult("jacoco-analysis-model.xml"));

        assertThat(tree.getAll(PACKAGE)).hasSize(18);

        tree.splitPackages();

        assertThat(tree.getAll(PACKAGE)).hasSize(3 + 18);
    }

    @Test
    void shouldNotSplitPackagesIfOnWrongHierarchyNode() {
        CoverageNode tree = readExampleReport();
        CoverageNode packageNode = tree.getChildren().get(0);
        assertThat(packageNode).hasName("edu.hm.hafner.util");

        List<CoverageNode> files = packageNode.getChildren();

        packageNode.splitPackages();
        assertThat(packageNode).hasName("edu.hm.hafner.util");
        assertThat(packageNode).hasChildren(files);
    }

    @Test
    void shouldThrowExceptionWhenObtainingAllBasicBlocks() {
        CoverageNode tree = readExampleReport();

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(LINE));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(BRANCH));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(INSTRUCTION));
    }

    @Test
    void shouldFindImportantElements() {
        CoverageNode tree = readExampleReport();

        assertThat(tree.getImportantMetrics()).containsExactly(LINE, BRANCH);
    }

    @Test
    void shouldFindFiles() {
        CoverageNode tree = readExampleReport();
        tree.splitPackages();

        String fileName = "Ensure.java";
        assertThat(tree.findByHashCode(FILE, fileName.hashCode())).isNotEmpty().hasValueSatisfying(
                node -> {
                    assertThat(node).hasName(fileName).isNotRoot().hasUncoveredLines(
                            78, 138, 139, 153, 154, 240, 245, 303, 340, 390, 395, 444, 476,
                            483, 555, 559, 568, 600, 626, 627, 628, 650, 653, 690, 720);
                }
        );
        assertThat(tree.findByHashCode(PACKAGE, fileName.hashCode())).isEmpty();
        assertThat(tree.findByHashCode(FILE, "not-found".hashCode())).isEmpty();

        String noBranchCoverage = "NoSuchElementException.java";
        assertThat(tree.find(FILE, noBranchCoverage)).isNotEmpty().hasValueSatisfying(
                node -> {
                    assertThat(node).hasName(noBranchCoverage).isNotRoot();
                    assertThat(node.getCoverage(BRANCH)).isNotSet();
                    assertThat(node.printCoverageFor(BRANCH)).isEqualTo(Coverage.COVERAGE_NOT_AVAILABLE);
                }
        );

    }

    @Test
    void shouldCreatePackageName() {
        CoverageNode tree = readExampleReport();

        String fileName = "Ensure.java";
        assertThat(tree.find(FILE, fileName)).isNotEmpty().hasValueSatisfying(
                node -> {
                    assertThat(node).hasName(fileName)
                            .hasParentName("edu.hm.hafner.util")
                            .hasParent()
                            .isNotRoot();
                }
        );

        tree.splitPackages();
        assertThat(tree.find(FILE, fileName)).isNotEmpty().hasValueSatisfying(
                node -> {
                    assertThat(node).hasName(fileName)
                            .hasParentName("edu.hm.hafner.util")
                            .hasParent()
                            .isNotRoot();
                }
        );
    }

    private CoverageNode readExampleReport() {
        return CoverageNodeConverter.convert(readResult("jacoco-codingstyle.xml"));
    }
}
