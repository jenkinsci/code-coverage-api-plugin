package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.FractionValue;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;

import hudson.model.FreeStyleBuild;

import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.util.QualityGateResult;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageBuildAction}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class CoverageBuildActionTest {
    private CoverageBuildAction createCoverageBuildActionWithDelta() {
        Node module = new ModuleNode("module");

        var coverageBuilder = new CoverageBuilder();
        var percent50 = coverageBuilder.setMetric(Metric.BRANCH).setCovered(1).setMissed(1).build();
        var percent80 = coverageBuilder.setMetric(Metric.LINE).setCovered(8).setMissed(2).build();
        var percent30 = coverageBuilder.setMetric(Metric.INSTRUCTION).setCovered(3).setMissed(7).build();
        var percent35 = coverageBuilder.setMetric(Metric.CLASS).setCovered(35).setMissed(65).build();
    
        module.addValue(percent50);
        module.addValue(percent80);
        module.addValue(percent30);
        module.addValue(percent35);
        
        var deltas = new TreeMap<Metric, Fraction>();
        var lineDelta = percent80.getCoveredPercentage().subtract(percent50.getCoveredPercentage());
        deltas.put(Metric.LINE, lineDelta);
        var branchDelta = percent50.getCoveredPercentage().subtract(percent80.getCoveredPercentage());
        deltas.put(Metric.BRANCH, branchDelta);
        var instructionDelta = percent30.getCoveredPercentage().subtract(percent30.getCoveredPercentage());
        deltas.put(Metric.INSTRUCTION, instructionDelta);
        var classDelta = percent35.getCoveredPercentage().subtract(percent30.getCoveredPercentage());
        deltas.put(Metric.CLASS, classDelta);
        deltas.put(Metric.FILE, Fraction.getFraction(99,1000));  // to test for boundary case

        var coverages = List.of(percent50, percent80, percent30, percent35);

        return spy(new CoverageBuildAction(mock(FreeStyleBuild.class), CoverageRecorder.DEFAULT_ID,
                StringUtils.EMPTY, StringUtils.EMPTY, module, new QualityGateResult(),
                createLog(), "-", deltas, coverages, deltas, coverages, deltas, coverages, false));
    }

    @Test
    void shouldNotLoadResultIfCoverageValuesArePersistedInAction() {
        Node module = new ModuleNode("module");

        var coverageBuilder = new CoverageBuilder();
        var percent50 = coverageBuilder.setMetric(Metric.BRANCH).setCovered(1).setMissed(1).build();
        var percent80 = coverageBuilder.setMetric(Metric.LINE).setCovered(8).setMissed(2).build();

        module.addValue(percent50);
        module.addValue(percent80);

        var deltas = new TreeMap<Metric, Fraction>();
        var lineDelta = percent80.getCoveredPercentage().subtract(percent50.getCoveredPercentage());
        deltas.put(Metric.LINE, lineDelta);
        var branchDelta = percent50.getCoveredPercentage().subtract(percent80.getCoveredPercentage());
        deltas.put(Metric.BRANCH, branchDelta);

        var coverages = List.of(percent50, percent80);
        var action = spy(new CoverageBuildAction(mock(FreeStyleBuild.class), CoverageRecorder.DEFAULT_ID,
                StringUtils.EMPTY, StringUtils.EMPTY, module, new QualityGateResult(),
                createLog(), "-", deltas, coverages, deltas, coverages, deltas, coverages, false));

        when(action.getResult()).thenThrow(new IllegalStateException("Result should not be accessed with getResult() when getting a coverage metric that is persisted in the build"));

        assertThat(action.getReferenceBuild()).isEmpty();

        assertThat(action.getStatistics().getValue(Baseline.PROJECT, Metric.BRANCH)).hasValue(percent50);
        assertThat(action.getStatistics().getValue(Baseline.PROJECT, Metric.LINE)).hasValue(percent80);
        assertThat(action.getStatistics().getValue(Baseline.MODIFIED_LINES, Metric.BRANCH)).hasValue(percent50);
        assertThat(action.getStatistics().getValue(Baseline.MODIFIED_LINES, Metric.LINE)).hasValue(percent80);
        assertThat(action.getStatistics().getValue(Baseline.MODIFIED_FILES, Metric.BRANCH)).hasValue(percent50);
        assertThat(action.getStatistics().getValue(Baseline.MODIFIED_FILES, Metric.LINE)).hasValue(percent80);
        assertThat(action.getStatistics().getValue(Baseline.PROJECT_DELTA, Metric.LINE))
                .hasValue(new FractionValue(Metric.LINE, lineDelta));
        assertThat(action.getStatistics().getValue(Baseline.PROJECT_DELTA, Metric.BRANCH))
                .hasValue(new FractionValue(Metric.BRANCH, branchDelta));

        assertThat(action.getAllValues(Baseline.PROJECT)).containsAll(coverages);
    }

    private static CoverageBuildAction createEmptyAction(final Node module) {
        return new CoverageBuildAction(mock(FreeStyleBuild.class), CoverageRecorder.DEFAULT_ID,
                StringUtils.EMPTY, StringUtils.EMPTY, module, new QualityGateResult(), createLog(), "-",
                new TreeMap<>(), List.of(), new TreeMap<>(), List.of(), new TreeMap<>(), List.of(), false);
    }

    private static FilteredLog createLog() {
        return new FilteredLog("Errors");
    }

    @Test
    void shouldCreateViewModel() {
        Node root = new ModuleNode("top-level");
        CoverageBuildAction action = createEmptyAction(root);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getNode).isSameAs(root);
        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isSameAs(action.getOwner());
    }

    @Test
    void shouldReturnPositiveTrendForLineMetric() {
        CoverageBuildAction action = createCoverageBuildActionWithDelta();
        double trend = action.getTrend(Baseline.PROJECT, Metric.LINE);
        assertThat(trend).isEqualTo(0.3);    // deltaValue = 0.3
    }

    @Test
    void shouldReturnNegativeTrendForBranchMetric() {
        CoverageBuildAction action = createCoverageBuildActionWithDelta();
        double trend = action.getTrend(Baseline.PROJECT, Metric.BRANCH);
        assertThat(trend).isEqualTo(-0.3);   // deltaValue = -0.3
    } 

    @Test
    void shouldReturnZeroForZeroDelta() {
        CoverageBuildAction action = createCoverageBuildActionWithDelta();
        double trend = action.getTrend(Baseline.PROJECT, Metric.INSTRUCTION);
        assertThat(trend).isEqualTo(0.0);    // deltaValue = 0.0
    } 

    @Test
    void shouldReturnZeroWhenDeltaIsNotPresent() {
        CoverageBuildAction action = createCoverageBuildActionWithDelta();
        double trend = action.getTrend(Baseline.PROJECT, Metric.METHOD);
        assertThat(trend).isEqualTo(0);      // deltaValue is not present
    }

    @Test
    void shouldReturnZeroForDeltaWithinBoundaries() {
        CoverageBuildAction action = createCoverageBuildActionWithDelta();
        double trend = action.getTrend(Baseline.PROJECT, Metric.CLASS);
        assertThat(trend).isEqualTo(0);      // deltaValue = 0.05
    }

    @Test
    void shouldReturnPositiveTrendForBoundaryDeltaValue() {
        CoverageBuildAction action = createCoverageBuildActionWithDelta();
        double trend = action.getTrend(Baseline.PROJECT, Metric.FILE);
        assertThat(trend).isEqualTo(0.1);    // deltaValue = 0.099 (will be rounded off)
    }
}
