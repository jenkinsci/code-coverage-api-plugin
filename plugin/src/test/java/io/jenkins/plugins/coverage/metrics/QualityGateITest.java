package io.jenkins.plugins.coverage.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateResult;

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
        var qualityGates = List.of(new QualityGate(-100.0, Metric.LINE, Baseline.PROJECT, QualityGateResult.UNSTABLE));
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void shouldFailQualityGateWithUnstable() {
        var qualityGates = List.of(new QualityGate(100, Metric.LINE, Baseline.PROJECT, QualityGateResult.UNSTABLE));
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void shouldFailQualityGateWithFailure() {
        var qualityGates = List.of(new QualityGate(100, Metric.LINE, Baseline.PROJECT, QualityGateResult.FAILURE));
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

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getQualityGateStatus()).isEqualTo(QualityGateStatus.WARNING);

        assertThat(coverageResult.getLog().getInfoMessages()).contains("Evaluating quality gates",
                "-> [Overall project - Line]: ≪PASSED≫ - (Actual value: LINE: 95.39% (5531/5798), Quality gate: 90.00)",
                "-> [Overall project - Branch]: ≪WARNING≫ - (Actual value: BRANCH: 88.28% (1544/1749), Quality gate: 90.00)",
                "-> Some quality gates have been missed: overall result is WARNING");
    }
}
