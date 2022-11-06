package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Node;

/**
 * Verifies if source code copying and rendering works on Docker agents.
 *
 * @author Ullrich Hafner
 */
@Testcontainers(disabledWithoutDocker = true)
class DockerAgentSourceCodeITest extends SourceCodeITest {
    private static final String SOURCES_IN_DOCKER_PATH = "/tmp/coverage";
    private static final String CONTAINER_PATH = SOURCES_IN_DOCKER_PATH + "/" + PACKAGE_PATH + SOURCE_FILE_NAME;

    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer()
            .withCopyFileToContainer(MountableFile.forClasspathResource("io/jenkins/plugins/coverage/metrics/" + SOURCE_FILE), CONTAINER_PATH);

    @Override
    protected Node crateCoverageAgent() {
        try {
            Node agent = createDockerAgent(AGENT_CONTAINER);
            agent.setLabelString(AGENT_LABEL);
            return agent;
        }
        catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    @Override
    protected String createExternalFolder() throws IOException {
        return SOURCES_IN_DOCKER_PATH;
    }

    @Override
    protected void copySourceFileToAgent(final String sourceDirectory, final Node localAgent, final WorkflowJob job) {
        if (!sourceDirectory.startsWith(SOURCES_IN_DOCKER_PATH)) {
            copySingleFileToAgentWorkspace(localAgent, job, SOURCE_FILE, createDestinationPath(sourceDirectory));
        }
    }
}
