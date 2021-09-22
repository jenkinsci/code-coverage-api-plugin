package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link BuildResultNavigator}.
 *
 * @author Ullrich Hafner
 */
class BuildResultNavigatorTest {
    @Test
    void shouldNavigateToSelectedBuild() {
        BuildResultNavigator navigator = new BuildResultNavigator();

        FreeStyleBuild current = mock(FreeStyleBuild.class);
        FreeStyleProject job = mock(FreeStyleProject.class);
        when(current.getParent()).thenReturn(job);
        when(current.getNumber()).thenReturn(100);

        FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
        when(job.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.getDisplayName()).thenReturn("last-build");
        when(lastBuild.getNumber()).thenReturn(111);

        assertThat(navigator.getSameUrlForOtherBuild(current,
                "http://localhost:8080/job/pipeline-analysis-model/100/spotbugs/",
                "spotbugs",
                "last-build"))
                .isNotEmpty()
                .contains("http://localhost:8080/job/pipeline-analysis-model/111/spotbugs");
        assertThat(navigator.getSameUrlForOtherBuild(current,
                "http://localhost:8080/job/pipeline-analysis-model/different-url",
                "spotbugs",
                "last-build"))
                .isEmpty();

    }

    @Test
    void shouldNavigateToSameBuild() {
        BuildResultNavigator navigator = new BuildResultNavigator();

        FreeStyleBuild current = mock(FreeStyleBuild.class);
        FreeStyleProject job = mock(FreeStyleProject.class);
        when(current.getParent()).thenReturn(job);
        when(current.getNumber()).thenReturn(100);
        when(current.getDisplayName()).thenReturn("#100");

        FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
        when(job.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.getDisplayName()).thenReturn("#111");
        when(lastBuild.getNumber()).thenReturn(111);
        when(lastBuild.getPreviousBuild()).thenReturn(current);

        assertThat(navigator.getSameUrlForOtherBuild(current,
                "http://localhost:8080/job/pipeline-analysis-model/100/spotbugs/",
                "spotbugs",
                "#100"))
                .isNotEmpty()
                .contains("http://localhost:8080/job/pipeline-analysis-model/100/spotbugs");
        assertThat(navigator.getSameUrlForOtherBuild(current,
                "http://localhost:8080/job/pipeline-analysis-model/100/spotbugs/",
                "spotbugs",
                "#111"))
                .isNotEmpty()
                .contains("http://localhost:8080/job/pipeline-analysis-model/111/spotbugs");
    }

    @Test
    void shouldNotFindBuildForUrl() {
        BuildResultNavigator navigator = new BuildResultNavigator();

        FreeStyleBuild current = mock(FreeStyleBuild.class);
        FreeStyleProject job = mock(FreeStyleProject.class);
        when(current.getParent()).thenReturn(job);
        when(current.getNumber()).thenReturn(100);

        FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
        when(job.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.getDisplayName()).thenReturn("last-build");
        when(lastBuild.getNumber()).thenReturn(111);

        assertThat(navigator.getSameUrlForOtherBuild(current,
                "http://localhost:8080/job/pipeline-analysis-model/100/spotbugs/",
                "spotbugs",
                "wrong-selection"))
                .isEmpty();
    }

    @Test
    void shouldNotFindBuildIfThereIsNoLastBuild() {
        BuildResultNavigator navigator = new BuildResultNavigator();

        FreeStyleBuild current = mock(FreeStyleBuild.class);
        FreeStyleProject job = mock(FreeStyleProject.class);
        when(current.getParent()).thenReturn(job);
        when(current.getNumber()).thenReturn(100);

        assertThat(navigator.getSameUrlForOtherBuild(current,
                "http://localhost:8080/job/pipeline-analysis-model/100/spotbugs/",
                "spotbugs",
                "wrong-selection"))
                .isEmpty();
    }

}
