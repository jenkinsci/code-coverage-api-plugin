package io.jenkins.plugins.coverage.model;

import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Node;

import hudson.model.Run;

import io.vavr.collection.List;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static io.jenkins.plugins.coverage.model.CoverageViewModel.*;
import static io.jenkins.plugins.coverage.model.testutil.CoverageStubs.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
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
        CoverageViewModel model = createModel();

        String hash = String.valueOf("PathUtil.java".hashCode());
        assertThat(model.getSourceCode(hash, ABSOLUTE_COVERAGE_TABLE_ID)).isEqualTo("n/a");
        assertThat(model.getSourceCode(hash, CHANGE_COVERAGE_TABLE_ID)).isEqualTo("n/a");
        assertThat(model.getSourceCode(hash, INDIRECT_COVERAGE_TABLE_ID)).isEqualTo("n/a");
    }

    @Test
    void shouldReportOverview() {
        CoverageViewModel model = createModel();

        assertThat(model.getDisplayName()).contains("'Java coding style'");

        CoverageOverview overview = model.getOverview();

        var expectedMetrics = new String[] {"File", "Class", "Method", "Line", "Instruction", "Branch"};
        assertThat(overview.getMetrics()).containsExactly(expectedMetrics);

        var expectedCovered = List.ofAll(7, 15, 97, 294, 1260, 109);
        assertThat(overview.getCovered()).containsExactlyElementsOf(expectedCovered);
        assertThat(overview.getCoveredPercentages()).allSatisfy(d -> assertThat(d).isStrictlyBetween(0.0, 1.0));

        var expectedMissed = List.ofAll(3, 3, 5, 29, 90, 7);
        assertThat(overview.getMissed()).containsExactlyElementsOf(expectedMissed);
        assertThat(overview.getMissedPercentages()).allSatisfy(d -> assertThat(d).isStrictlyBetween(0.0, 1.0));

        assertThatJson(overview).node("metrics").isArray().containsExactly(expectedMetrics);
        assertThatJson(overview).node("covered").isArray().containsExactlyElementsOf(expectedCovered);
        assertThatJson(overview).node("missed").isArray().containsExactlyElementsOf(expectedMissed);
    }

    @Test
    void shouldProvideIndirectCoverageChanges() {
        Node node = createIndirectCoverageChangesNode(Fraction.ZERO, LINE, 1, 1);

        CoverageViewModel model = new CoverageViewModel(mock(Run.class), node);

        assertThat(model.hasIndirectCoverageChanges()).isTrue();
    }

    @Test
    void shouldProvideRightTableModelById() {
        CoverageViewModel model = createModel();
        assertThat(model.getTableModel(CHANGE_COVERAGE_TABLE_ID)).isInstanceOf(ChangeCoverageTableModel.class);
        assertThat(model.getTableModel(INDIRECT_COVERAGE_TABLE_ID)).isInstanceOf(IndirectCoverageChangesTable.class);
        assertThat(model.getTableModel(ABSOLUTE_COVERAGE_TABLE_ID)).isInstanceOf(CoverageTableModel.class);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> model.getTableModel("wrong-id"));
    }

    private CoverageViewModel createModel() {
        return new CoverageViewModel(mock(Run.class), readJacocoResult("jacoco-codingstyle.xml"));
    }

}
