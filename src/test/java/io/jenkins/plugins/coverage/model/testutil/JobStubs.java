package io.jenkins.plugins.coverage.model.testutil;

import java.util.Arrays;

import edu.hm.hafner.util.VisibleForTesting;

import hudson.model.Build;
import hudson.model.Job;
import hudson.model.Run;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;

import static org.mockito.Mockito.*;

/**
 * Provides stubs of Jenkins {@link Job} and {@link Build}.
 *
 * @author Florian Orendi
 */
public final class JobStubs {

    private JobStubs() {
        // prevents instantiation
    }

    /**
     * Creates a stub of {@link Job}.
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Job<?, ?> createJob() {
        return mock(Job.class);
    }

    /**
     * Creates a stub for a {@link Job} that has the specified actions attached.
     *
     * @param actions
     *         The actions to attach, might be empty
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Job<?, ?> createJobWithActions(final CoverageBuildAction... actions) {
        Job job = createJob();
        Run<?, ?> build = createBuildWithActions(actions);
        when(job.getLastCompletedBuild()).thenReturn(build);
        return job;
    }

    @VisibleForTesting
    public static Run<?, ?> createBuild() {
        return mock(Run.class);
    }

    /**
     * Creates a stub for a {@link Run} that has the specified actions attached.
     *
     * @param actions
     *         the actions to attach, might be empty
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Run<?, ?> createBuildWithActions(final CoverageBuildAction... actions) {
        Run<?, ?> build = createBuild();
        when(build.getActions(CoverageBuildAction.class)).thenReturn(Arrays.asList(actions));
        if (actions.length > 0) {
            when(build.getAction(CoverageBuildAction.class)).thenReturn(actions[0]);
        }
        return build;
    }
}
