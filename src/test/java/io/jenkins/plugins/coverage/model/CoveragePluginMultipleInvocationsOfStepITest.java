package io.jenkins.plugins.coverage.model;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Test multiple invocations of step.
 */
public class CoveragePluginMultipleInvocationsOfStepITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_LOWER_BRANCH_COVERAGE = "jacoco-analysis-model.xml";
    private static final String JACOCO_HIGHER_BRANCH_COVERAGE = "jacoco-codingstyle.xml";
    private static final int JACOCO_HIGHER_BRANCH_COVERAGE_COVERED_VALUE = 109;
    private static final int JACOCO_HIGHER_BRANCH_COVERAGE_MISSED_VALUE = 7;
    private static final int JACOCO_LOWER_BRANCH_COVERAGE_COVERED_VALUE = 1661;
    private static final int JACOCO_LOWER_BRANCH_COVERAGE_MISSED_VALUE = 214;

    /**
     * Pipeline with multiple invocations of step, no tag set and higher coverage file first.
     */
    @Test
    public void withoutTagFirstLowerFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_LOWER_BRANCH_COVERAGE, JACOCO_HIGHER_BRANCH_COVERAGE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_HIGHER_BRANCH_COVERAGE + "')]\n"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_LOWER_BRANCH_COVERAGE + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(JACOCO_LOWER_BRANCH_COVERAGE_COVERED_VALUE, JACOCO_LOWER_BRANCH_COVERAGE_MISSED_VALUE));

    }

    /**
     * Pipeline with multiple invocations of step, no tag set and lower coverage file first.
     */
    @Test
    public void withoutTagFirstHigherFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_LOWER_BRANCH_COVERAGE, JACOCO_HIGHER_BRANCH_COVERAGE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_LOWER_BRANCH_COVERAGE + "')]\n"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_HIGHER_BRANCH_COVERAGE + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(JACOCO_HIGHER_BRANCH_COVERAGE_COVERED_VALUE, JACOCO_HIGHER_BRANCH_COVERAGE_MISSED_VALUE));
    }

    /**
     * Pipeline with multiple invocations of step and tag set.
     */
    @Test
    public void withTag() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_LOWER_BRANCH_COVERAGE, JACOCO_HIGHER_BRANCH_COVERAGE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_LOWER_BRANCH_COVERAGE + "')]\n"
                + "   tag: 'Tag1'\n"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_HIGHER_BRANCH_COVERAGE + "')]\n"
                + "   tag: 'Tag2'\n"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(JACOCO_HIGHER_BRANCH_COVERAGE_COVERED_VALUE, JACOCO_HIGHER_BRANCH_COVERAGE_MISSED_VALUE));
    }
}
