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
import io.jenkins.plugins.coverage.model.CoveragePercentage;

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
     * Creates a mock of {@link CoverageBuildAction} which provides all available coverages.
     *
     * @param coverageMetric
     *         The coverage metric
     * @param coverageValue
     *         The coverage value to be provided for all coverage types
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static CoverageBuildAction createCoverageBuildAction(
            final CoverageMetric coverageMetric, final Fraction coverageValue) {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        Coverage coverage = createCoverage(coverageValue);
        CoveragePercentage percentage = CoveragePercentage.valueOf(coverageValue);

        SortedMap<CoverageMetric, CoveragePercentage> deltas = mock(SortedMap.class);
        when(deltas.size()).thenReturn(1);
        when(deltas.containsKey(coverageMetric)).thenReturn(true);
        when(deltas.containsValue(percentage)).thenReturn(true);
        when(deltas.get(coverageMetric)).thenReturn(percentage);

        when(action.hasDelta(coverageMetric)).thenReturn(true);
        when(action.getDifference()).thenReturn(deltas);

        when(action.hasCoverage(coverageMetric)).thenReturn(true);
        when(action.getCoverage(coverageMetric)).thenReturn(coverage);

        when(action.hasChangeCoverage()).thenReturn(true);
        when(action.hasChangeCoverage(coverageMetric)).thenReturn(true);
        when(action.getChangeCoverage(coverageMetric)).thenReturn(coverage);

        when(action.hasIndirectCoverageChanges()).thenReturn(true);
        when(action.hasIndirectCoverageChanges(coverageMetric)).thenReturn(true);
        when(action.getIndirectCoverageChanges(coverageMetric)).thenReturn(coverage);

        when(action.hasChangeCoverageDifference(coverageMetric)).thenReturn(true);
        when(action.getChangeCoverageDifference(coverageMetric)).thenReturn(percentage);

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
        when(coverage.getCoveredFraction()).thenReturn(coverageFraction);
        when(coverage.getCoveredPercentage()).thenReturn(CoveragePercentage.valueOf(coverageFraction));
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
        when(coverageNode.getCoverage(coverageMetric)).thenReturn(coverage);
        return coverageNode;
    }

    /**
     * Creates a stub of {@link CoverageNode}, which represents the change coverage and provides information about it.
     *
     * @param changeCoverage
     *         The change coverage
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
    public static CoverageNode createChangeCoverageNode(final Fraction changeCoverage, final CoverageMetric metric,
            final int coverageFileChange, final long coverageLineChanges) {
        CoverageNode coverageNode = createCoverageNode(changeCoverage, metric);
        when(coverageNode.hasChangeCoverage()).thenReturn(true);
        when(coverageNode.hasChangeCoverage(metric)).thenReturn(true);
        when(coverageNode.getChangeCoverageTree()).thenReturn(coverageNode);
        when(coverageNode.hasCodeChanges()).thenReturn(true);
        when(coverageNode.getFileAmountWithChangedCoverage()).thenReturn(coverageFileChange);
        when(coverageNode.getLineAmountWithChangedCoverage()).thenReturn(coverageLineChanges);
        return coverageNode;
    }

    /**
     * Creates a stub of {@link CoverageNode}, which represents indirect coverage changes and provides information about
     * it.
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
    public static CoverageNode createIndirectCoverageChangesNode(final Fraction coverageChanges,
            final CoverageMetric metric, final int coverageFileChange, final long coverageLineChanges) {
        CoverageNode coverageNode = createCoverageNode(coverageChanges, metric);
        when(coverageNode.hasIndirectCoverageChanges()).thenReturn(true);
        when(coverageNode.hasIndirectCoverageChanges(metric)).thenReturn(true);
        when(coverageNode.getIndirectCoverageChangesTree()).thenReturn(coverageNode);
        when(coverageNode.getFileAmountWithIndirectCoverageChanges()).thenReturn(coverageFileChange);
        when(coverageNode.getLineAmountWithIndirectCoverageChanges()).thenReturn(coverageLineChanges);
        return coverageNode;
    }
}
