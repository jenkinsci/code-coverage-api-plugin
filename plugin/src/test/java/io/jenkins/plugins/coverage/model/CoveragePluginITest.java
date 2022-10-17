package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.MutationValue;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.metrics.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.CoverageTool;
import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for different jacoco and cobertura files.
 */
class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    /**
     * Covered lines in {@value JACOCO_ANALYSIS_MODEL_FILE}.
     */
    private static final int JACOCO_COVERED_LINES = 6083;
    /**
     * All lines in {@value JACOCO_ANALYSIS_MODEL_FILE}.
     */
    private static final int JACOCO_ALL_LINES = 6368;
    /**
     * Covered lines in {@value JACOCO_ANALYSIS_MODEL_FILE} and {@value JACOCO_CODINGSTYLE_FILE}.
     */
    private static final int BOTH_JACOCO_COVERED_LINES = 6377;
    /**
     * All lines in {@value JACOCO_ANALYSIS_MODEL_FILE} and {@value JACOCO_CODINGSTYLE_FILE}.
     */
    private static final int BOTH_JACOCO_ALL_LINES = 6691;
    /**
     * Covered lines in {@value COBERTURA_HIGHER_COVERAGE_FILE}.
     */
    private static final int COBERTURA_COVERED_LINES = 2;
    /**
     * All lines in {@value COBERTURA_HIGHER_COVERAGE_FILE}.
     */
    private static final int COBERTURA_ALL_LINES = 2;
    /**
     * Covered lines in {@value JACOCO_ANALYSIS_MODEL_FILE} and {@value COBERTURA_HIGHER_COVERAGE_FILE}.
     */
    private static final int JACOCO_COBERTURA_COVERED_LINES = 6085;
    /**
     * All lines in {@value JACOCO_ANALYSIS_MODEL_FILE} and {@value COBERTURA_HIGHER_COVERAGE_FILE}.
     */
    private static final int JACOCO_COBERTURA_ALL_LINES = 6370;
    /**
     * Jacoco file for testing.
     */
    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    /**
     * Another jacoco file for testing.
     */
    private static final String JACOCO_CODINGSTYLE_FILE = "jacoco-codingstyle.xml";
    /**
     * Cobertura file for testing.
     */
    private static final String COBERTURA_HIGHER_COVERAGE_FILE = "cobertura-higher-coverage.xml";
    /**
     * Another cobertura file for testing.
     */
    private static final String COBERTURA_WITH_LOTS_OF_DATA_FILE = "cobertura-lots-of-data.xml";

    // ---------------------------------------------------------------------------------------
    // vv Converted tests vv
    // ---------------------------------------------------------------------------------------

    @Test
    void shouldFailWithoutParserInFreestyleJob() {
        FreeStyleProject project = createFreeStyleProject();

        project.getPublishersList().add(new CoverageRecorder());

        verifyNoParserError(project);
    }

    @Test
    void shouldFailWithoutParserInPipeline() {
        WorkflowJob job = createPipeline();

        setPipelineScript(job, "recordCoverage()");

        verifyNoParserError(job);
    }

    private void verifyNoParserError(final ParameterizedJob<?, ?> project) {
        Run<?, ?> run = buildWithResult(project, Result.FAILURE);

        assertThat(getConsoleLog(run)).contains("[-ERROR-] No tools defined that will record the coverage files");
    }

    @EnumSource
    @ParameterizedTest(name = "{index} => Freestyle job with parser {0}")
    @DisplayName("Report error but do not fail build in freestyle job when no input files are found")
    void shouldReportErrorWhenNoFilesHaveBeenFoundInFreestyleJob(final CoverageParser parser) {
        FreeStyleProject project = createFreeStyleProject();

        createRecorder(project, parser);

        verifyNoFilesFound(project);
    }

    @EnumSource
    @ParameterizedTest(name = "{index} => Pipeline with parser {0}")
    @DisplayName("Report error but do not fail build in pipeline when no input files are found")
    void shouldReportErrorWhenNoFilesHaveBeenFoundInPipeline(final CoverageParser parser) {
        WorkflowJob job = createPipeline();

        createPipelineFor(job, parser);

        verifyNoFilesFound(job);
    }

    private void verifyNoFilesFound(final ParameterizedJob<?, ?> project) {
        Run<?, ?> run = buildWithResult(project, Result.SUCCESS);

        assertThat(getConsoleLog(run)).contains("[-ERROR-] No files found for pattern '**/*xml'. Configuration error?");
    }

    @Test
    void shouldRecordOneJacocoResultInFreestyleJob() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);

        createRecorder(project, CoverageParser.JACOCO);

        // FIXME: which parser is correct?
        /*
            Expected :LINE: 95.52% (6083/6368)
            Actual   :LINE: 95.41% (5588/5857)
         */
        verifyOneJacocoResult(project);
    }

    @Test
    void shouldRecordOneJacocoResultInPipeline() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);

        createPipelineFor(job, CoverageParser.JACOCO);

        verifyOneJacocoResult(job);
    }

    private void verifyOneJacocoResult(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(createLineCoverageBuilder()
                        .setCovered(JACOCO_COVERED_LINES)
                        .setMissed(JACOCO_ALL_LINES - JACOCO_COVERED_LINES)
                        .build());
    }

    @Test
    void shouldRecordTwoJacocoResultsInFreestyleJob() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODINGSTYLE_FILE);

        createRecorder(project, CoverageParser.JACOCO);

        // FIXME: which parser is correct?
        /*
            Expected :LINE: 95.31% (6377/6691)
            Actual   :LINE: 95.18% (5882/6180)
         */
        verifyTwoJacocoResults(project);
    }

    @Test
    void shouldRecordTwoJacocoResultsInPipeline() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODINGSTYLE_FILE);

        createPipelineFor(job, CoverageParser.JACOCO);

        verifyTwoJacocoResults(job);
    }

    private void verifyTwoJacocoResults(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(createLineCoverageBuilder()
                        .setCovered(BOTH_JACOCO_COVERED_LINES)
                        .setMissed(BOTH_JACOCO_ALL_LINES - BOTH_JACOCO_COVERED_LINES)
                        .build());
    }

    private void setPipelineScript(final WorkflowJob job, final String recorderSnippet) {
        job.setDefinition(new CpsFlowDefinition(
                "node {\n"
                        + recorderSnippet + "\n"
                        + " }\n", true));
    }

    private void createPipelineFor(final WorkflowJob job, final CoverageParser parser) {
        setPipelineScript(job, "recordCoverage tools: [[parser: '" + parser.name() + "', pattern: '**/*xml']]");
    }

    private void createRecorder(final FreeStyleProject project, final CoverageParser parser) {
        CoverageRecorder recorder = new CoverageRecorder();
        var tool = new CoverageTool();
        tool.setParser(parser);
        tool.setPattern("**/*xml");
        recorder.setTools(List.of(tool));
        project.getPublishersList().add(recorder);
    }

    @Test
    void shouldRecordOneCoberturaResultInFreestyleJob() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE_FILE);

        createRecorder(project, CoverageParser.COBERTURA);

        // FIXME: all parsers should only fail for mandatory properties (complexity is only optional)
        verifyOneCoberturaResult(project);
    }

    private void verifyOneCoberturaResult(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new CoverageBuilder().setMetric(Metric.LINE).setCovered(COBERTURA_COVERED_LINES)
                        .setMissed(COBERTURA_ALL_LINES - COBERTURA_COVERED_LINES)
                        .build());
    }

    @Test
    void shouldRecordOnePitResultInFreestyleJob() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles("mutations.xml");

        createRecorder(project, CoverageParser.PIT);

        verifyOnePitResult(project);
    }

    private void verifyOnePitResult(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getCoverage(Metric.MUTATION))
                .isInstanceOfSatisfying(MutationValue.class, m -> {
                    assertThat(m.getKilled()).isEqualTo(222);
                    assertThat(m.getTotal()).isEqualTo(246);
                });

    }

    private static CoverageBuilder createLineCoverageBuilder() {
        return new CoverageBuilder().setMetric(Metric.LINE);
    }

    // ---------------------------------------------------------------------------------------
    // ^^ Converted tests ^^
    // ---------------------------------------------------------------------------------------

    /**
     * Pipeline integration test with one cobertura file.
     */
    @Test
    void pipelineForOneCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE_FILE);

        createPipelineFor(job, CoverageParser.COBERTURA);

        verifyOneCoberturaResult(job);
    }

    /**
     * Pipeline integration test with two cobertura files.
     */
    @Test
    void pipelineForTwoCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE_FILE,
                COBERTURA_WITH_LOTS_OF_DATA_FILE);

        createPipelineFor(job, CoverageParser.COBERTURA);

        verifyForTwoCobertura(job);
    }

    /**
     * Freestyle integration test with two cobertura files.
     */
    @Disabled("Bug")
    @Test
    void freestyleForTwoCobertura() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_HIGHER_COVERAGE_FILE, COBERTURA_WITH_LOTS_OF_DATA_FILE);
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_HIGHER_COVERAGE_FILE);
        CoberturaReportAdapter coberturaReportAdapter2 = new CoberturaReportAdapter(COBERTURA_WITH_LOTS_OF_DATA_FILE);

        coverageAdapters.add(coberturaReportAdapter);
        coverageAdapters.add(coberturaReportAdapter2);
        coveragePublisher.setAdapters(coverageAdapters);
        project.getPublishersList().add(coveragePublisher);

        verifyForTwoCobertura(project);
    }

    /**
     * Pipeline integration test with one cobertura and one jacoco file.
     */
    @Test
    void pipelineForOneCoberturaAndOneJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, COBERTURA_HIGHER_COVERAGE_FILE);
        setPipelineScript(job, "node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml'), istanbulCoberturaAdapter('**/*.xml')]"
                + "}");

        verifyForOneCoberturaAndOneJacoco(job);
    }

    /**
     * Freestyle integration test with one cobertura and one jacoco file.
     */
    @Test
    void freestyleForOneCoberturaAndOneJacoco() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE, COBERTURA_HIGHER_COVERAGE_FILE);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE);
        coverageAdapters.add(jacocoReportAdapter);

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_HIGHER_COVERAGE_FILE);
        coverageAdapters.add(coberturaReportAdapter);

        coveragePublisher.setAdapters(coverageAdapters);
        project.getPublishersList().add(coveragePublisher);

        verifyForOneCoberturaAndOneJacoco(project);
    }

    /**
     * Verifies project with two cobertura files.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForTwoCobertura(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        //FIXME
        assertThat(coverageResult.getLineCoverage()).isEqualTo(
                new Coverage.CoverageBuilder().setCovered(472).setMissed(722 - 472).build());
    }

    /**
     * Verifies project with one cobertura and one jacoco file.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForOneCoberturaAndOneJacoco(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage.CoverageBuilder().setCovered(JACOCO_COBERTURA_COVERED_LINES)
                        .setMissed(JACOCO_COBERTURA_ALL_LINES - JACOCO_COBERTURA_COVERED_LINES)
                        .build());
    }

}
