package io.jenkins.plugins.coverage.model;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
@Testcontainers(disabledWithoutDocker = true)
class DockerCoveragePluginSourceITest extends CoveragePluginSourceITest {
    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer();

    /** Integration test for a freestyle build with code coverage that runs on an agent. */
    @Test
    void coverageFreeStyleOnAgent() throws IOException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);
        project.setAssignedNode(agent);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_ANALYSIS_MODEL_FILE);

        verifySimpleCoverageNode(project);
    }

    /** Integration test for a pipeline with code coverage that runs on an agent. */
    @Test
    void coveragePipelineOnAgentNode() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_ANALYSIS_MODEL_FILE);

        verifySimpleCoverageNode(project);
    }

    @SuppressWarnings("PMD.SystemPrintln")
    private void verifySimpleCoverageNode(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage.CoverageBuilder().setMetric(Metric.LINE)
                        .setCovered(JACOCO_ANALYSIS_MODEL_COVERED)
                        .setMissed(JACOCO_ANALYSIS_MODEL_MISSED).build());
        System.out.println(getConsoleLog(build));
    }

    private WorkflowJob createPipelineOnAgent() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node('" + DOCKER_AGENT_NAME + "') {"
                + "timestamps {\n"
                + "    checkout([$class: 'GitSCM', "
                + "        branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c' ]],\n"
                + "        userRemoteConfigs: [[url: '" + "https://github.com/jenkinsci/analysis-model.git" + "']],\n"
                + "        extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "                    relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_ANALYSIS_MODEL_FILE
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}"
                + "}", true));

        return job;
    }
}
