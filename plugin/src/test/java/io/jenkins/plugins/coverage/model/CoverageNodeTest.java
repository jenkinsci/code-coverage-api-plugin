package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 * @author Florian Orendi
 */
@DefaultLocale("en")
class CoverageNodeTest extends AbstractCoverageTest {
    private static final String PROJECT_NAME = "Java coding style: jacoco-codingstyle.xml";

    @Test
    void shouldProvideEmptyPathForDefaultPackage() {
        PackageCoverageNode node = new PackageCoverageNode("-");
        assertThat(node.getPath()).isEqualTo("");
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
        assertThat(tree.getMetricFractions()).containsExactly(
                entry(MODULE, Fraction.ONE),
                entry(PACKAGE, Fraction.ONE),
                entry(FILE, Fraction.getFraction(7, 7 + 3)),
                entry(CLASS, Fraction.getFraction(15, 15 + 3)),
                entry(METHOD, Fraction.getFraction(97, 97 + 5)),
                entry(LINE, Fraction.getFraction(294, 294 + 29)),
                entry(INSTRUCTION, Fraction.getFraction(1260, 1260 + 90)),
                entry(BRANCH, Fraction.getFraction(109, 109 + 7)));
        assertThat(tree.getMetricPercentages()).containsExactly(
                entry(MODULE, CoveragePercentage.valueOf(Fraction.ONE)),
                entry(PACKAGE, CoveragePercentage.valueOf(Fraction.ONE)),
                entry(FILE, CoveragePercentage.valueOf(
                        Fraction.getFraction(7, 7 + 3))),
                entry(CLASS, CoveragePercentage.valueOf(
                        Fraction.getFraction(15, 15 + 3))),
                entry(METHOD, CoveragePercentage.valueOf(
                        Fraction.getFraction(97, 97 + 5))),
                entry(LINE, CoveragePercentage.valueOf(
                        Fraction.getFraction(294, 294 + 29))),
                entry(INSTRUCTION, CoveragePercentage.valueOf(
                        Fraction.getFraction(1260, 1260 + 90))),
                entry(BRANCH, CoveragePercentage.valueOf(
                        Fraction.getFraction(109, 109 + 7))));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util")
        );
    }

    private void verifyCoverageMetrics(final CoverageNode tree) {
        Fraction coverageFraction = Fraction.getFraction(294, 294 + 29);
        Fraction missedFraction = Fraction.getFraction(29, 294 + 29);
        assertThat(tree.getCoverage(LINE)).isSet()
                .hasCovered(294)
                .hasCoveredFraction(coverageFraction)
                .hasCoveredPercentage(CoveragePercentage.valueOf(coverageFraction))
                .hasMissed(29)
                .hasMissedFraction(missedFraction)
                .hasMissedPercentage(CoveragePercentage.valueOf(missedFraction))
                .hasTotal(294 + 29);
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("91.02%");
        assertThat(tree.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("91,02%");

        coverageFraction = Fraction.getFraction(109, 109 + 7);
        missedFraction = Fraction.getFraction(7, 109 + 7);
        assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(109)
                .hasCoveredFraction(coverageFraction)
                .hasCoveredPercentage(CoveragePercentage.valueOf(coverageFraction))
                .hasMissed(7)
                .hasMissedFraction(missedFraction)
                .hasMissedPercentage(CoveragePercentage.valueOf(missedFraction))
                .hasTotal(109 + 7);
        assertThat(tree.printCoverageFor(BRANCH)).isEqualTo("93.97%");
        assertThat(tree.printCoverageFor(BRANCH, Locale.GERMAN)).isEqualTo("93,97%");

        coverageFraction = Fraction.getFraction(1260, 1260 + 90);
        missedFraction = Fraction.getFraction(90, 1260 + 90);
        assertThat(tree.getCoverage(INSTRUCTION)).isSet()
                .hasCovered(1260)
                .hasCoveredFraction(coverageFraction)
                .hasCoveredPercentage(CoveragePercentage.valueOf(coverageFraction))
                .hasMissed(90)
                .hasMissedFraction(missedFraction)
                .hasMissedPercentage(CoveragePercentage.valueOf(missedFraction))
                .hasTotal(1260 + 90);
        assertThat(tree.printCoverageFor(INSTRUCTION)).isEqualTo("93.33%");
        assertThat(tree.printCoverageFor(INSTRUCTION, Locale.GERMAN)).isEqualTo("93,33%");

        coverageFraction = Fraction.ONE;
        missedFraction = Fraction.ZERO;
        assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredFraction(coverageFraction)
                .hasCoveredPercentage(CoveragePercentage.valueOf(coverageFraction))
                .hasMissed(0)
                .hasMissedFraction(missedFraction)
                .hasMissedPercentage(CoveragePercentage.valueOf(missedFraction))
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");
        assertThat(tree.printCoverageFor(MODULE, Locale.GERMAN)).isEqualTo("100,00%");

        assertThat(tree).hasName(PROJECT_NAME)
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName(CoverageNode.ROOT);
    }

    @Test
    void shouldNotReturnCoverageValuesWithoutLeaves() {
        CoverageNode coverageNode = new CoverageNode(MODULE, "Root");
        assertThat(coverageNode).isRoot();
        assertThat(coverageNode.getMetricFractions()).isEmpty();
        assertThat(coverageNode.getMetricPercentages()).isEmpty();
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
        String checkStyleParserPath = "edu/hm/hafner/analysis/parser/checkstyle/" + checkStyleParser;
        Optional<CoverageNode> wrappedCheckStyle = tree.find(CoverageMetric.FILE, checkStyleParserPath);
        assertThat(wrappedCheckStyle).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node)
                        .hasName(checkStyleParser)
                        .hasPath(checkStyleParserPath)
        );

        CoverageNode checkStyle = wrappedCheckStyle.get();
        assertThat(checkStyle.getMetricFractions())
                .containsEntry(FILE, Fraction.ONE)
                .containsEntry(CLASS, Fraction.ONE)
                .containsEntry(METHOD, Fraction.getFraction(6, 6))
                .containsEntry(LINE, Fraction.getFraction(41, 42))
                .containsEntry(INSTRUCTION, Fraction.getFraction(180, 187))
                .containsEntry(BRANCH, Fraction.getFraction(11, 12));
        assertThat(checkStyle.getMetricFractions())
                .containsEntry(FILE, Fraction.ONE)
                .containsEntry(CLASS, Fraction.ONE)
                .containsEntry(METHOD, Fraction.getFraction(6, 6))
                .containsEntry(LINE, Fraction.getFraction(41, 42))
                .containsEntry(INSTRUCTION, Fraction.getFraction(180, 187))
                .containsEntry(BRANCH, Fraction.getFraction(11, 12));

        String pmdParser = "PmdParser.java";
        String pmdParserPath = "edu/hm/hafner/analysis/parser/pmd/" + pmdParser;
        Optional<CoverageNode> wrappedPmd = tree.find(CoverageMetric.FILE, pmdParserPath);
        assertThat(wrappedPmd).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node)
                        .hasName(pmdParser)
                        .hasPath(pmdParserPath)
        );

        CoverageNode pmd = wrappedPmd.get();
        assertThat(pmd.getMetricFractions())
                .containsEntry(FILE, Fraction.ONE)
                .containsEntry(CLASS, Fraction.ONE)
                .containsEntry(METHOD, Fraction.getFraction(8, 8))
                .containsEntry(LINE, Fraction.getFraction(72, 79))
                .containsEntry(INSTRUCTION, Fraction.getFraction(285, 313))
                .containsEntry(BRANCH, Fraction.getFraction(15, 18));
        assertThat(pmd.getMetricPercentages())
                .containsEntry(FILE, CoveragePercentage.valueOf(Fraction.ONE))
                .containsEntry(CLASS, CoveragePercentage.valueOf(Fraction.ONE))
                .containsEntry(METHOD, CoveragePercentage.valueOf(
                        Fraction.getFraction(8, 8)))
                .containsEntry(LINE, CoveragePercentage.valueOf(
                        Fraction.getFraction(72, 79)))
                .containsEntry(INSTRUCTION, CoveragePercentage.valueOf(
                        Fraction.getFraction(285, 313)))
                .containsEntry(BRANCH, CoveragePercentage.valueOf(
                        Fraction.getFraction(15, 18)));

        assertThat(checkStyle.computeDelta(pmd))
                .containsEntry(FILE, Fraction.ZERO)
                .containsEntry(CLASS, Fraction.ZERO)
                .containsEntry(METHOD, Fraction.getFraction(0, 12))
                .containsEntry(LINE, Fraction.getFraction(215, 3318))
                .containsEntry(INSTRUCTION, Fraction.getFraction(3045, 58_531))
                .containsEntry(BRANCH, Fraction.getFraction(1, 12));
        assertThat(checkStyle.computeDeltaAsPercentage(pmd))
                .containsEntry(FILE, CoveragePercentage.valueOf(Fraction.ZERO))
                .containsEntry(CLASS, CoveragePercentage.valueOf(Fraction.ZERO))
                .containsEntry(METHOD, CoveragePercentage.valueOf(
                        Fraction.getFraction(0, 12)))
                .containsEntry(LINE, CoveragePercentage.valueOf(
                        Fraction.getFraction(215, 3318)))
                .containsEntry(INSTRUCTION, CoveragePercentage.valueOf(
                        Fraction.getFraction(3045, 58_531)))
                .containsEntry(BRANCH, CoveragePercentage.valueOf(
                        Fraction.getFraction(1, 12)));

        assertThat(pmd.computeDelta(checkStyle))
                .containsEntry(FILE, Fraction.ZERO)
                .containsEntry(CLASS, Fraction.ZERO)
                .containsEntry(METHOD, Fraction.getFraction(0, 12))
                .containsEntry(LINE, Fraction.getFraction(-215, 3318))
                .containsEntry(INSTRUCTION, Fraction.getFraction(-3045, 58_531))
                .containsEntry(BRANCH, Fraction.getFraction(-1, 12));
        assertThat(pmd.computeDeltaAsPercentage(checkStyle))
                .containsEntry(FILE, CoveragePercentage.valueOf(Fraction.ZERO))
                .containsEntry(CLASS, CoveragePercentage.valueOf(Fraction.ZERO))
                .containsEntry(METHOD, CoveragePercentage.valueOf(
                        Fraction.getFraction(0, 12)))
                .containsEntry(LINE, CoveragePercentage.valueOf(
                        Fraction.getFraction(-215, 3318)))
                .containsEntry(INSTRUCTION, CoveragePercentage.valueOf(
                        Fraction.getFraction(-3045, 58_531)))
                .containsEntry(BRANCH, CoveragePercentage.valueOf(
                        Fraction.getFraction(-1, 12)));
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
        String filePath = "edu/hm/hafner/util/" + fileName;
        assertThat(tree.findByHashCode(FILE, filePath.hashCode())).isNotEmpty().hasValueSatisfying(
                        node -> assertThat(node).hasPath(filePath).isNotRoot());
        assertThat(tree.findByHashCode(FILE, fileName.hashCode())).isEmpty();
        assertThat(tree.findByHashCode(FILE, "not-found".hashCode())).isEmpty();

        String noBranchCoverage = "NoSuchElementException.java";
        String noBranchCoveragePath = "edu/hm/hafner/util/" + noBranchCoverage;
        assertThat(tree.find(FILE, noBranchCoveragePath)).isNotEmpty().hasValueSatisfying(
                node -> {
                    assertThat(node)
                            .hasName(noBranchCoverage)
                            .hasPath(noBranchCoveragePath)
                            .isNotRoot();
                    assertThat(node.getCoverage(BRANCH)).isNotSet();
                    assertThat(node.printCoverageFor(BRANCH)).isEqualTo(Messages.Coverage_Not_Available());
                }
        );
    }

    @Test
    void shouldCreatePackageName() {
        CoverageNode tree = readExampleReport();

        String fileName = "Ensure.java";
        String filePath = "edu/hm/hafner/util/" + fileName;
        assertThat(tree.find(FILE, filePath)).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot()
        );

        tree.splitPackages();
        assertThat(tree.find(FILE, filePath)).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot()
        );
    }

    @Test
    void shouldRemovePackagesWithoutFiles() {
        CoverageNode tree = readNode("jacoco-analysis-model.xml");
        tree.splitPackages();
        int packageNodes = tree.getChildren().size();
        CoverageNode filteredTree = tree.filterPackageStructure();

        assertThat(filteredTree.getChildren().stream()
                .noneMatch(node -> node.getChildren().stream()
                        .noneMatch(child -> child.getMetric().equals(FILE))))
                .isTrue();
        assertThat(tree.getChildren().size()).isEqualTo(packageNodes);
    }

    @Test
    void shouldGetAllFileCoverageNodes() {
        CoverageNode tree = readNode("jacoco-analysis-model.xml");
        tree.splitPackages();
        assertThat(tree.getAllFileCoverageNodes())
                .hasSize(307)
                .satisfies(nodes -> nodes.forEach(
                        node -> assertThat(node).isInstanceOf(FileCoverageNode.class)));
    }

    @Test
    void shouldProvideExistentChangeCoverage() {
        CoverageNode tree = createTreeWithMockedTreeCreator();
        assertThat(tree.hasCodeChanges()).isTrue();
        assertThat(tree.hasChangeCoverage()).isTrue();
        assertThat(tree.hasChangeCoverage(LINE)).isTrue();
        assertThat(tree.getChangeCoverageTree()).isEqualTo(tree);
        assertThat(tree.getFileAmountWithChangedCoverage()).isOne();
        assertThat(tree.getLineAmountWithChangedCoverage()).isOne();
    }

    @Test
    void shouldProvideExistentIndirectCoverageChanges() {
        CoverageNode tree = createTreeWithMockedTreeCreator();
        assertThat(tree.hasIndirectCoverageChanges()).isTrue();
        assertThat(tree.hasIndirectCoverageChanges(LINE)).isTrue();
        assertThat(tree.getIndirectCoverageChangesTree()).isEqualTo(tree);
        assertThat(tree.getFileAmountWithIndirectCoverageChanges()).isOne();
        assertThat(tree.getLineAmountWithIndirectCoverageChanges()).isOne();
    }

    @Test
    void shouldDetermineNotExistentChangeCoverage() {
        CoverageNode tree = createTreeWithMockedTreeCreatorWithoutChanges();
        assertThat(tree.hasCodeChanges()).isFalse();
        assertThat(tree.hasChangeCoverage()).isFalse();
        assertThat(tree.hasChangeCoverage(LINE)).isFalse();
        assertThat(tree.getChangeCoverageTree())
                .hasNoChildren()
                .hasNoLeaves();
        assertThat(tree.getFileAmountWithChangedCoverage()).isZero();
        assertThat(tree.getLineAmountWithChangedCoverage()).isZero();
    }

    @Test
    void shouldDetermineNotExistentIndirectCoverageChanges() {
        CoverageNode tree = createTreeWithMockedTreeCreatorWithoutChanges();
        assertThat(tree.hasIndirectCoverageChanges()).isFalse();
        assertThat(tree.hasIndirectCoverageChanges(LINE)).isFalse();
        assertThat(tree.getIndirectCoverageChangesTree())
                .hasNoChildren()
                .hasNoLeaves();
        assertThat(tree.getFileAmountWithIndirectCoverageChanges()).isZero();
        assertThat(tree.getLineAmountWithIndirectCoverageChanges()).isZero();
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

    /**
     * Reads the coverage tree from the report 'jacoco-codingstyle.xml'.
     *
     * @return the {@link CoverageNode} root of the tree
     */
    private CoverageNode readExampleReport() {
        return readNode("jacoco-codingstyle.xml");
    }

    /**
     * Creates a coverage tree with a mocked {@link CoverageTreeCreator} in order to test calculations for change
     * coverage and indirect coverage changes.
     *
     * @return the {@link CoverageNode root} of the created tree
     */
    private CoverageNode createTreeWithMockedTreeCreator() {
        CoverageTreeCreator coverageTreeCreator = mock(CoverageTreeCreator.class);
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, CoverageNode.ROOT, coverageTreeCreator);

        FileCoverageNode fileNode = new FileCoverageNode("test", "test");
        fileNode.putCoveragePerLine(1, CoverageBuilder.NO_COVERAGE);
        fileNode.addChangedCodeLine(1);
        fileNode.addChangedCodeLine(2);
        fileNode.putIndirectCoverageChange(3, 1);
        CoverageNode tree = readExampleReport();
        root.add(tree);
        root.add(fileNode);

        when(coverageTreeCreator.createChangeCoverageTree(root)).thenReturn(root);
        when(coverageTreeCreator.createIndirectCoverageChangesTree(root)).thenReturn(root);

        return root;
    }

    /**
     * Creates a coverage tree with a mocked {@link CoverageTreeCreator} and without changes in order to test
     * calculations for non existent change coverage and indirect coverage changes.
     *
     * @return the {@link CoverageNode root} of the created tree
     */
    private CoverageNode createTreeWithMockedTreeCreatorWithoutChanges() {
        CoverageTreeCreator coverageTreeCreator = mock(CoverageTreeCreator.class);
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, CoverageNode.ROOT, coverageTreeCreator);

        when(coverageTreeCreator.createChangeCoverageTree(root)).thenReturn(root);
        when(coverageTreeCreator.createIndirectCoverageChangesTree(root)).thenReturn(root);

        return root;
    }
}
