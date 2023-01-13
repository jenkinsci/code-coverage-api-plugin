package io.jenkins.plugins.coverage.metrics.source;

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

import io.jenkins.plugins.coverage.metrics.AbstractCoverageITest;
import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
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
    private static final String PATH_UTIL = "public&nbsp;class&nbsp;PathUtil&nbsp;{";
    private static final String NO_SOURCE_CODE = "n/a";
    static final String ACU_COBOL_PARSER_FILE_NAME = "AcuCobolParser.java";
    static final String ACU_COBOL_PARSER_SOURCE_FILE = ACU_COBOL_PARSER_FILE_NAME + ".txt";
    static final String ACU_COBOL_PARSER_PACKAGE_PATH = "edu/hm/hafner/analysis/parser/";
    private static final String ACU_COBOL_PARSER_SOURCE_FILE_PATH = ACU_COBOL_PARSER_PACKAGE_PATH + ACU_COBOL_PARSER_FILE_NAME;
    private static final String ACU_COBOL_PARSER_COVERAGE_REPORT = "jacoco-acu-cobol-parser.xml";
    private static final String PATH_UTIL_COVERAGE_REPORT = "jacoco-path-util.xml";
    static final String PATH_UTIL_FILE_NAME = "PathUtil.java";
    static final String PATH_UTIL_SOURCE_FILE = PATH_UTIL_FILE_NAME + ".txt";
    static final String PATH_UTIL_PACKAGE_PATH = "edu/hm/hafner/util/";
    private static final String PATH_UTIL_SOURCE_FILE_PATH = PATH_UTIL_PACKAGE_PATH + PATH_UTIL_FILE_NAME;
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
        copyReports(localAgent, job);

        job.setDefinition(createPipelineWithSourceCode(EVERY_BUILD, sourceDirectory));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished painting (0 files have been painted, 1 files failed)")
                .contains(String.format(
                        "[-ERROR-] Removing source directory '%s' - it has not been approved in Jenkins' global configuration.",
                        sourceDirectory));

        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        localAgent.setLabelString("<null>");
    }

    private Run<?, ?> runCoverageWithSourceCode(final String sourceDirectory)
            throws IOException {
        var localAgent = crateCoverageAgent();

        WorkflowJob job = createPipeline();
        copyReports(localAgent, job);
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
        verifySourceCodeInBuild(firstBuild, ACU_COBOL_PARSER, PATH_UTIL);

        Run<?, ?> secondBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(secondBuild, ACU_COBOL_PARSER, PATH_UTIL);
        verifySourceCodeInBuild(firstBuild, ACU_COBOL_PARSER, PATH_UTIL); // should be still available

        job.setDefinition(createPipelineWithSourceCode(LAST_BUILD, sourceDirectory));
        Run<?, ?> thirdBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(thirdBuild, ACU_COBOL_PARSER, PATH_UTIL);
        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(secondBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available

        job.setDefinition(createPipelineWithSourceCode(NEVER, sourceDirectory));
        Run<?, ?> lastBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(lastBuild, NO_SOURCE_CODE, NO_SOURCE_CODE);
        verifySourceCodeInBuild(firstBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(secondBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(thirdBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available

        assertThat(temporaryDirectory.listFiles()).isEqualTo(temporaryFiles);

        localAgent.setLabelString("<null>");
        return firstBuild;
    }

    private void copyReports(final hudson.model.Node localAgent, final WorkflowJob job) {
        copySingleFileToAgentWorkspace(localAgent, job, ACU_COBOL_PARSER_COVERAGE_REPORT, ACU_COBOL_PARSER_COVERAGE_REPORT);
        copySingleFileToAgentWorkspace(localAgent, job, PATH_UTIL_COVERAGE_REPORT, PATH_UTIL_COVERAGE_REPORT);
    }

    private CpsFlowDefinition createPipelineWithSourceCode(final SourceCodeRetention sourceCodeRetention,
            final String sourceDirectory) {
        return new CpsFlowDefinition("node ('coverage-agent') {"
                + "    recordCoverage "
                + "         tools: [[parser: 'JACOCO', pattern: '" + ACU_COBOL_PARSER_COVERAGE_REPORT + "']], \n"
                + "         sourceCodeRetention: '" + sourceCodeRetention.name() + "', \n"
                + "         sourceCodeEncoding: 'UTF-8', \n"
                + "         sourceDirectories: [[path: '" + sourceDirectory + "']]\n"
                + "    recordCoverage id:'path',  "
                + "         tools: [[parser: 'JACOCO', pattern: '" + PATH_UTIL_COVERAGE_REPORT + "']], \n"
                + "         sourceCodeRetention: '" + sourceCodeRetention.name() + "', \n"
                + "         sourceCodeEncoding: 'UTF-8', \n"
                + "         sourceDirectories: [[path: '" + sourceDirectory + "']]"
                + "}", true);
    }

    private void verifySourceCodeInBuild(final Run<?, ?> build, final String acuCobolParserSourceCodeSnippet,
            final String pathUtilSourceCodeSnippet) {
        System.out.println(getConsoleLog(build));

        List<CoverageBuildAction> actions = build.getActions(CoverageBuildAction.class);
        var builder = new CoverageBuilder().setMetric(Metric.LINE).setMissed(0);
        assertThat(actions).hasSize(2).satisfiesExactly(
                action -> {
                    assertThat(action.getAllValues(Baseline.PROJECT)).contains(builder.setCovered(8).build());
                    Optional<Node> fileNode = action.getResult().find(Metric.FILE, ACU_COBOL_PARSER_SOURCE_FILE_PATH);
                    assertThat(fileNode).isNotEmpty()
                            .hasValueSatisfying(node -> assertThat(node.getPath()).isEqualTo(
                                    ACU_COBOL_PARSER_SOURCE_FILE_PATH));
                    assertThat(action.getTarget().getSourceCode(String.valueOf(ACU_COBOL_PARSER_SOURCE_FILE_PATH.hashCode()), "coverage-table"))
                            .contains(acuCobolParserSourceCodeSnippet);
                },
                action -> {
                    assertThat(action.getAllValues(Baseline.PROJECT)).contains(builder.setCovered(43).build());
                    Optional<Node> fileNode = action.getResult().find(Metric.FILE, PATH_UTIL_SOURCE_FILE_PATH);
                    assertThat(fileNode).isNotEmpty()
                            .hasValueSatisfying(node -> assertThat(node.getPath()).isEqualTo(
                                    PATH_UTIL_SOURCE_FILE_PATH));
                    assertThat(action.getTarget().getSourceCode(String.valueOf(PATH_UTIL_SOURCE_FILE_PATH.hashCode()), "coverage-table"))
                            .contains(pathUtilSourceCodeSnippet);
                });
    }

    String createDestinationPath(final String sourceDirectory, final String packagePath, final String fileName) {
        if (sourceDirectory.isEmpty()) {
            return packagePath + fileName;
        }
        else {
            return sourceDirectory + "/" + packagePath + fileName;
        }
    }

    abstract hudson.model.Node crateCoverageAgent();

    abstract String createExternalFolder() throws IOException;

    abstract void copySourceFileToAgent(String sourceDirectory, hudson.model.Node localAgent, WorkflowJob job);
}
