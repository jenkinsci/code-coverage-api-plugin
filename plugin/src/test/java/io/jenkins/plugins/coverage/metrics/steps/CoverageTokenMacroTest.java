package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoverageTokenMacroTest extends AbstractCoverageTest {
    @Test
    void shouldCreateEmptyTokenWhenNoActionPresent() {
        var macro = new CoverageTokenMacro();

        assertThat(evaluate(macro, mock(Run.class))).isEqualTo("n/a");
        assertThat(macro.evaluate(mock(AbstractBuild.class), TaskListener.NULL, "coverage")).isEqualTo("n/a");
        assertThat(macro.acceptsMacroName(CoverageTokenMacro.COVERAGE)).isTrue();
    }

    @Test
    void shouldReturnValuesForDifferentMetricsAndBaselines() {
        var macro = new CoverageTokenMacro();

        var action = mock(CoverageBuildAction.class);
        when(action.getUrlName()).thenReturn("coverage");
        when(action.getStatistics()).thenReturn(createStatistics());

        var build = mock(Run.class);
        when(build.getActions(CoverageBuildAction.class)).thenReturn(List.of(action));

        assertThat(evaluate(macro, build)).isEqualTo("50.00%");

        macro.setMetric(Metric.BRANCH.name());
        assertThat(evaluate(macro, build)).isEqualTo("90.00%");

        macro.setMetric(Metric.FILE.name());
        assertThat(evaluate(macro, build)).isEqualTo("75.00%");

        macro.setBaseline(Baseline.MODIFIED_LINES_DELTA.name());
        assertThat(evaluate(macro, build)).isEqualTo("-10.00%");

        macro.setId("unknown");
        assertThat(evaluate(macro, build)).isEqualTo("n/a");

        macro.setId("");
        assertThat(evaluate(macro, build)).isEqualTo("-10.00%");

        macro.setMetric(Metric.MUTATION.name());
        assertThat(evaluate(macro, build)).isEqualTo("n/a");
    }

    private String evaluate(final CoverageTokenMacro macro, final Run<?, ?> run) {
        return macro.evaluate(run, mock(FilePath.class), TaskListener.NULL, "coverage");
    }
}
