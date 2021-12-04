package io.jenkins.plugins.coverage.model;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Run;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test to check if declarative pipelines are supported.
 */
public class DeclarativePipelineSupportITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String COBERTURA_HIGHER_COVERAGE = "cobertura-higher-coverage.xml";
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Check if code coverage is supported in declarative pipelines.
     */
    @Test
    public void declarativePipelineShouldRunSuccessfully() {

        //cobertura
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE);

        job.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "\n"
                + "    agent any\n"
                + "\n"
                + "    stages {\n"
                + "\n"
                + "        stage('Test') {\n"
                + "            steps {\n"
                + "                    publishCoverage(\n"
                + "                        adapters: [\n"
                + "                            istanbulCoberturaAdapter('" + COBERTURA_HIGHER_COVERAGE + "')\n"
                + "                        ],\n"
                + "            )}\n"
                + "        }\n"
                + "    }\n"
                + "}", true));
        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getAction(CoverageBuildAction.class).getLineCoverage().getCovered()).isEqualTo(2);
        //TODO: add assertions

        //cobertura
        WorkflowJob jobJacoco = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE);

        jobJacoco.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "\n"
                + "    agent any\n"
                + "\n"
                + "    stages {\n"
                + "\n"
                + "        stage('Test') {\n"
                + "            steps {\n"
                + "                    publishCoverage(\n"
                + "                        adapters: [\n"
                + "                            jacocoAdapter('" + JACOCO_FILE_NAME + "')\n"
                + "                        ],\n"
                + "            )}\n"
                + "        }\n"
                + "    }\n"
                + "}", true));

        Run<?, ?> buildJacoco = buildSuccessfully(jobJacoco);
        //TODO: add assertions
    }

}


