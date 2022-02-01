package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import hudson.model.Job;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageType;
import io.jenkins.plugins.coverage.model.util.WebUtil;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;

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
    private static final CoverageType COVERAGE_TYPE = CoverageType.PROJECT;
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
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtil.getRelativeCoverageDefaultUrl());
        final Optional<BigDecimal> coverageValue = column.getCoverageValue(job);
        assertThat(coverageValue).isEmpty();
        assertThat(column.getFillColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_FILL_COLOR));
        assertThat(column.getLineColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_LINE_COLOR));
    }

    @Test
    void shouldShowNoResultIfNoAction() {
        CodeCoverageColumn column = createColumn();

        Job<?, ?> job = createJobWithActions();

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtil.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getFillColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_FILL_COLOR));
        assertThat(column.getLineColor(job, null)).isEqualTo(ColorUtils.colorAsHex(ColorUtils.NA_LINE_COLOR));
    }

    @Test
    void shouldShowNoResultForUnknownCoverageType() {
        CodeCoverageColumn column = createColumn();
        column.setCoverageType(CoverageType.UNDEFINED.getType());

        Job<?, ?> job = createJobWithCoverageAction(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo("");
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getFillColor(job, BigDecimal.ZERO)).isEqualTo(
                ColorUtils.colorAsHex(ColorUtils.NA_FILL_COLOR));
        assertThat(column.getLineColor(job, BigDecimal.ZERO)).isEqualTo(
                ColorUtils.colorAsHex(ColorUtils.NA_LINE_COLOR));
    }

    @Test
    void shouldShowNoResultForUnavailableCoverageMetric() {
        CodeCoverageColumn column = createColumn();
        column.setCoverageMetric(CoverageMetric.CLASS.getName());

        Job<?, ?> job = createJobWithCoverageAction(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtil.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job)).isEmpty();

        column.setCoverageType(CoverageType.PROJECT_DELTA.getType());

        assertThat(column.getCoverageText(job)).isEqualTo(CodeCoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtil.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job)).isEmpty();
    }

    @Test
    void shouldCalculateProjectCoverage() {
        CodeCoverageColumn column = createColumn();

        BigDecimal coveragePercentage = createScaledBigDecimal("0.5");
        Job<?, ?> job = createJobWithCoverageAction(BigDecimal.ZERO, coveragePercentage);

        assertThat(column.getCoverageText(job)).isEqualTo("50 %");
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtil.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(value -> {
                    final BigDecimal coverage = value.get();
                    assertThat(coverage.compareTo(coveragePercentage.multiply(new BigDecimal(100)))).isEqualTo(0);
                    assertThat(column.getFillColor(job, coverage))
                            .isEqualTo(ColorUtils.colorAsHex(CoverageLevel.OVER_50.getFillColor()));
                    assertThat(column.getLineColor(job, coverage))
                            .isEqualTo(ColorUtils.colorAsHex(CoverageLevel.OVER_50.getLineColor()));
                });
    }

    @Test
    void shouldCalculateProjectCoverageDelta() {
        CodeCoverageColumn column = createColumn();
        column.setCoverageType(CoverageType.PROJECT_DELTA.getType());

        BigDecimal coverageDelta = createScaledBigDecimal("5.0");
        Job<?, ?> job = createJobWithCoverageAction(coverageDelta, BigDecimal.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo("5 %");
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtil.getRelativeCoverageDefaultUrl());
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(value -> {
                    final BigDecimal coverage = value.get();
                    assertThat(coverage.compareTo(coverageDelta)).isEqualTo(0);
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
    private Job<?, ?> createJobWithCoverageAction(final BigDecimal coverageDelta, final BigDecimal coveragePercentage) {
        CoverageBuildAction coverageBuildAction =
                createCoverageBuildAction(COVERAGE_METRIC, coverageDelta.doubleValue(),
                        coveragePercentage.doubleValue());
        return createJobWithActions(coverageBuildAction);
    }

    /**
     * Creates a {@link BigDecimal} which contains the passed value and uses the format which is used for displaying
     * values within the code coverage column.
     *
     * @param value
     *         The value to be wrapped
     *
     * @return the created {@link BigDecimal}
     */
    private BigDecimal createScaledBigDecimal(final String value) {
        return new BigDecimal(value)
                .setScale(3, RoundingMode.DOWN)
                .stripTrailingZeros();
    }
}
