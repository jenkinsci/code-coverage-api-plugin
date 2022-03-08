package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.PathUtil;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
class CoveragePluginSourceITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String ACU_COBOL_PARSER = "public&nbsp;class&nbsp;AcuCobolParser&nbsp;extends&nbsp;LookaheadParser&nbsp;{";
    private static final String NO_SOURCE_CODE = "n/a";
    private static final String SOURCE_FILE = "AcuCobolParser.java.txt";
    private static final String PACKAGE_PATH = "edu/hm/hafner/analysis/parser/";
    private static final String ACU_COBOL_PARSER_COVERAGE_REPORT = "jacoco-acu-cobol-parser.xml";
    private static final PathUtil PATH_UTIL = new PathUtil();
    static final String FILE_NAME = "jacoco-analysis-model.xml";

    /** Verifies that the plugin reads source code from the workspace root. */
    @Test
    void coveragePluginPipelineWithSourceCode() {
        Run<?, ?> workspace = runCoverageWithSourceCode("", "");

        assertThat(getConsoleLog(workspace)).contains(createSingleMessage(workspace));
    }

    private String createSingleMessage(final Run<?, ?> workspace) {
        return String.format("Searching for source code files in '%s'", createSingleDirectory(workspace));
    }

    /** Verifies that the plugin reads source code in subdirectories of the workspace.  */
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

        String sourceCodeRetention = "STORE_ALL_BUILD";
        job.setDefinition(createPipelineWithSourceCode(sourceCodeRetention, sourceDirectory));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished painting (0 files have been painted, 1 files failed)")
                .contains(String.format("[-ERROR-] Removing source directory '%s' - it has not been approved in Jenkins' global configuration.",
                        sourceDirectory));

        //verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
    }

    private String createExternalSourceFolder() throws IOException {
        Path tempDirectory = Files.createTempDirectory("coverage");
        Path sourceCodeDirectory = tempDirectory.resolve(PACKAGE_PATH);
        Files.createDirectories(sourceCodeDirectory);
        Files.copy(getResourceAsFile(SOURCE_FILE), sourceCodeDirectory.resolve("AcuCobolParser.java"),
                StandardCopyOption.REPLACE_EXISTING);
        return tempDirectory.toString().replace('\\', '/');
    }

    private Run<?, ?> runCoverageWithSourceCode(final String checkoutDirectory, final String sourceDirectory) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(ACU_COBOL_PARSER_COVERAGE_REPORT);
        copyFileToWorkspace(job, SOURCE_FILE, checkoutDirectory + PACKAGE_PATH + "AcuCobolParser.java");

        String sourceCodeRetention = "STORE_ALL_BUILD";
        job.setDefinition(createPipelineWithSourceCode(sourceCodeRetention, sourceDirectory));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished painting successfully");

        //verifySourceCodeInBuild(firstBuild, ACU_COBOL_PARSER);

        Run<?, ?> secondBuild = buildSuccessfully(job);
        //verifySourceCodeInBuild(secondBuild, ACU_COBOL_PARSER);
        //verifySourceCodeInBuild(firstBuild, ACU_COBOL_PARSER); // should be still available

        job.setDefinition(createPipelineWithSourceCode("STORE_LAST_BUILD", sourceDirectory));
        Run<?, ?> thirdBuild = buildSuccessfully(job);
        //verifySourceCodeInBuild(thirdBuild, ACU_COBOL_PARSER);
        //verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
        //verifySourceCodeInBuild(secondBuild, NO_SOURCE_CODE); // should be still available

        job.setDefinition(createPipelineWithSourceCode("NEVER_STORE", sourceDirectory));
        Run<?, ?> lastBuild = buildSuccessfully(job);
        //verifySourceCodeInBuild(lastBuild, NO_SOURCE_CODE);
        //verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE); // should be still available
        //verifySourceCodeInBuild(secondBuild, NO_SOURCE_CODE); // should be still available
        //verifySourceCodeInBuild(thirdBuild, NO_SOURCE_CODE); // should be still available

        return firstBuild;
    }

    private CpsFlowDefinition createPipelineWithSourceCode(final String sourceCodeRetention,
            final String sourceDirectory) {
        return new CpsFlowDefinition("node {"
                + "    publishCoverage adapters: [jacocoAdapter('" + ACU_COBOL_PARSER_COVERAGE_REPORT + "')], \n"
                + "         sourceFileResolver: sourceFiles('" + sourceCodeRetention + "'), \n"
                + "         sourceCodeEncoding: 'UTF-8', \n"
                + "         sourceDirectories: [[path: '" + sourceDirectory + "']]"
                + "}", true);
    }

    /*private void verifySourceCodeInBuild(final Run<?, ?> build, final String sourceCodeSnippet) {
        SourceViewModel model = verifySourceModel(build);

        assertThat(model.getSourceFileContent()).contains(sourceCodeSnippet);
    }

    private SourceViewModel verifySourceModel(final Run<?, ?> build) {
        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);
        assertThat(action.getLineCoverage())
                .isEqualTo(new Coverage(8, 0));

        Optional<CoverageNode> fileNode = action.getResult().find(CoverageMetric.FILE, "AcuCobolParser.java");
        assertThat(fileNode).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node.getPath()).isEqualTo(
                        "edu/hm/hafner/analysis/parser/AcuCobolParser.java"));

        String link = String.valueOf(fileNode.get().getPath().hashCode());
        SourceViewModel model = action.getTarget().getDynamic(link, null, null);
        assertThat(model.getDisplayName()).contains("AcuCobolParser.java");
        return model;
    }*/

    /** Freestyle job integration test for a simple build with code coverage. */
    @Test
    void coveragePluginFreestyleHelloWorld() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifySimpleCoverageNode(project);
    }

    @SuppressWarnings("PMD.SystemPrintln")
    void verifySimpleCoverageNode(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        System.out.println(getConsoleLog(build));
    }
}
