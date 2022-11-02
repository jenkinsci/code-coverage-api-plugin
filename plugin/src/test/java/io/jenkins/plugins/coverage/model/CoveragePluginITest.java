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
import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for different jacoco and cobertura files.
 */
class CoveragePluginITest extends AbstractCoverageITest {
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
        FreeStyleProject project = createFreestyleJob(parser);

        verifyNoFilesFound(project);
    }

    @EnumSource
    @ParameterizedTest(name = "{index} => Pipeline with parser {0}")
    @DisplayName("Report error but do not fail build in pipeline when no input files are found")
    void shouldReportErrorWhenNoFilesHaveBeenFoundInPipeline(final CoverageParser parser) {
        WorkflowJob job = createPipeline(parser);

        verifyNoFilesFound(job);
    }

    private void verifyNoFilesFound(final ParameterizedJob<?, ?> project) {
        Run<?, ?> run = buildWithResult(project, Result.SUCCESS);

        assertThat(getConsoleLog(run)).contains("[-ERROR-] No files found for pattern '**/*xml'. Configuration error?");
    }

    @Test
    void shouldRecordOneJacocoResultInFreestyleJob() {
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);

        verifyOneJacocoResult(project);
    }

    @Test
    void shouldRecordOneJacocoResultInPipeline() {
        WorkflowJob job = createPipeline(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);

        verifyOneJacocoResult(job);
    }

    @Test
    void shouldRecordOneJacocoResultInDeclarativePipeline() {
        WorkflowJob job = createDeclarativePipeline(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);

        verifyOneJacocoResult(job);
    }

    private void verifyOneJacocoResult(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getMetricsForSummary())
                .containsExactly(Metric.LINE, Metric.BRANCH, Metric.COMPLEXITY, Metric.LOC);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(createLineCoverageBuilder()
                        .setCovered(JACOCO_ANALYSIS_MODEL_COVERED)
                        .setMissed(JACOCO_ANALYSIS_MODEL_TOTAL - JACOCO_ANALYSIS_MODEL_COVERED)
                        .build());
    }

    @Test
    void shouldRecordTwoJacocoResultsInFreestyleJob() {
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO,
                JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODING_STYLE_FILE);
        verifyTwoJacocoResults(project);
    }

    @Test
    void shouldRecordTwoJacocoResultsInPipeline() {
        WorkflowJob job = createPipeline(CoverageParser.JACOCO,
                JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODING_STYLE_FILE);

        verifyTwoJacocoResults(job);
    }

    @Test
    void shouldRecordTwoJacocoResultsInDeclarativePipeline() {
        WorkflowJob job = createDeclarativePipeline(CoverageParser.JACOCO,
                JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODING_STYLE_FILE);

        verifyTwoJacocoResults(job);
    }

    private void verifyTwoJacocoResults(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(createLineCoverageBuilder()
                        .setCovered(JACOCO_ANALYSIS_MODEL_COVERED + JACOCO_CODING_STYLE_COVERED)
                        .setMissed(JACOCO_ANALYSIS_MODEL_MISSED + JACOCO_CODING_STYLE_MISSED)
                        .build());
    }

    @Test
    void shouldRecordOneCoberturaResultInFreestyleJob() {
        FreeStyleProject project = createFreestyleJob(CoverageParser.COBERTURA, COBERTURA_HIGHER_COVERAGE_FILE);

        // FIXME: all parsers should only fail for mandatory properties (complexity is only optional)
        verifyOneCoberturaResult(project);
    }

    @Test
    void shouldRecordOneCoberturaResultInPipeline() {
        WorkflowJob job = createPipeline(CoverageParser.COBERTURA, COBERTURA_HIGHER_COVERAGE_FILE);

        verifyOneCoberturaResult(job);
    }

    @Test
    void shouldRecordOneCoberturaResultInDeclarativePipeline() {
        WorkflowJob job = createDeclarativePipeline(CoverageParser.COBERTURA, COBERTURA_HIGHER_COVERAGE_FILE);

        verifyOneCoberturaResult(job);
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
        FreeStyleProject project = createFreestyleJob(CoverageParser.PIT, "mutations.xml");

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
     * Pipeline integration test with two cobertura files.
     */
    @Test
    void pipelineForTwoCobertura() {
        WorkflowJob job = createPipeline(CoverageParser.COBERTURA,
                COBERTURA_HIGHER_COVERAGE_FILE, COBERTURA_WITH_LOTS_OF_DATA_FILE);

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

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                JACOCO_ANALYSIS_MODEL_FILE);
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
