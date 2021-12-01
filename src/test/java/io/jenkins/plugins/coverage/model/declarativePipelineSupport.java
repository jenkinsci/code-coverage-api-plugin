package io.jenkins.plugins.coverage.model;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;


@SuppressWarnings({"PMD.ExcessiveImports", "PMD.ExcessiveClassLength", "PMD.ExcessivePublicCount", "PMD.GodClass", "checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public class declarativePipelineSupport extends IntegrationTestWithJenkinsPerSuite {

    @Test
    public void declarativePipelineShouldRunSuccessfully() {
        //FIXME: (NoSuchMethodError: No such DSL method 'pipeline' found among steps)
        WorkflowJob job = createPipeline();

        job.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "    agent any\n"
                + "    stages {\n"
                + "        stage('Welcome Step') {\n"
                + "            steps { \n"
                + "                echo 'Welcome to LambdaTest'\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}", true));
        buildSuccessfully(job);
    }

}


