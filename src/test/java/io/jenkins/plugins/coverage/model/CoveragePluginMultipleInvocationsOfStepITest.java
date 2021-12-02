package io.jenkins.plugins.coverage.model;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Run;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class CoveragePluginMultipleInvocationsOfStepITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String COBERTURA_HIGHER_COVERAGE = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_LOWER_COVERAGE = "cobertura-lower-coverage.xml";

    @Test
    public void test() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE,COBERTURA_LOWER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('"+ COBERTURA_LOWER_COVERAGE +"')]"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('"+ COBERTURA_HIGHER_COVERAGE +"')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);
        //CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
    }
}
