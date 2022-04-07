package io.jenkins.plugins.coverage.model;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.model.Run;

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
        assertThat(model.getSourceCode(hash, "coverage-table")).isEqualTo("n/a");
        assertThat(model.getSourceCode(hash, "change-coverage-table")).isEqualTo("n/a");
        assertThat(model.getSourceCode(hash, "coverage-changes-table")).isEqualTo("n/a");
    }

    @Test
    void shouldReportOverview() {
        CoverageViewModel model = createModel();

        assertThat(model.getDisplayName()).contains("Java coding style: jacoco-codingstyle.xml");

        CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "Package", "File", "Class", "Method", "Line", "Instruction", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                1, 7, 15, 97, 294, 1260, 109
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 3, 3, 5, 29, 90, 7
        );
    }

    @Test
    void shouldProvideChangeCoverage() {
        CoverageNode node = createChangeCoverageNode(Fraction.ZERO, LINE, 1, 1);
        CoverageViewModel model = createModelFromMock(node);
        assertThat(model.hasChangeCoverage()).isTrue();

        CoverageOverview overview = model.getChangeCoverageOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "File", "Line", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                0, 0, 0
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 0, 0
        );
    }

    @Test
    void shouldProvideIndirectCoverageChanges() {
        CoverageNode node = createIndirectCoverageChangesNode(Fraction.ZERO, LINE, 1, 1);
        CoverageViewModel model = createModelFromMock(node);
        assertThat(model.hasIndirectCoverageChanges()).isTrue();
    }

    @Test
    void shouldProvideRightTableModelById() {
        CoverageViewModel model = createModel();
        assertThat(model.getTableModel(CHANGE_COVERAGE_TABLE_ID)).isInstanceOf(ChangeCoverageTable.class);
        assertThat(model.getTableModel(COVERAGE_CHANGES_TABLE_ID)).isInstanceOf(CoverageTableModel.class);
        assertThat(model.getTableModel("")).isInstanceOf(CoverageTableModel.class);
    }

    private CoverageViewModel createModel() {
        return new CoverageViewModel(mock(Run.class),
                readNode("jacoco-codingstyle.xml"));
    }

    /**
     * Creates a {@link CoverageViewModel} which represents a mocked {@link CoverageNode}.
     *
     * @param mock
     *         The mocked node
     *
     * @return the created model
     */
    private CoverageViewModel createModelFromMock(final CoverageNode mock) {
        when(mock.filterPackageStructure()).thenReturn(mock);

        SortedMap<CoverageMetric, Coverage> changeMetricsDistribution = new TreeMap<>();
        changeMetricsDistribution.put(LINE, Coverage.NO_COVERAGE);
        changeMetricsDistribution.put(BRANCH, Coverage.NO_COVERAGE);
        changeMetricsDistribution.put(FILE, Coverage.NO_COVERAGE);
        changeMetricsDistribution.put(PACKAGE, Coverage.NO_COVERAGE);
        when(mock.getMetricsDistribution()).thenReturn(changeMetricsDistribution);

        return new CoverageViewModel(mock(Run.class), mock);
    }
}
