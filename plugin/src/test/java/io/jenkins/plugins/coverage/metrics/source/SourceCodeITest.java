package io.jenkins.plugins.coverage.metrics.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.PathUtil;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageITest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageBuildAction;
import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.prism.SourceCodeRetention;

import static io.jenkins.plugins.prism.SourceCodeRetention.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Verifies the source code copying and rendering on agents.
 *
 * @author Ullrich Hafner
 */
abstract class SourceCodeITest extends AbstractCoverageITest {
    private static final String ACU_COBOL_PARSER = "<tr class=\"coverFull\" data-html-tooltip=\"Covered at least once\"><td class=\"line\"><a name=\"27\">27</a></td><td class=\"hits\">1</td><td class=\"code\">        super(ACU_COBOL_WARNING_PATTERN);</td></tr>";
    private static final String PATH_UTIL = "<tr class=\"coverFull\" data-html-tooltip=\"Covered at least once\"><td class=\"line\"><a name=\"20\">20</a></td><td class=\"hits\">1</td><td class=\"code\">public class PathUtil {<!-- --></td></tr>";
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
    private static final PathUtil UTIL = new PathUtil();

    @ParameterizedTest(name = "Entries of `sourceDirectories` use absolute paths: {0}")
    @ValueSource(booleans = {true, false})
    void verifySourcesInWorkspaceRoot(final boolean useAbsolutePath) throws IOException {
        runCoverageWithSourceCode("", useAbsolutePath);
    }

    @ParameterizedTest(name = "Entries of `sourceDirectories` use absolute paths: {0}")
    @ValueSource(booleans = {true, false})
    void verifySourcesInWorkspaceSubFolder(final boolean useAbsolutePath) throws IOException {
        runCoverageWithSourceCode("sub-dir", useAbsolutePath);
    }

    @Test
    void verifySourcesInApprovedExternalFolder() throws IOException {
        String directory = createExternalFolder();
        PrismConfiguration.getInstance().setSourceDirectories(List.of(new PermittedSourceCodeDirectory(directory)));

        Run<?, ?> externalDirectory = runCoverageWithSourceCode(directory, false);
        assertThat(getConsoleLog(externalDirectory))
                .contains("Searching for source code files in:", "-> " + directory);
    }

    @Test
    void refuseSourceCodePaintingInNotApprovedExternalFolder() throws IOException {
        var localAgent = crateCoverageAgent();
        String sourceDirectory = createExternalFolder();

        WorkflowJob job = createPipeline();
        var subFolder = "ignore/";
        copySourceFileToAgent(subFolder, localAgent, job);
        copyReports(localAgent, job);

        job.setDefinition(createPipelineWithSourceCode(EVERY_BUILD, sourceDirectory));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        assertThat(getConsoleLog(firstBuild))
                .contains("-> finished resolving of absolute paths (found: 0, not found: 1)")
                .contains("-> finished painting (0 files have been painted, 1 files failed)")
                .contains(String.format(
                        "[-ERROR-] Removing non-workspace source directory '%s' - it has not been approved in Jenkins' global configuration.",
                        sourceDirectory))
                .contains("- Source file '" + ACU_COBOL_PARSER_SOURCE_FILE_PATH + "' not found");
        localAgent.setLabelString("<null>");
    }

