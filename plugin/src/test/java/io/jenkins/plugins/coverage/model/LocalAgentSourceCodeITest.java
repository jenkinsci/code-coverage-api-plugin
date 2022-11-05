package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import edu.hm.hafner.util.PathUtil;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Node;
import hudson.slaves.DumbSlave;

/**
 * Verifies if source code copying and rendering works on local dummy agents (see {@link DumbSlave}).
 *
 * @author Ullrich Hafner
 */
class LocalAgentSourceCodeITest extends SourceCodeITest {
    private static final PathUtil PATH_UTIL = new PathUtil();

    protected hudson.model.Node crateCoverageAgent() {
        return createAgent(AGENT_LABEL);
    }

    protected String createExternalFolder() throws IOException {
        Path tempDirectory = Files.createTempDirectory("coverage");
        Path sourceCodeDirectory = tempDirectory.resolve(PACKAGE_PATH);
        Files.createDirectories(sourceCodeDirectory);
        Files.copy(getResourceAsFile(SOURCE_FILE), sourceCodeDirectory.resolve("AcuCobolParser.java"),
                StandardCopyOption.REPLACE_EXISTING);
        return PATH_UTIL.getAbsolutePath(tempDirectory);
    }

    @Override
    protected void copySourceFileToAgent(final String sourceDirectory, final Node localAgent, final WorkflowJob job) {
        copySingleFileToAgentWorkspace(localAgent, job, SOURCE_FILE, createDestinationPath(sourceDirectory));
    }
}
