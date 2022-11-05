package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Run;

import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.prism.SourceCodeRetention;

import static io.jenkins.plugins.prism.SourceCodeRetention.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Verifies if source code copying and rendering works on agents.
 *
 * @author Ullrich Hafner
 */
abstract class SourceCodeITest extends AbstractCoverageITest {
    private static final String ACU_COBOL_PARSER = "public&nbsp;class&nbsp;AcuCobolParser&nbsp;extends&nbsp;LookaheadParser&nbsp;{";
    private static final String NO_SOURCE_CODE = "n/a";
    static final String SOURCE_FILE_NAME = "AcuCobolParser.java";
    static final String SOURCE_FILE = SOURCE_FILE_NAME + ".txt";
    static final String PACKAGE_PATH = "edu/hm/hafner/analysis/parser/";
    private static final String SOURCE_FILE_PATH = PACKAGE_PATH + SOURCE_FILE_NAME;
    private static final String ACU_COBOL_PARSER_COVERAGE_REPORT = "jacoco-acu-cobol-parser.xml";
    static final String AGENT_LABEL = "coverage-agent";

    /** Verifies that the plugin reads source code from the workspace root. */
    @Test
    void coveragePluginPipelineWithSourceCode() throws IOException {
        runCoverageWithSourceCode("");
    }

    /** Verifies that the plugin reads source code in subdirectories of the workspace. */
    @Test
    void coveragePluginPipelineWithSourceCodeInSubdirectory() throws IOException {
        runCoverageWithSourceCode("sub-dir");
    }

    /** Verifies that the plugin reads source code in external but approved directories. */
    @Test
    void coveragePluginPipelineWithSourceCodeInPermittedDirectory() throws IOException {
        String directory = createExternalFolder();
        PrismConfiguration.getInstance().setSourceDirectories(List.of(new PermittedSourceCodeDirectory(directory)));

        Run<?, ?> externalDirectory = runCoverageWithSourceCode(directory);
        assertThat(getConsoleLog(externalDirectory))
                .contains("Searching for source code files in:", "-> " + directory);
    }

    /** Verifies that the plugin refuses source code in directories that are not approved in Jenkins' configuration. */
    @Test
    void coveragePluginPipelineNotRegisteredSourceCodeDirectory() throws IOException {
        var localAgent = crateCoverageAgent();
        String sourceDirectory = createExternalFolder();

        WorkflowJob job = createPipeline();
        copySourceFileToAgent("ignore/", localAgent, job);
        copySingleFileToAgentWorkspace(localAgent, job, ACU_COBOL_PARSER_COVERAGE_REPORT, ACU_COBOL_PARSER_COVERAGE_REPORT);

        job.setDefinition(createPipelineWithSourceCode(EVERY_BUILD, sourceDirectory));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished painting (0 files have been painted, 1 files failed)")
                .contains(String.format(
                        "[-ERROR-] Removing source directory '%s' - it has not been approved in Jenkins' global configuration.",
                        sourceDirectory));

        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
        localAgent.setLabelString("<null>");
    }

    private Run<?, ?> runCoverageWithSourceCode(final String sourceDirectory)
            throws IOException {
        var localAgent = crateCoverageAgent();

        WorkflowJob job = createPipeline();
        copySingleFileToAgentWorkspace(localAgent, job, ACU_COBOL_PARSER_COVERAGE_REPORT, ACU_COBOL_PARSER_COVERAGE_REPORT);
        copySourceFileToAgent(sourceDirectory, localAgent, job);

        // get the temporary directory - used by unit tests - to verify its content
        File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));
        assertThat(temporaryDirectory.exists()).isTrue();
        assertThat(temporaryDirectory.isDirectory()).isTrue();
        File[] temporaryFiles = temporaryDirectory.listFiles();

        job.setDefinition(createPipelineWithSourceCode(EVERY_BUILD, sourceDirectory));
        Run<?, ?> firstBuild = buildSuccessfully(job);
        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished painting successfully");
        verifySourceCodeInBuild(firstBuild, ACU_COBOL_PARSER);

        Run<?, ?> secondBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(secondBuild, ACU_COBOL_PARSER);
        verifySourceCodeInBuild(firstBuild, ACU_COBOL_PARSER); // should be still available

        job.setDefinition(createPipelineWithSourceCode(LAST_BUILD, sourceDirectory));
        Run<?, ?> thirdBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(thirdBuild, ACU_COBOL_PARSER);
        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(secondBuild, NO_SOURCE_CODE); // should be still available

        job.setDefinition(createPipelineWithSourceCode(NEVER, sourceDirectory));
        Run<?, ?> lastBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(lastBuild, NO_SOURCE_CODE);
        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(secondBuild, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(thirdBuild, NO_SOURCE_CODE); // should be still available

        assertThat(temporaryDirectory.listFiles()).isEqualTo(temporaryFiles);

        localAgent.setLabelString("<null>");
        return firstBuild;
    }

    private CpsFlowDefinition createPipelineWithSourceCode(final SourceCodeRetention sourceCodeRetention,
            final String sourceDirectory) {
        return new CpsFlowDefinition("node ('coverage-agent') {"
                + "    recordCoverage tools: [[parser: 'JACOCO', pattern: '**/*xml']], \n"
                + "         sourceCodeRetention: '" + sourceCodeRetention.name() + "', \n"
                + "         sourceCodeEncoding: 'UTF-8', \n"
                + "         sourceDirectories: [[path: '" + sourceDirectory + "']]"
                + "}", true);
    }

    private void verifySourceCodeInBuild(final Run<?, ?> build, final String sourceCodeSnippet) {
        CoverageViewModel model = verifyViewModel(build);

        assertThat(model.getSourceCode(String.valueOf(SOURCE_FILE_PATH.hashCode()), "coverage-table"))
                .contains(sourceCodeSnippet);
    }

    private CoverageViewModel verifyViewModel(final Run<?, ?> build) {
        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);

        System.out.println(getConsoleLog(build));

        assertThat(action.getLineCoverage())
                .isEqualTo(new CoverageBuilder().setMetric(Metric.LINE).setCovered(8).setMissed(0).build());

        Optional<Node> fileNode = action.getResult().find(Metric.FILE, SOURCE_FILE_PATH);
        assertThat(fileNode).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node.getPath()).isEqualTo(SOURCE_FILE_PATH));

        return action.getTarget();
    }

    String createDestinationPath(final String sourceDirectory) {
        if (sourceDirectory.isEmpty()) {
            return PACKAGE_PATH + "AcuCobolParser.java";
        }
        else {
            return sourceDirectory + "/" + PACKAGE_PATH + "AcuCobolParser.java";
        }
    }

    abstract hudson.model.Node crateCoverageAgent();

    abstract String createExternalFolder() throws IOException;

    abstract void copySourceFileToAgent(String sourceDirectory, hudson.model.Node localAgent, WorkflowJob job);
}
