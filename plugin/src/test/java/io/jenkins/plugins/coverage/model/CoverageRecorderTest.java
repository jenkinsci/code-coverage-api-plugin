package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageRecorder}.
 *
 * @author Ullrich Hafner
 */
class CoverageRecorderTest extends IntegrationTestWithJenkinsPerSuite {
    @Test
    void pipelineForNoAdapter() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition(
                "node {\n"
                        + "    recordCoverage()\n"
                        + " }\n", true));

        Run<?, ?> run = buildWithResult(job, Result.SUCCESS);

        assertThat(getConsoleLog(run)).isEqualTo("blub");
    }
}
