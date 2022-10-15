package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
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
        FreeStyleBuild build = mock(FreeStyleBuild.class);

        CoverageBuildAction action = createBuildAction(build);

        when(build.getAction(CoverageBuildAction.class)).thenReturn(action);
        when(build.getNumber()).thenReturn(15);

        FreeStyleProject job = mock(FreeStyleProject.class);
        when(job.getLastBuild()).thenReturn(build);

        CoverageJobAction jobAction = new CoverageJobAction(job);

        StaplerResponse response = mock(StaplerResponse.class);
        jobAction.doIndex(mock(StaplerRequest.class), response);

        verify(response).sendRedirect2("../15/coverage");
    }

    @Test
    void shouldCreateTrendChartForLineAndBranchCoverage() throws IOException {
        FreeStyleBuild build = mock(FreeStyleBuild.class);

        CoverageBuildAction action = createBuildAction(build);

        when(build.getAction(CoverageBuildAction.class)).thenReturn(action);
        int buildNumber = 15;
        when(build.getNumber()).thenReturn(buildNumber);

        FreeStyleProject job = mock(FreeStyleProject.class);
        when(job.getLastBuild()).thenReturn(build);

        CoverageJobAction jobAction = new CoverageJobAction(job);

        List<BuildResult<CoverageBuildAction>> history = new ArrayList<>();
        BuildResult<CoverageBuildAction> result = new BuildResult<>(new Build(buildNumber), action);
        history.add(result);
        LinesChartModel chart = jobAction.createChart(history, "{}");

        assertThatJson(chart).node("buildNumbers").isArray().hasSize(1).containsExactly(buildNumber);
        assertThatJson(chart).node("domainAxisLabels").isArray().hasSize(1).containsExactly("#15");
        assertThatJson(chart).node("series").isArray().hasSize(2);

        assertThatJson(chart.getSeries().get(0)).satisfies(series -> {
            assertThatJson(series).node("name").isEqualTo("Line");
            assertThatJson(series).node("data").isArray().containsExactly("50");
        });
        assertThatJson(chart.getSeries().get(1)).satisfies(series -> {
            assertThatJson(series).node("name").isEqualTo("Branch");
            assertThatJson(series).node("data").isArray().containsExactly("90");
        });
    }

    private CoverageBuildAction createBuildAction(final FreeStyleBuild build) {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        when(action.getOwner()).thenAnswer(i -> build);
        when(action.getUrlName()).thenReturn("coverage");
        when(action.getBranchCoverage()).thenReturn(new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(9).setMissed(1).build());
        when(action.getLineCoverage()).thenReturn(new CoverageBuilder().setMetric(Metric.LINE).setCovered(10).setMissed(10).build());
        return action;
    }
}
