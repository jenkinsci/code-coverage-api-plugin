package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.util.VisibleForTesting;

import static org.mockito.Mockito.*;

/**
 * Provides some factory methods to create stubs of {@link BuildResult build results}.
 *
 * @author Ullrich Hafner
 */
public final class ResultStubs {
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

    private ResultStubs() {
        // prevents instantiation
    }
}
