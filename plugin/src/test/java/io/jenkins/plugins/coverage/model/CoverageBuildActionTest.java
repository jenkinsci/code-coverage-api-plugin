package io.jenkins.plugins.coverage.model;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.HealthReport;
import hudson.model.Run;

import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;

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
    private static final CoveragePercentage COVERAGE_PERCENTAGE = CoveragePercentage.valueOf(COVERAGE_FRACTION);
    private static final CoverageMetric COVERAGE_METRIC = CoverageMetric.LINE;

    private static final int COVERAGE_FILE_CHANGES = 5;
    private static final long COVERAGE_LINE_CHANGES = 10;

    @Test
    void shouldNotLoadResultIfCoverageValuesArePersistedInAction() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "module");

        CoverageBuilder coverageBuilder = new CoverageBuilder();

        Coverage percent50 = coverageBuilder.setCovered(1).setMissed(1).build();
        module.add(new CoverageLeaf(CoverageMetric.BRANCH, percent50));

        Coverage percent80 = coverageBuilder.setCovered(8).setMissed(2).build();
        module.add(new CoverageLeaf(CoverageMetric.LINE, percent80));

        CoverageBuildAction action = spy(createEmptyAction(module));
        when(action.getResult()).thenThrow(new IllegalStateException("Result should not be accessed with getResult() when getting a coverage metric that is persisted in the build"));

        assertThat(action.hasCoverage(CoverageMetric.LINE)).isTrue();
        assertThat(action.getCoverage(CoverageMetric.LINE)).isEqualTo(percent80);
        assertThat(action.hasCoverage(CoverageMetric.BRANCH)).isTrue();
        assertThat(action.getCoverage(CoverageMetric.BRANCH)).isEqualTo(percent50);

        assertThatIllegalStateException().isThrownBy(
                () -> action.hasCoverage(CoverageMetric.INSTRUCTION));
        assertThatIllegalStateException().isThrownBy(
                () -> action.getCoverage(CoverageMetric.INSTRUCTION));

        assertThat(action.formatChangeCoverage(CoverageMetric.BRANCH)).isEqualTo("Branch: n/a");
        assertThat(action.formatChangeCoverageOverview()).isEqualTo("n/a");

        assertThat(action.formatIndirectCoverageChanges(CoverageMetric.BRANCH)).isEqualTo("Branch: n/a");
        assertThat(action.formatIndirectCoverageChangesOverview()).isEqualTo("n/a");

        assertThat(action.formatChangeCoverageDifference(CoverageMetric.BRANCH)).isEqualTo("n/a");
        assertThat(action.formatDelta(CoverageMetric.BRANCH)).isEqualTo("n/a");
    }

    private static CoverageBuildAction createEmptyAction(final CoverageNode module) {
        return new CoverageBuildAction(mock(FreeStyleBuild.class), module, mock(HealthReport.class), "-",
                new TreeMap<>(), new TreeMap<>(),
                new TreeMap<>(), new TreeMap<>(), false);
    }

    @Test
    void shouldNotLoadResultIfDeltasArePersistedInAction() {
        SortedMap<CoverageMetric, CoveragePercentage> deltas = new TreeMap<>();

        CoverageBuilder coverageBuilder = new CoverageBuilder();

        CoveragePercentage percent50 = CoveragePercentage.valueOf(coverageBuilder.setCovered(1).setMissed(1).build()
                .getCoveredFraction());
        CoveragePercentage percent80 = CoveragePercentage.valueOf(coverageBuilder.setCovered(8).setMissed(2).build()
                .getCoveredFraction());

        deltas.put(CoverageMetric.BRANCH, percent50);
        deltas.put(CoverageMetric.LINE, percent80);

        CoverageBuildAction action = new CoverageBuildAction(mock(FreeStyleBuild.class),
                new CoverageNode(CoverageMetric.MODULE, "module"),
                mock(HealthReport.class), "-",
                deltas, deltas,
                deltas, deltas, false);

        CoverageBuildAction spy = spy(action);
        when(spy.getResult()).thenThrow(new IllegalArgumentException("Result should not be accessed with getResult() when getting a coverage metric that is persisted in the build"));

        assertThat(spy.hasChangeCoverage()).isTrue();
        assertThat(spy.hasChangeCoverage(CoverageMetric.LINE)).isTrue();
        assertThat(spy.hasChangeCoverage(CoverageMetric.BRANCH)).isTrue();
        // FIXME: those values are not persisted yet
//        assertThat(spy.getChangeCoverage(CoverageMetric.LINE)).isEqualTo(percent80);

        assertThat(spy.hasIndirectCoverageChanges()).isTrue();
        assertThat(spy.hasIndirectCoverageChanges(CoverageMetric.LINE)).isTrue();
        assertThat(spy.hasIndirectCoverageChanges(CoverageMetric.BRANCH)).isTrue();
        // FIXME: those values are not persisted yet
//        assertThat(spy.getIndirectCoverageChanges(CoverageMetric.LINE)).isEqualTo(percent80);

        assertThat(spy.hasChangeCoverageDifference(CoverageMetric.LINE)).isTrue();
        assertThat(spy.hasChangeCoverageDifference(CoverageMetric.BRANCH)).isTrue();
        // FIXME: those values are not persisted yet
//        assertThat(spy.getIndirectCoverageChanges(CoverageMetric.LINE)).isEqualTo(percent80);

        assertThat(spy.hasDelta(CoverageMetric.LINE)).isTrue();
        assertThat(spy.hasDelta(CoverageMetric.BRANCH)).isTrue();
        assertThat(spy.getDifference()).contains(entry(CoverageMetric.LINE, percent80));
        assertThat(spy.getDifference()).contains(entry(CoverageMetric.BRANCH, percent50));
    }

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
                .extracting(Coverage::getCoveredPercentage)
                .isEqualTo(COVERAGE_PERCENTAGE);
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
        assertThat(action.getChangeCoverageDifference(COVERAGE_METRIC)).isEqualTo(COVERAGE_PERCENTAGE);
    }

    @Test
    void shouldGetChangeCoverageForSpecifiedMetric() {
        CoverageBuildAction action = createChangeCoverageBuildActionWithMocks();
        assertThat(action.hasChangeCoverage()).isTrue();
        assertThat(action.hasCodeChanges()).isTrue();
        assertThat(action.hasChangeCoverage(COVERAGE_METRIC)).isTrue();
        assertThat(action.getChangeCoverage(COVERAGE_METRIC))
                .extracting(Coverage::getCoveredPercentage)
                .isEqualTo(COVERAGE_PERCENTAGE);
    }

    @Test
    void shouldGetIndirectCoverageChangesForSpecifiedMetric() {
        CoverageBuildAction action = createIndirectCoverageChangesBuildActionWithMocks();
        assertThat(action.hasIndirectCoverageChanges()).isTrue();
        assertThat(action.hasIndirectCoverageChanges(COVERAGE_METRIC)).isTrue();
        assertThat(action.getIndirectCoverageChanges(COVERAGE_METRIC))
                .extracting(Coverage::getCoveredPercentage)
                .isEqualTo(COVERAGE_PERCENTAGE);
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
