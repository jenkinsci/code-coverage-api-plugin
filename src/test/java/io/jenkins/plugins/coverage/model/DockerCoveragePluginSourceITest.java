package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Node;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;

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
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);

        copySingleFileToAgentWorkspace(agent, project, FILE_NAME, FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifySimpleCoverageNode(project);
    }

    /** Integration test for a pipeline with code coverage that runs on an agent. */
    @Test
    void coveragePipelineOnAgentNode() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, FILE_NAME, FILE_NAME);

        verifySimpleCoverageNode(project);
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
                + "    publishCoverage adapters: [jacocoAdapter('" + FILE_NAME
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}"
                + "}", true));

        return job;
    }
}
