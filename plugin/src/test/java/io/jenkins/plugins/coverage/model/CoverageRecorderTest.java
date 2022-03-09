package io.jenkins.plugins.coverage.model;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Result;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Tests the class {@link CoverageRecorder}.
 *
 * @author Ullrich Hafner
 */
public class CoverageRecorderTest extends IntegrationTestWithJenkinsPerSuite {
    @Test
    public void pipelineForNoAdapter() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "    recordCoverage"
                        + " }", true));

        buildWithResult(job, Result.FAILURE);
    }
}
