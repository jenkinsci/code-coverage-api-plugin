package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.PathUtil;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Run;
import hudson.model.TopLevelItem;

import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.prism.SourceCodeRetention;

import static io.jenkins.plugins.prism.SourceCodeRetention.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
class CoveragePluginSourceITest extends AbstractCoverageITest {
    private static final String ACU_COBOL_PARSER = "public&nbsp;class&nbsp;AcuCobolParser&nbsp;extends&nbsp;LookaheadParser&nbsp;{";
    private static final String NO_SOURCE_CODE = "n/a";
    private static final String SOURCE_FILE_NAME = "AcuCobolParser.java";
    private static final String SOURCE_FILE = SOURCE_FILE_NAME + ".txt";
    private static final String PACKAGE_PATH = "edu/hm/hafner/analysis/parser/";
    private static final String SOURCE_FILE_PATH = PACKAGE_PATH + SOURCE_FILE_NAME;
    private static final String ACU_COBOL_PARSER_COVERAGE_REPORT = "jacoco-acu-cobol-parser.xml";
    private static final PathUtil PATH_UTIL = new PathUtil();

    /** Verifies that the plugin reads source code from the workspace root. */
    @Test
    void coveragePluginPipelineWithSourceCode() {
        Run<?, ?> workspace = runCoverageWithSourceCode("", "");

        assertThat(getConsoleLog(workspace)).contains(createSingleMessage(workspace));
    }

    private String createSingleMessage(final Run<?, ?> workspace) {
        return String.format("Searching for source code files in '%s'", createSingleDirectory(workspace));
    }

    /** Verifies that the plugin reads source code in subdirectories of the workspace. */
    @Test
    void coveragePluginPipelineWithSourceCodeInSubdirectory() {
        Run<?, ?> workspace = runCoverageWithSourceCode("", "");
        assertThat(getConsoleLog(workspace)).contains(createSingleMessage(workspace));
    }

    private String createSingleDirectory(final Run<?, ?> workspace) {
        return PATH_UTIL.getAbsolutePath(String.format("%s/src/main/java",
                getWorkspace((TopLevelItem) workspace.getParent()).getRemote()));
    }

    /** Verifies that the plugin reads source code in external but approved directories. */
    @Test
    void coveragePluginPipelineWithSourceCodeInPermittedDirectory() throws IOException {
        String directory = createExternalSourceFolder();
        PrismConfiguration.getInstance().setSourceDirectories(Collections.singletonList(
                new PermittedSourceCodeDirectory(directory)));

        Run<?, ?> externalDirectory = runCoverageWithSourceCode("ignore", directory);
        assertThat(getConsoleLog(externalDirectory))
                .contains("Searching for source code files in:",
                        "-> " + createSingleDirectory(externalDirectory),
                        "-> " + directory);
    }

    /** Verifies that the plugin refuses source code in directories that are not approved in Jenkins' configuration. */
    @Test
    void coveragePluginPipelineNotRegisteredSourceCodeDirectory() throws IOException {
        String sourceDirectory = createExternalSourceFolder();

        WorkflowJob job = createPipelineWithWorkspaceFiles(ACU_COBOL_PARSER_COVERAGE_REPORT);
        copyFileToWorkspace(job, SOURCE_FILE, "ignore/" + PACKAGE_PATH + "AcuCobolParser.java");

        job.setDefinition(createPipelineWithSourceCode(EVERY_BUILD, sourceDirectory));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished painting (0 files have been painted, 1 files failed)")
                .contains(String.format(
                        "[-ERROR-] Removing source directory '%s' - it has not been approved in Jenkins' global configuration.",
                        sourceDirectory));

        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
    }

    private String createExternalSourceFolder() throws IOException {
        Path tempDirectory = Files.createTempDirectory("coverage");
        Path sourceCodeDirectory = tempDirectory.resolve(PACKAGE_PATH);
        Files.createDirectories(sourceCodeDirectory);
        Files.copy(getResourceAsFile(SOURCE_FILE), sourceCodeDirectory.resolve("AcuCobolParser.java"),
                StandardCopyOption.REPLACE_EXISTING);
        return PATH_UTIL.getAbsolutePath(tempDirectory);
    }

    private Run<?, ?> runCoverageWithSourceCode(final String checkoutDirectory, final String sourceDirectory) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(ACU_COBOL_PARSER_COVERAGE_REPORT);
        copyFileToWorkspace(job, SOURCE_FILE, checkoutDirectory + PACKAGE_PATH + "AcuCobolParser.java");

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

        return firstBuild;
    }

    private CpsFlowDefinition createPipelineWithSourceCode(final SourceCodeRetention sourceCodeRetention,
            final String sourceDirectory) {
        return new CpsFlowDefinition("node {"
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
        assertThat(action.getLineCoverage())
                .isEqualTo(new CoverageBuilder().setMetric(Metric.LINE).setCovered(8).setMissed(0).build());

        Optional<Node> fileNode = action.getResult().find(Metric.FILE, SOURCE_FILE_PATH);
        assertThat(fileNode).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node.getPath()).isEqualTo(SOURCE_FILE_PATH));

        return action.getTarget();
    }
}
