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
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";

    /**
     * Check if code coverage is supported in declarative pipelines.
     */
    @Test
    public void declarativePipelineSupportJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_XML);

        job.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "    agent any\n"
                + "    stages {\n"
                + "        stage('Test') {\n"
                + "            steps {\n"
                + "                    publishCoverage(\n"
                + "                        adapters: [\n"
                + "                            jacocoAdapter('" + JACOCO_ANALYSIS_MODEL_XML + "')\n"
                + "                        ],\n"
                + "            )}\n"
                + "        }\n"
                + "    }\n"
                + "}", true));
        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getAction(CoverageBuildAction.class).getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
    }

}


