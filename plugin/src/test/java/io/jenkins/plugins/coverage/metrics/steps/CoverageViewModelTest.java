package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.FilteredLog;

import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.util.QualityGateResult;

import static io.jenkins.plugins.coverage.metrics.steps.CoverageViewModel.*;
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

        var expectedMetrics = new String[] {"Package", "File", "Class", "Method", "Line", "Branch", "Instruction"};
        assertThat(overview.getMetrics()).containsExactly(expectedMetrics);

        var expectedCovered = List.of(4, 7, 15, 97, 294, 109, 1260);
        assertThat(overview.getCovered()).containsExactlyElementsOf(expectedCovered);
        ensureValidPercentages(overview.getCoveredPercentages());

        var expectedMissed = List.of(0, 3, 3, 5, 29, 7, 90);
        assertThat(overview.getMissed()).containsExactlyElementsOf(expectedMissed);
        ensureValidPercentages(overview.getMissedPercentages());

        assertThatJson(overview).node("metrics").isArray().containsExactly(expectedMetrics);
        assertThatJson(overview).node("covered").isArray().containsExactlyElementsOf(expectedCovered);
        assertThatJson(overview).node("missed").isArray().containsExactlyElementsOf(expectedMissed);
    }

    private static void ensureValidPercentages(final List<Double> percentages) {
        assertThat(percentages).allSatisfy(d ->
                assertThat(d).isLessThanOrEqualTo(100.0).isGreaterThanOrEqualTo(0.0));
    }

    @Test
    void shouldProvideIndirectCoverageChanges() {
        Node node = createIndirectCoverageChangesNode();

        CoverageViewModel model = createModel(node);

        assertThat(model.hasIndirectCoverageChanges()).isTrue();
    }

    private Node createIndirectCoverageChangesNode() {
        var root = new ModuleNode("root");
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
                node, AbstractCoverageTest.createStatistics(), new QualityGateResult(), "-", new FilteredLog("Errors"),
                i -> i);
    }
}
