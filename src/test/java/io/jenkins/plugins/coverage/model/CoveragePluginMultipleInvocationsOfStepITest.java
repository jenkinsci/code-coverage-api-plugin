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
    private static final String COBERTURA_HIGHER_COVERAGE = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_LOWER_COVERAGE = "cobertura-lower-coverage.xml";

    /**
     * Pipeline with multiple invocations of step and not tag set.
     */
    @Test
    public void withNoTag() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE, COBERTURA_LOWER_COVERAGE);
        // FIXME: Ist das so richtig?
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + COBERTURA_LOWER_COVERAGE + "')]"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + COBERTURA_HIGHER_COVERAGE + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    /**
     * Pipeline with multiple invocations of step and tag set.
     */
    @Test
    public void withTag() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE, COBERTURA_LOWER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters('hi'): [istanbulCoberturaAdapter('" + COBERTURA_LOWER_COVERAGE + "')]"
                + "   publishCoverage adapters('ciao'): [istanbulCoberturaAdapter('" + COBERTURA_HIGHER_COVERAGE + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }
}
