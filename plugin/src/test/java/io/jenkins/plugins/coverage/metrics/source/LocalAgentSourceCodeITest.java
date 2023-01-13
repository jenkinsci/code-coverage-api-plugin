package io.jenkins.plugins.coverage.metrics.source;

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

    @Override
    protected Node crateCoverageAgent() {
        return createAgent(AGENT_LABEL);
    }

    @Override
    protected String createExternalFolder() throws IOException {
        Path tempDirectory = Files.createTempDirectory("coverage");

        createFile(tempDirectory,
                ACU_COBOL_PARSER_PACKAGE_PATH, ACU_COBOL_PARSER_SOURCE_FILE, ACU_COBOL_PARSER_FILE_NAME);
        createFile(tempDirectory,
                PATH_UTIL_PACKAGE_PATH, PATH_UTIL_SOURCE_FILE, PATH_UTIL_FILE_NAME);

        return PATH_UTIL.getAbsolutePath(tempDirectory);
    }

    private void createFile(final Path tempDirectory,
            final String packagePath, final String sourceName, final String fileName) throws IOException {
        Path sourceCodeDirectory = tempDirectory.resolve(packagePath);
        Files.createDirectories(sourceCodeDirectory);
        Files.copy(getResourceAsFile(sourceName), sourceCodeDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    protected void copySourceFileToAgent(final String sourceDirectory, final Node localAgent, final WorkflowJob job) {
        copySingleFileToAgentWorkspace(localAgent, job, ACU_COBOL_PARSER_SOURCE_FILE, createDestinationPath(sourceDirectory,
                ACU_COBOL_PARSER_PACKAGE_PATH, ACU_COBOL_PARSER_FILE_NAME));
        copySingleFileToAgentWorkspace(localAgent, job, PATH_UTIL_SOURCE_FILE, createDestinationPath(sourceDirectory,
                PATH_UTIL_PACKAGE_PATH, PATH_UTIL_FILE_NAME));
    }
}
