package io.jenkins.plugins.coverage.model;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageJobAction}.
 *
 * @author Ullrich Hafner
 */
class CoverageJobActionTest {

    @Test
    void shouldIgnoreIndexIfNoActionFound() throws IOException {
        FreeStyleProject job = mock(FreeStyleProject.class);

        CoverageJobAction action = new CoverageJobAction(job);

        assertThat(action.getProject()).isSameAs(job);

        StaplerResponse response = mock(StaplerResponse.class);
        action.doIndex(mock(StaplerRequest.class), response);

        verifyNoInteractions(response);
    }

    @Test
    void shouldNavigateToLastAction() throws IOException {
        FreeStyleProject job = mock(FreeStyleProject.class);
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        when(action.getOwner()).thenAnswer(i -> build);
        when(action.getUrlName()).thenReturn("coverage");

        when(build.getAction(CoverageBuildAction.class)).thenReturn(action);
        when(build.getNumber()).thenReturn(15);
        when(job.getLastBuild()).thenReturn(build);

        CoverageJobAction jobAction = new CoverageJobAction(job);

        assertThat(jobAction.getProject()).isSameAs(job);

        StaplerResponse response = mock(StaplerResponse.class);
        jobAction.doIndex(mock(StaplerRequest.class), response);

        verify(response).sendRedirect2("../15/coverage");
    }
}
