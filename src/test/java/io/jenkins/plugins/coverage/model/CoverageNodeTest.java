package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
class CoverageNodeTest extends AbstractCoverageTest {
    @Test
    void shouldConvertCodingStyleToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-codingstyle.xml");

        CoverageNode tree = CoverageNode.fromResult(report);
//        tree.splitPackages();

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

        assertThat(tree.getAll(REPORT)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(SOURCE_FILE)).hasSize(10);
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
    }
}
