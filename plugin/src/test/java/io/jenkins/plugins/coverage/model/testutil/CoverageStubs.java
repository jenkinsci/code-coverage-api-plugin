package io.jenkins.plugins.coverage.model.testutil;

import java.util.NavigableMap;
import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.VisibleForTesting;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoveragePercentage;

import static io.jenkins.plugins.coverage.model.CoverageBuildAction.*;
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
            final Metric coverageMetric, final Fraction coverageValue) {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        Coverage coverage = createCoverage(coverageValue);
        CoveragePercentage percentage = CoveragePercentage.valueOf(coverageValue);

        NavigableMap<Metric, Fraction> deltas = mock(NavigableMap.class);
        when(deltas.size()).thenReturn(1);
        when(deltas.containsKey(coverageMetric)).thenReturn(true);
        when(deltas.containsValue(coverageValue)).thenReturn(true);
        when(deltas.get(coverageMetric)).thenReturn(coverageValue);

        when(action.hasDelta(coverageMetric)).thenReturn(true);
        when(action.getDelta()).thenReturn(deltas);

        when(action.hasCoverage(coverageMetric)).thenReturn(true);
        when(action.getCoverage(coverageMetric)).thenReturn(coverage);

        when(action.hasChangeCoverage()).thenReturn(true);
        when(action.hasChangeCoverage(coverageMetric)).thenReturn(true);
        when(action.getChangeCoverage(coverageMetric)).thenReturn(coverage);

        when(action.hasIndirectCoverageChanges()).thenReturn(true);
        when(action.hasIndirectCoverageChanges(coverageMetric)).thenReturn(true);
        when(action.getIndirectCoverageChanges(coverageMetric)).thenReturn(coverage);

        when(action.hasChangeCoverageDifference(coverageMetric)).thenReturn(true);
        when(action.getChangeCoverageDifference(coverageMetric)).thenReturn(coverageValue);

        when(action.getUrlName()).thenReturn(DETAILS_URL);

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
     * Creates a stub of {@link Node}, which provides the passed coverage percentage for the passed metric.
     *
     * @param coverageFraction
     *         The coverage fraction
     * @param coverageMetric
     *         The coverage metric
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static FileNode createCoverageNode(final Fraction coverageFraction, final Metric coverageMetric) {
        FileNode coverageNode = mock(FileNode.class);
        Coverage coverage = createCoverage(coverageFraction);
        when(coverageNode.getValue(coverageMetric)).thenReturn(Optional.of(coverage));
        return coverageNode;
    }

    /**
     * Creates a stub of {@link Node}, which represents the change coverage and provides information about it.
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
    public static Node createChangeCoverageNode(final Fraction changeCoverage, final Metric metric,
            final int coverageFileChange, final long coverageLineChanges) {
        var root = new ModuleNode("root");
        var builder = new CoverageBuilder().setMetric(Metric.LINE);
        for (int file = 0; file < 5; file++) {
            var fileNode = new FileNode("File-" + file);

            for (int line = 0; line < 2; line++) {
                fileNode.addCounters(10 + line, 1, 1);
                fileNode.addChangedCodeLine(10 + line);
            }
            root.addChild(fileNode);
        }

        return root;
    }

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
}
