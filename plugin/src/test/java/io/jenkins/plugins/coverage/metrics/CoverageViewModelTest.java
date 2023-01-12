package io.jenkins.plugins.coverage.metrics;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

import hudson.model.Run;

import io.jenkins.plugins.util.QualityGateResult;

import static io.jenkins.plugins.coverage.metrics.CoverageViewModel.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageViewModel}.
 *
 * @author Ullrich Hafner
 * @author Florian Orendi
 */
@SuppressWarnings("PMD.TooManyStaticImports")
class CoverageViewModelTest extends AbstractCoverageTest {
    /**
     * Creates a stub of {@link Node}, which represents indirect coverage changes and provides information about it.
     *
     * @param coverageChanges
     *         The indirect coverage change
     * @param metric
     *         The coverage metric
     * @param coverageFileChange
     *         The amount of files which contain indirect coverage changes
     * @param coverageLineChanges
     *         The amount of lines which contain indirect coverage changes
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Node createIndirectCoverageChangesNode(final Fraction coverageChanges,
            final Metric metric, final int coverageFileChange, final long coverageLineChanges) {
        var root = new ModuleNode("root");
        var builder = new CoverageBuilder().setMetric(Metric.LINE);
        for (int file = 0; file < 5; file++) {
            var fileNode = new FileNode("File-" + file);

            for (int line = 0; line < 2; line++) {
                fileNode.addCounters(10 + line, 1, 1);
                fileNode.addIndirectCoverageChange(10 + line, 2);
            }
            root.addChild(fileNode);
        }
        return root;
    }

    @Test
    void shouldReturnEmptySourceViewForExistingLinkButMissingSourceFile() {
        CoverageViewModel model = createModelFromCodingStyleReport();

        String hash = String.valueOf("PathUtil.java".hashCode());
        assertThat(model.getSourceCode(hash, ABSOLUTE_COVERAGE_TABLE_ID)).isEqualTo("n/a");
        assertThat(model.getSourceCode(hash, CHANGE_COVERAGE_TABLE_ID)).isEqualTo("n/a");
        assertThat(model.getSourceCode(hash, INDIRECT_COVERAGE_TABLE_ID)).isEqualTo("n/a");
    }

    @Test
    void shouldReportOverview() {
        CoverageViewModel model = createModelFromCodingStyleReport();

        CoverageOverview overview = model.getOverview();

        var expectedMetrics = new String[] {"Package", "File", "Class", "Method", "Line", "Instruction", "Branch"};
        assertThat(overview.getMetrics()).containsExactly(expectedMetrics);

        var expectedCovered = List.of(4, 7, 15, 97, 294, 1260, 109);
        assertThat(overview.getCovered()).containsExactlyElementsOf(expectedCovered);
        ensureValidPercentages(overview.getCoveredPercentages());

        var expectedMissed = List.of(0, 3, 3, 5, 29, 90, 7);
        assertThat(overview.getMissed()).containsExactlyElementsOf(expectedMissed);
        ensureValidPercentages(overview.getMissedPercentages());

        assertThatJson(overview).node("metrics").isArray().containsExactly(expectedMetrics);
        assertThatJson(overview).node("covered").isArray().containsExactlyElementsOf(expectedCovered);
        assertThatJson(overview).node("missed").isArray().containsExactlyElementsOf(expectedMissed);
    }

    private static void ensureValidPercentages(final List<Double> percentages) {
        assertThat(percentages).allSatisfy(d ->
                assertThat(d).isLessThanOrEqualTo(1.0).isGreaterThanOrEqualTo(0.0));
    }

    @Test
    void shouldProvideIndirectCoverageChanges() {
        Node node = createIndirectCoverageChangesNode(Fraction.ZERO, Metric.LINE, 1, 1);

        CoverageViewModel model = createModel(node);

        assertThat(model.hasIndirectCoverageChanges()).isTrue();
    }

    @Test
    void shouldProvideRightTableModelById() {
        CoverageViewModel model = createModelFromCodingStyleReport();
        assertThat(model.getTableModel(CHANGE_COVERAGE_TABLE_ID)).isInstanceOf(ChangeCoverageTableModel.class);
        assertThat(model.getTableModel(INDIRECT_COVERAGE_TABLE_ID)).isInstanceOf(IndirectCoverageChangesTable.class);
        assertThat(model.getTableModel(ABSOLUTE_COVERAGE_TABLE_ID)).isInstanceOf(CoverageTableModel.class);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> model.getTableModel("wrong-id"));
    }

    private CoverageViewModel createModelFromCodingStyleReport() {
        var model = createModel(readJacocoResult("jacoco-codingstyle.xml"));
        assertThat(model.getDisplayName()).contains("'Java coding style'");
        return model;
    }

    private CoverageViewModel createModel(final Node node) {
        return new CoverageViewModel(mock(Run.class), "id", StringUtils.EMPTY,
                node, AbstractCoverageTest.createStatistics(), new QualityGateResult(), "-", new FilteredLog("Errors"));
    }
}