    private Run<?, ?> runCoverageWithSourceCode(final String sourceDir, final boolean useAbsolutePath)
            throws IOException {
        var localAgent = crateCoverageAgent();

        WorkflowJob job = createPipeline();
        copyReports(localAgent, job);
        copySourceFileToAgent(sourceDir, localAgent, job);

        // get the temporary directory - used by unit tests - to verify its content
        File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));
        assertThat(temporaryDirectory.exists()).isTrue();
        assertThat(temporaryDirectory.isDirectory()).isTrue();
        File[] temporaryFiles = temporaryDirectory.listFiles();

        String requestedSourceFolder;
        if (useAbsolutePath) {
            requestedSourceFolder = new PathUtil().getAbsolutePath(getAgentWorkspace(localAgent, job).child(sourceDir).getRemote());
        }
        else {
            requestedSourceFolder = sourceDir;
        }
        job.setDefinition(createPipelineWithSourceCode(EVERY_BUILD, requestedSourceFolder));
        Run<?, ?> firstBuild = buildSuccessfully(job);
        assertThat(getConsoleLog(firstBuild))
                .contains("-> resolved absolute paths for all 1 source files")
                .contains("-> finished painting successfully");
        verifySourceCodeInBuild(sourceDir, firstBuild, ACU_COBOL_PARSER, PATH_UTIL);

        Run<?, ?> secondBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(sourceDir, secondBuild, ACU_COBOL_PARSER, PATH_UTIL);
        verifySourceCodeInBuild(sourceDir, firstBuild, ACU_COBOL_PARSER, PATH_UTIL); // should be still available

        job.setDefinition(createPipelineWithSourceCode(LAST_BUILD, sourceDir));
        Run<?, ?> thirdBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(sourceDir, thirdBuild, ACU_COBOL_PARSER, PATH_UTIL);
        verifySourceCodeInBuild(sourceDir, firstBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(sourceDir, secondBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available

        job.setDefinition(createPipelineWithSourceCode(NEVER, sourceDir));
        Run<?, ?> lastBuild = buildSuccessfully(job);
        verifySourceCodeInBuild(sourceDir, lastBuild, NO_SOURCE_CODE, NO_SOURCE_CODE);
        verifySourceCodeInBuild(sourceDir, firstBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(sourceDir, secondBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available
        verifySourceCodeInBuild(sourceDir, thirdBuild, NO_SOURCE_CODE, NO_SOURCE_CODE); // should be still available

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

    private void verifySourceCodeInBuild(final String pathPrefix, final Run<?, ?> build, final String acuCobolParserSourceCodeSnippet,
            final String pathUtilSourceCodeSnippet) {
        List<CoverageBuildAction> actions = build.getActions(CoverageBuildAction.class);
        var builder = new CoverageBuilder().setMetric(Metric.LINE).setMissed(0);
        assertThat(actions).hasSize(2).satisfiesExactly(
                action -> {
                    assertThat(action.getAllValues(Baseline.PROJECT)).contains(builder.setCovered(8).build());
                    var relativePath = getRelativePath(pathPrefix, ACU_COBOL_PARSER_SOURCE_FILE_PATH);
                    Optional<Node> fileNode = action.getResult().find(Metric.FILE, relativePath);
                    assertThat(fileNode).isNotEmpty().get()
                            .isInstanceOfSatisfying(FileNode.class,
                                    node -> assertThat(node.getRelativePath()).isEqualTo(relativePath));
                    assertThat(action.getTarget().getSourceCode(String.valueOf(relativePath.hashCode()), "coverage-table"))
                            .contains(acuCobolParserSourceCodeSnippet);
                },
                action -> {
                    assertThat(action.getAllValues(Baseline.PROJECT)).contains(builder.setCovered(43).build());
                    var relativePath = getRelativePath(pathPrefix, PATH_UTIL_SOURCE_FILE_PATH);
                    Optional<Node> fileNode = action.getResult().find(Metric.FILE, relativePath);
                    assertThat(fileNode).isNotEmpty().get()
                            .isInstanceOfSatisfying(FileNode.class,
                                    node -> assertThat(node.getRelativePath()).isEqualTo(relativePath));
                    assertThat(action.getTarget().getSourceCode(String.valueOf(relativePath.hashCode()), "coverage-table"))
                            .contains(pathUtilSourceCodeSnippet);
                });
    }

    private String getRelativePath(final String path, final String filePath) {
        return UTIL.getRelativePath(Paths.get(path, filePath));
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
