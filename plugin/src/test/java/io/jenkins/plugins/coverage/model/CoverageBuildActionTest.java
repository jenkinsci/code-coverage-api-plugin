package io.jenkins.plugins.coverage.model;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.model.HealthReport;
import hudson.model.Run;

import io.jenkins.plugins.coverage.model.visualization.CoverageViewModel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageBuildAction}.
 *
 * @author Ullrich Hafner
 */
class CoverageBuildActionTest {
    @Test
    void shouldCreateViewModel() {
        Run<?, ?> build = mock(Run.class);
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "top-level");
        SortedMap<CoverageMetric, Fraction> metrics = new TreeMap<>();
        SortedMap<CoverageMetric, Fraction> changeCoverage = new TreeMap<>();

        CoverageBuildAction action =
                new CoverageBuildAction(build, root, new HealthReport(), "-", metrics, changeCoverage, false);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getNode).isEqualTo(root);
        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);
    }
}
