package io.jenkins.plugins.coverage.metrics;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateCriticality;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests with active quality gates.
 */
class QualityGateITest extends AbstractCoverageITest {
    @Test
    void shouldNotHaveQualityGate() {
        WorkflowJob job = createPipeline(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.INACTIVE);
    }

    @Test
    void shouldPassQualityGate() {
        var qualityGates = List.of(new QualityGate(-100.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void shouldFailQualityGateWithUnstable() {
        var qualityGates = List.of(new QualityGate(100, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.WARNING);


    }

    @Test
    void shouldFailQualityGateWithFailure() {
        var qualityGates = List.of(new QualityGate(100, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE));
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.FAILED);
    }

    @Test
    void shouldUseQualityGateInPipeline() {
        WorkflowJob project = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);

        setPipelineScript(project,
                "recordCoverage("
                        + "tools: [[parser: '" + CoverageParser.JACOCO.name() + "', pattern: '**/*xml']],\n"
                        + "qualityGates: ["
                        + "     [threshold: 90.0, metric: 'LINE', baseline: 'PROJECT', unstable: true], "
                        + "     [threshold: 90.0, metric: 'BRANCH', baseline: 'PROJECT', unstable: true]])\n");

        WorkflowRun build = (WorkflowRun)buildWithResult(project, Result.UNSTABLE);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.WARNING);

        assertThat(coverageResult.getLog().getInfoMessages()).contains("Evaluating quality gates",
                "-> [Overall project - Line]: ≪PASSED≫ - (Actual value: LINE: 95.39% (5531/5798), Quality gate: 90.00)",
                "-> [Overall project - Branch]: ≪WARNING≫ - (Actual value: BRANCH: 88.28% (1544/1749), Quality gate: 90.00)",
                "-> Some quality gates have been missed: overall result is WARNING");


        FlowNode flowNode = new DepthFirstScanner().findFirstMatch(build.getExecution(),
                node -> "recordCoverage".equals(Objects.requireNonNull(node).getDisplayFunctionName()));
        assertThat(flowNode).isNotNull();

        WarningAction warningAction = flowNode.getPersistentAction(WarningAction.class);
        assertThat(warningAction).isNotNull();
        assertThat(warningAction.getMessage()).isEqualTo(
                "Some quality gates have been missed: overall result is UNSTABLE");
    }
}
