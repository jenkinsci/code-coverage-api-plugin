package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.Functions;
import hudson.model.Job;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageTypeTmp;
import io.jenkins.plugins.coverage.model.util.FractionFormatter;
import io.jenkins.plugins.coverage.model.util.WebUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageColorizationLevel;

import static io.jenkins.plugins.coverage.model.testutil.CoverageStubs.*;
import static io.jenkins.plugins.coverage.model.testutil.JobStubs.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CodeCoverageColumn}.
 *
 * @author Florian Orendi
 */
class CodeCoverageColumnTest {

    private static final String COLUMN_NAME = "Test Column";
    private static final CoverageTypeTmp COVERAGE_TYPE = CoverageTypeTmp.PROJECT;
    private static final CoverageMetric COVERAGE_METRIC = CoverageMetric.BRANCH;

    @Test
    void shouldHaveWorkingDataGetters() {
        CodeCoverageColumn column = createColumn();
        assertThat(column.getColumnName()).isEqualTo(COLUMN_NAME);
        assertThat(column.getCoverageType()).isEqualTo(COVERAGE_TYPE.getType());
        assertThat(column.getCoverageMetric()).isEqualTo(COVERAGE_METRIC.getName());
    }

    @Test
    void shouldShowNoResultIfBuild() {
        CodeCoverageColumn column = createColumn();

        Job<?, ?> job = createJob();

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.getRelativeCoverageDefaultUrl());
        final Optional<Fraction> coverageValue = column.getCoverageValue(job);
        assertThat(coverageValue).isEmpty();
        assertThat(column.getFillColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_FILL_COLOR));
        assertThat(column.getLineColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_LINE_COLOR));
    }

    @Test
    void shouldShowNoResultIfNoAction() {
        CodeCoverageColumn column = createColumn();

        Job<?, ?> job = createJobWithActions();

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getFillColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_FILL_COLOR));
        assertThat(column.getLineColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_LINE_COLOR));
    }

    @Test
    void shouldShowNoResultForUnknownCoverageType() {
        CodeCoverageColumn column = createColumn();
        column.setCoverageType(CoverageTypeTmp.UNDEFINED.getType());

        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO, Fraction.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo("");
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getFillColor(job, Fraction.ZERO)).isEqualTo(
                ColorUtils.colorAsHex(ColorUtils.NA_FILL_COLOR));
        assertThat(column.getLineColor(job, Fraction.ZERO)).isEqualTo(
                ColorUtils.colorAsHex(ColorUtils.NA_LINE_COLOR));
    }

    @Test
    void shouldShowNoResultForUnavailableCoverageMetric() {
        CodeCoverageColumn column = createColumn();
        column.setCoverageMetric(CoverageMetric.CLASS.getName());

        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO, Fraction.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job)).isEmpty();

        column.setCoverageType(CoverageTypeTmp.PROJECT_DELTA.getType());

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job)).isEmpty();
    }

    @Test
    void shouldCalculateProjectCoverage() {
        CodeCoverageColumn column = createColumn();

        Fraction coverageFraction = Fraction.getFraction(1, 2);
        Fraction coveragePercentage = FractionFormatter.transformFractionToPercentage(coverageFraction);
        String coveragePercentageText =
                FractionFormatter.formatPercentage(coveragePercentage, Functions.getCurrentLocale());

        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO, coverageFraction);

        assertThat(column.getCoverageText(job)).isEqualTo(coveragePercentageText);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(value -> {
                    final Fraction coverage = value.get();
                    assertThat(coverage).isEqualTo(coveragePercentage);
                    assertThat(column.getFillColor(job, coverage))
                            .isEqualTo(
                                    ColorUtils.colorAsHex(CoverageColorizationLevel.LVL_50_DECREASE5.getFillColor()));
                    assertThat(column.getLineColor(job, coverage))
                            .isEqualTo(
                                    ColorUtils.colorAsHex(CoverageColorizationLevel.LVL_50_DECREASE5.getLineColor()));
                });
    }

    @Test
    void shouldCalculateProjectCoverageDelta() {
        CodeCoverageColumn column = createColumn();
        column.setCoverageType(CoverageTypeTmp.PROJECT_DELTA.getType());

        Fraction coverageDelta = Fraction.getFraction(1, 20);
        Fraction coverageDeltaPercentage = FractionFormatter.transformFractionToPercentage(coverageDelta);
        String coverageDeltaPercentageText =
                FractionFormatter.formatDeltaPercentage(coverageDeltaPercentage, Functions.getCurrentLocale());
        Job<?, ?> job = createJobWithCoverageAction(coverageDelta, Fraction.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(coverageDeltaPercentageText);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(value -> {
                    final Fraction coverage = value.get();
                    assertThat(coverage).isEqualTo(coverageDeltaPercentage);
                    assertThat(column.getFillColor(job, coverage))
                            .isEqualTo(ColorUtils.colorAsHex(CoverageChangeTendency.INCREASED.getFillColor()));
                    assertThat(column.getLineColor(job, coverage))
                            .isEqualTo(ColorUtils.colorAsHex(CoverageChangeTendency.INCREASED.getLineColor()));
                });
    }

    /**
     * Creates a {@link CodeCoverageColumn}.
     *
     * @return the created column.
     */
    private CodeCoverageColumn createColumn() {
        CodeCoverageColumn column = new CodeCoverageColumn();
        column.setColumnName(COLUMN_NAME);
        column.setCoverageType(COVERAGE_TYPE.getType());
        column.setCoverageMetric(COVERAGE_METRIC.getName());
        return column;
    }

    /**
     * Creates a mock of {@link CoverageBuildAction} which provides the passed project coverage delta and percentage.
     *
     * @param coverageDelta
     *         The project coverage delta
     * @param coveragePercentage
     *         The project coverage percentage
     *
     * @return the created mock
     */
    private Job<?, ?> createJobWithCoverageAction(final Fraction coverageDelta, final Fraction coveragePercentage) {
        CoverageBuildAction coverageBuildAction =
                createCoverageBuildAction(COVERAGE_METRIC, coverageDelta, coveragePercentage);
        return createJobWithActions(coverageBuildAction);
    }
}
