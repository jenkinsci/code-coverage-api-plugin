package io.jenkins.plugins.coverage.metrics.source;

import java.io.IOException;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Node;

/**
 * Verifies if source code copying and rendering works on Docker agents.
 *
 * @author Ullrich Hafner
 */
@Testcontainers(disabledWithoutDocker = true)
@SuppressFBWarnings("BC")
class DockerAgentSourceCodeITest extends SourceCodeITest {
    private static final String SOURCES_IN_DOCKER_PATH = "/tmp/coverage";
    private static final String ACU_COBOL_PARSER_CONTAINER_PATH = SOURCES_IN_DOCKER_PATH + "/" + ACU_COBOL_PARSER_PACKAGE_PATH + ACU_COBOL_PARSER_FILE_NAME;
    private static final String PATH_UTIL_CONTAINER_PATH = SOURCES_IN_DOCKER_PATH + "/" + PATH_UTIL_PACKAGE_PATH + PATH_UTIL_FILE_NAME;

    private static final String RESOURCES = "io/jenkins/plugins/coverage/metrics/source/";
    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer()
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource(RESOURCES + ACU_COBOL_PARSER_SOURCE_FILE),
                    ACU_COBOL_PARSER_CONTAINER_PATH)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource(RESOURCES + PATH_UTIL_SOURCE_FILE),
                    PATH_UTIL_CONTAINER_PATH);

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
    protected String createExternalFolder() {
        return SOURCES_IN_DOCKER_PATH;
    }

    @Override
    protected void copySourceFileToAgent(final String sourceDirectory, final Node localAgent, final WorkflowJob job) {
        if (!sourceDirectory.startsWith(SOURCES_IN_DOCKER_PATH)) {
            copySingleFileToAgentWorkspace(localAgent, job,
                    ACU_COBOL_PARSER_SOURCE_FILE,
                    createDestinationPath(sourceDirectory, ACU_COBOL_PARSER_PACKAGE_PATH, ACU_COBOL_PARSER_FILE_NAME));
            copySingleFileToAgentWorkspace(localAgent, job,
                    PATH_UTIL_SOURCE_FILE,
                    createDestinationPath(sourceDirectory, PATH_UTIL_PACKAGE_PATH, PATH_UTIL_FILE_NAME));
        }
    }
}
