package io.jenkins.plugins.coverage.model;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Integration test to check if declarative pipelines are supported.
 */
public class DeclarativePipelineSupportITest extends IntegrationTestWithJenkinsPerSuite {

    /**
     * Check if declarative pipelines are supported.
     */
    @Test
    public void declarativePipelineShouldRunSuccessfully() {
        WorkflowJob job = createPipeline();
        //TODO: ersetzen
        job.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "    agent any\n"
                + "    stages {\n"
                + "        stage('Welcome Step') {\n"
                + "            steps { \n"
                + "                echo 'Welcome to LambdaTest'\n"
                //command for code coverage
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}", true));
        buildSuccessfully(job);
    }

}


