package io.jenkins.plugins.coverage.model.testutil;

import java.util.SortedMap;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.util.VisibleForTesting;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;

import static org.mockito.Mockito.*;

/**
 * Provides some factory methods to create stubs of different possible coverage results.
 *
 * @author Ullrich Hafner
 * @author Florian Orendi
 */
public final class CoverageStubs {
    private CoverageStubs() {
        // prevents instantiation
    }

    /**
     * Creates a new build result for the given build.
     *
     * @param buildNumber
     *         the number of the build
     * @param lineCoverage
     *         the line coverage in the build
     * @param branchCoverage
     *         the branch coverage in the build
     *
     * @return the {@link BuildResult} stub, that contains a {@link CoverageBuildAction} instance with the specified
     *         behavior
     */
    @VisibleForTesting
    public static BuildResult<CoverageBuildAction> createResult(final int buildNumber,
            final Coverage lineCoverage, final Coverage branchCoverage) {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        when(action.getLineCoverage()).thenReturn(lineCoverage);
        when(action.getBranchCoverage()).thenReturn(branchCoverage);

        Build build = new Build(buildNumber);

        return new BuildResult<>(build, action);
    }

    /**
     * Creates a stub of {@link CoverageBuildAction}, which provides the project coverage percentage and delta for the
     * passed metric.
     *
     * @param coverageMetric
     *         The coverage metric
     * @param coverageDelta
     *         The project coverage delta
     * @param coverageFraction
     *         The project coverage fraction
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static CoverageBuildAction createCoverageBuildAction(
            final CoverageMetric coverageMetric,
            final Fraction coverageDelta,
            final Fraction coverageFraction) {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        Coverage coverage = createCoverage(coverageFraction);

        SortedMap<CoverageMetric, Fraction> deltas = mock(SortedMap.class);
        when(deltas.size()).thenReturn(1);
        when(deltas.containsKey(coverageMetric)).thenReturn(true);
        when(deltas.containsValue(coverageDelta)).thenReturn(true);
        when(deltas.get(coverageMetric)).thenReturn(coverageDelta);

        when(action.hasDelta(coverageMetric)).thenReturn(true);
        when(action.hasCoverage(coverageMetric)).thenReturn(true);
        when(action.getDifference()).thenReturn(deltas);
        when(action.getCoverage(coverageMetric)).thenReturn(coverage);

        return action;
    }

    /**
     * Creates a stub of {@link Coverage}, which provides the passed coverage percentage.
     *
     * @param coverageFraction
     *         The coverage fraction
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Coverage createCoverage(final Fraction coverageFraction) {
        Coverage coverage = mock(Coverage.class);
        when(coverage.getCoveredPercentage()).thenReturn(coverageFraction);
        when(coverage.isSet()).thenReturn(true);
        return coverage;
    }

    /**
     * Creates a stub of {@link CoverageNode}, which provides the passed coverage percentage for the passed metric.
     *
     * @param coverageFraction
     *         The coverage fraction
     * @param coverageMetric
     *         The coverage metric
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static CoverageNode createCoverageNode(final Fraction coverageFraction,
            final CoverageMetric coverageMetric) {
        CoverageNode coverageNode = mock(CoverageNode.class);
        Coverage coverage = createCoverage(coverageFraction);
        when(coverage.getCoveredPercentage()).thenReturn(coverageFraction);
        when(coverageNode.getCoverage(coverageMetric)).thenReturn(coverage);
        return coverageNode;
    }
}
