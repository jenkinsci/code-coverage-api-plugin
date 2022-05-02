package io.jenkins.plugins.coverage.model;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.Functions;
import hudson.model.HealthReport;
import hudson.model.Run;

import static io.jenkins.plugins.coverage.model.testutil.CoverageStubs.*;
import static io.jenkins.plugins.coverage.model.testutil.JobStubs.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageBuildAction}.
 *
 * @author Ullrich Hafner
 */
class CoverageBuildActionTest {

    private static final Locale LOCALE = Functions.getCurrentLocale();

    private static final Fraction COVERAGE_FRACTION = Fraction.ONE_HALF;
    private static final CoveragePercentage COVERAGE_PERCENTAGE =
            CoveragePercentage.getCoveragePercentage(COVERAGE_FRACTION);
    private static final CoverageMetric COVERAGE_METRIC = CoverageMetric.LINE;

    private static final int COVERAGE_FILE_CHANGES = 5;
    private static final long COVERAGE_LINE_CHANGES = 10;

    @Test
    void shouldCreateViewModel() {
        Run<?, ?> build = mock(Run.class);
        CoverageNode root = new CoverageNode(COVERAGE_METRIC, "top-level");
        SortedMap<CoverageMetric, CoveragePercentage> metrics = new TreeMap<>();
        SortedMap<CoverageMetric, CoveragePercentage> changeCoverage = new TreeMap<>();
        SortedMap<CoverageMetric, CoveragePercentage> changeCoverageDelta = new TreeMap<>();
        SortedMap<CoverageMetric, CoveragePercentage> indirectCoverageChanges = new TreeMap<>();

        CoverageBuildAction action =
                new CoverageBuildAction(build, root, new HealthReport(), "-", metrics, changeCoverage,
                        changeCoverageDelta, indirectCoverageChanges, false);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getNode).isEqualTo(root);
        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);
    }

    @Test
    void shouldGetCoverageForSpecifiedMetric() {
        CoverageBuildAction action = createCoverageBuildActionWithMocks();
        assertThat(action.hasCoverage(COVERAGE_METRIC)).isTrue();
        assertThat(action.getCoverage(COVERAGE_METRIC))
                .isNotNull()
                .satisfies(coverage -> assertThat(coverage.getCoveredPercentage()).isEqualTo(COVERAGE_PERCENTAGE));
    }

    @Test
    void shouldGetCoverageDifferenceForSpecifiedMetric() {
        CoverageBuildAction action = createCoverageBuildActionWithMocks();
        assertThat(action.hasDelta(COVERAGE_METRIC)).isTrue();
        assertThat(action.hasDelta(CoverageMetric.BRANCH)).isFalse();
        assertThat(action.getDifference())
                .hasSize(1)
                .containsKey(COVERAGE_METRIC)
                .containsValue(COVERAGE_PERCENTAGE);
        assertThat(action.formatDelta(COVERAGE_METRIC))
                .isEqualTo(COVERAGE_PERCENTAGE.formatDeltaPercentage(LOCALE));
    }

    @Test
    void shouldGetChangeCoverageDifferences() {
        CoverageBuildAction action = createCoverageBuildActionWithMocks();
        assertThat(action.hasChangeCoverageDifference(COVERAGE_METRIC)).isTrue();
        assertThat(action.getChangeCoverageDifference(COVERAGE_METRIC))
                .isNotNull()
                .satisfies(coverage -> assertThat(coverage).isEqualTo(COVERAGE_PERCENTAGE));
    }

    @Test
    void shouldGetChangeCoverageForSpecifiedMetric() {
        CoverageBuildAction action = createChangeCoverageBuildActionWithMocks();
        assertThat(action.hasChangeCoverage()).isTrue();
        assertThat(action.hasChangeCoverage(COVERAGE_METRIC)).isTrue();
        assertThat(action.getChangeCoverage(COVERAGE_METRIC))
                .isNotNull()
                .satisfies(coverage -> assertThat(coverage.getCoveredPercentage()).isEqualTo(COVERAGE_PERCENTAGE));
    }

    @Test
    void shouldGetIndirectCoverageChangesForSpecifiedMetric() {
        CoverageBuildAction action = createIndirectCoverageChangesBuildActionWithMocks();
        assertThat(action.hasIndirectCoverageChanges()).isTrue();
        assertThat(action.hasIndirectCoverageChanges(COVERAGE_METRIC)).isTrue();
        assertThat(action.getIndirectCoverageChanges(COVERAGE_METRIC))
                .isNotNull()
                .satisfies(coverage -> assertThat(coverage.getCoveredPercentage()).isEqualTo(COVERAGE_PERCENTAGE));
    }

    @Test
    void shouldFormatChangeCoverage() {
        CoverageBuildAction action = createChangeCoverageBuildActionWithMocks();
        assertThat(action.formatChangeCoverage(COVERAGE_METRIC)).isEqualTo(getFormattedLineCoverage());
        assertThat(action.formatChangeCoverageOverview()).isEqualTo(getFormattedLineCoverageOverview());
    }

    @Test
    void shouldFormatIndirectCoverageChanges() {
        CoverageBuildAction action = createIndirectCoverageChangesBuildActionWithMocks();
        assertThat(action.formatIndirectCoverageChanges(COVERAGE_METRIC)).isEqualTo(getFormattedLineCoverage());
        assertThat(action.formatIndirectCoverageChangesOverview()).isEqualTo(getFormattedLineCoverageOverview());
    }

    @Test
    void shouldFormatChangeCoverageDifference() {
        CoverageBuildAction action = createChangeCoverageBuildActionWithMocks();
        String expected = COVERAGE_PERCENTAGE.formatDeltaPercentage(LOCALE);
        assertThat(action.formatChangeCoverageDifference(COVERAGE_METRIC)).isEqualTo(expected);
    }

    @Test
    void shouldFormatNotAvailableCoverageValues() {
        CoverageNode root = createCoverageNode(COVERAGE_FRACTION, CoverageMetric.BRANCH);
        when(root.hasChangeCoverage()).thenReturn(false);
        when(root.hasIndirectCoverageChanges()).thenReturn(false);

        CoverageBuildAction action = createCoverageBuildAction(root);

        assertThat(action.formatChangeCoverage(CoverageMetric.BRANCH)).isEqualTo("Branch: n/a");
        assertThat(action.formatChangeCoverageOverview()).isEqualTo("n/a");

        assertThat(action.formatIndirectCoverageChanges(CoverageMetric.BRANCH)).isEqualTo("Branch: n/a");
        assertThat(action.formatIndirectCoverageChangesOverview()).isEqualTo("n/a");

        assertThat(action.formatChangeCoverageDifference(CoverageMetric.BRANCH)).isEqualTo("n/a");
        assertThat(action.formatDelta(CoverageMetric.BRANCH)).isEqualTo("n/a");
    }

    /**
     * Creates a {@link CoverageBuildAction} which represents the coverage for the metric {@link #COVERAGE_METRIC} with
     * the value {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the created action
     */
    private CoverageBuildAction createCoverageBuildActionWithMocks() {
        CoverageNode root = createCoverageNode(COVERAGE_FRACTION, COVERAGE_METRIC);
        return createCoverageBuildAction(root);
    }

    /**
     * Creates a {@link CoverageBuildAction} which represents the change coverage for the metric {@link
     * #COVERAGE_METRIC} with the value {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the created action
     */
    private CoverageBuildAction createChangeCoverageBuildActionWithMocks() {
        CoverageNode root = createChangeCoverageNode(COVERAGE_FRACTION, COVERAGE_METRIC,
                COVERAGE_FILE_CHANGES, COVERAGE_LINE_CHANGES);
        return createCoverageBuildAction(root);
    }

    /**
     * Creates a {@link CoverageBuildAction} which represents the indirect coverage changes for the metric {@link
     * #COVERAGE_METRIC} with the value {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the created action
     */
    private CoverageBuildAction createIndirectCoverageChangesBuildActionWithMocks() {
        CoverageNode root = createIndirectCoverageChangesNode(COVERAGE_FRACTION, COVERAGE_METRIC,
                COVERAGE_FILE_CHANGES, COVERAGE_LINE_CHANGES);
        return createCoverageBuildAction(root);
    }

    /**
     * Creates a {@link CoverageBuildAction} with the passed {@link CoverageNode result}.
     *
     * @param root
     *         The result of the action
     *
     * @return the created action
     */
    private CoverageBuildAction createCoverageBuildAction(final CoverageNode root) {
        Run<?, ?> build = createBuild();
        HealthReport healthReport = mock(HealthReport.class);

        TreeMap<CoverageMetric, CoveragePercentage> deltas = new TreeMap<>();
        deltas.put(COVERAGE_METRIC, COVERAGE_PERCENTAGE);
        TreeMap<CoverageMetric, CoveragePercentage> changeCoverage = new TreeMap<>();
        changeCoverage.put(COVERAGE_METRIC, COVERAGE_PERCENTAGE);
        TreeMap<CoverageMetric, CoveragePercentage> changeCoverageDifference = new TreeMap<>();
        changeCoverageDifference.put(COVERAGE_METRIC, COVERAGE_PERCENTAGE);
        TreeMap<CoverageMetric, CoveragePercentage> indirectCoverageChanges = new TreeMap<>();
        indirectCoverageChanges.put(COVERAGE_METRIC, COVERAGE_PERCENTAGE);

        return new CoverageBuildAction(build, root, healthReport, "-", deltas,
                changeCoverage, changeCoverageDifference, indirectCoverageChanges, false);
    }

    /**
     * Gets a formatted text representation of the line coverage {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the formatted text
     */
    private String getFormattedLineCoverage() {
        return "Line: " + COVERAGE_PERCENTAGE.formatPercentage(LOCALE);
    }

    /**
     * Gets a formatted text representation of the line coverage overview of {@link #COVERAGE_LINE_CHANGES} and {@link
     * #COVERAGE_FILE_CHANGES}.
     *
     * @return the formatted text
     */
    private String getFormattedLineCoverageOverview() {
        return COVERAGE_LINE_CHANGES + " lines (" + COVERAGE_FILE_CHANGES + " files) are affected";
    }
}
