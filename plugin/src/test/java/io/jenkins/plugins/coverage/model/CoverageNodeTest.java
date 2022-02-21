package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
class CoverageNodeTest extends AbstractCoverageTest {
    private static final String PROJECT_NAME = "Java coding style: jacoco-codingstyle.xml";

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldReturnEmptyCoverageIfNotFound() {
        CoverageNode root = readExampleReport();

        assertThat(root.getCoverage(CoverageMetric.valueOf("new"))).isNotSet();
    }

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
                entry(MODULE, Fraction.ONE),
                entry(PACKAGE, Fraction.ONE),
                entry(FILE, Fraction.getFraction(7, 7 + 3)),
                entry(CLASS, Fraction.getFraction(15, 15 + 3)),
                entry(METHOD, Fraction.getFraction(97, 97 + 5)),
                entry(LINE, Fraction.getFraction(294, 294 + 29)),
                entry(INSTRUCTION, Fraction.getFraction(1260, 1260 + 90)),
                entry(BRANCH, Fraction.getFraction(109, 109 + 7)));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util")
        );
    }

    private void verifyCoverageMetrics(final CoverageNode tree) {
        assertThat(tree.getCoverage(LINE)).isSet()
                .hasCovered(294)
                .hasCoveredPercentage(Fraction.getFraction(294, 294 + 29))
                .hasMissed(29)
                .hasMissedPercentage(Fraction.getFraction(29, 294 + 29))
                .hasTotal(294 + 29);
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("91.02%");
        assertThat(tree.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("91,02%");

        assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(109)
                .hasCoveredPercentage(Fraction.getFraction(109, 109 + 7))
                .hasMissed(7)
                .hasMissedPercentage(Fraction.getFraction(7, 109 + 7))
                .hasTotal(109 + 7);
        assertThat(tree.printCoverageFor(BRANCH)).isEqualTo("93.97%");
        assertThat(tree.printCoverageFor(BRANCH, Locale.GERMAN)).isEqualTo("93,97%");

        assertThat(tree.getCoverage(INSTRUCTION)).isSet()
                .hasCovered(1260)
                .hasCoveredPercentage(Fraction.getFraction(1260, 1260 + 90))
                .hasMissed(90)
                .hasMissedPercentage(Fraction.getFraction(90, 1260 + 90))
                .hasTotal(1260 + 90);
        assertThat(tree.printCoverageFor(INSTRUCTION)).isEqualTo("93.33%");
        assertThat(tree.printCoverageFor(INSTRUCTION, Locale.GERMAN)).isEqualTo("93,33%");

        assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredPercentage(Fraction.ONE)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");
        assertThat(tree.printCoverageFor(MODULE, Locale.GERMAN)).isEqualTo("100,00%");

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
        CoverageNode tree = readNode("jacoco-analysis-model.xml");

        String checkStyleParser = "CheckStyleParser.java";
        Optional<CoverageNode> wrappedCheckStyle = tree.find(CoverageMetric.FILE, checkStyleParser);
        assertThat(wrappedCheckStyle).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node)
                        .hasName(checkStyleParser)
                        .hasPath("edu/hm/hafner/analysis/parser/checkstyle/CheckStyleParser.java")
        );

        CoverageNode checkStyle = wrappedCheckStyle.get();
        assertThat(checkStyle.getMetricPercentages())
                .containsEntry(FILE, Fraction.ONE)
                .containsEntry(CLASS, Fraction.ONE)
                .containsEntry(METHOD, Fraction.getFraction(6, 6))
                .containsEntry(LINE, Fraction.getFraction(41, 42))
                .containsEntry(INSTRUCTION, Fraction.getFraction(180, 187))
                .containsEntry(BRANCH, Fraction.getFraction(11, 12));

        String pmdParser = "PmdParser.java";
        Optional<CoverageNode> wrappedPmd = tree.find(CoverageMetric.FILE, pmdParser);
        assertThat(wrappedPmd).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node)
                        .hasName(pmdParser)
                        .hasPath("edu/hm/hafner/analysis/parser/pmd/PmdParser.java")
        );

        CoverageNode pmd = wrappedPmd.get();
        assertThat(pmd.getMetricPercentages())
                .containsEntry(FILE, Fraction.ONE)
                .containsEntry(CLASS, Fraction.ONE)
                .containsEntry(METHOD, Fraction.getFraction(8, 8))
                .containsEntry(LINE, Fraction.getFraction(72, 79))
                .containsEntry(INSTRUCTION, Fraction.getFraction(285, 313))
                .containsEntry(BRANCH, Fraction.getFraction(15, 18));

        assertThat(checkStyle.computeDelta(pmd))
                .containsEntry(FILE, Fraction.ZERO)
                .containsEntry(CLASS, Fraction.ZERO)
                .containsEntry(METHOD, Fraction.getFraction(0, 12))
                .containsEntry(LINE, Fraction.getFraction(215, 3318))
                .containsEntry(INSTRUCTION, Fraction.getFraction(3045, 58_531))
                .containsEntry(BRANCH, Fraction.getFraction(1, 12));

        assertThat(pmd.computeDelta(checkStyle))
                .containsEntry(FILE, Fraction.ZERO)
                .containsEntry(CLASS, Fraction.ZERO)
                .containsEntry(METHOD, Fraction.getFraction(0, 12))
                .containsEntry(LINE, Fraction.getFraction(-215, 3318))
                .containsEntry(INSTRUCTION, Fraction.getFraction(-3045, 58_531))
                .containsEntry(BRANCH, Fraction.getFraction(-1, 12));
    }

    @Test
    void shouldSplitComplexPackageStructure() {
        CoverageNode tree = readNode("jacoco-analysis-model.xml");

        assertThat(tree.getAll(PACKAGE)).hasSize(18);

        tree.splitPackages();

        assertThat(tree.getAll(PACKAGE)).hasSize(3 + 18);
    }

    @Test
    void shouldNotSplitPackagesIfOnWrongHierarchyNode() {
        CoverageNode tree = readExampleReport();
        CoverageNode packageNode = tree.getChildren().get(0);
        assertThat(packageNode).hasName("edu.hm.hafner.util").hasPath("edu/hm/hafner/util");

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
                    assertThat(node.printCoverageFor(BRANCH)).isEqualTo(Messages.Coverage_Not_Available());
                }
        );
    }

    @Test
    void shouldCreatePackageName() {
        CoverageNode tree = readExampleReport();

        String fileName = "Ensure.java";
        assertThat(tree.find(FILE, fileName)).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot()
        );

        tree.splitPackages();
        assertThat(tree.find(FILE, fileName)).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot()
        );
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(CoverageNode.class)
                .withPrefabValues(CoverageNode.class,
                        new CoverageNode(CoverageMetric.FILE, "file.txt"),
                        new CoverageNode(CoverageMetric.LINE, "line"))
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withIgnoredFields("parent")
                .verify();
    }

    private CoverageNode readExampleReport() {
        return readNode("jacoco-codingstyle.xml");
    }
}
