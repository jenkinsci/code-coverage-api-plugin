package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.MutationValue;
import edu.hm.hafner.metric.Value;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageITest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.CoverageParser;

import static edu.hm.hafner.metric.Metric.*;
import static io.jenkins.plugins.coverage.metrics.AbstractCoverageTest.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for different JaCoCo, Cobertura, and PIT files.
 */
class CoveragePluginITest extends AbstractCoverageITest {
    private static final String COBERTURA_HIGHER_COVERAGE_FILE = "cobertura-higher-coverage.xml";
    private static final int COBERTURA_COVERED_LINES = 2;
    private static final int COBERTURA_MISSED_LINES = 0;
    private static final String NO_FILES_FOUND_ERROR_MESSAGE = "[-ERROR-] No files found for pattern '**/*xml'. Configuration error?";

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

        verifyLogMessageThatNoFilesFound(project);
    }

    @EnumSource
    @ParameterizedTest(name = "{index} => Pipeline with parser {0}")
    @DisplayName("Report error but do not fail build in pipeline when no input files are found")
    void shouldReportErrorWhenNoFilesHaveBeenFoundInPipeline(final CoverageParser parser) {
        WorkflowJob job = createPipeline(parser);

        verifyLogMessageThatNoFilesFound(job);
    }

    private void verifyLogMessageThatNoFilesFound(final ParameterizedJob<?, ?> project) {
        Run<?, ?> run = buildWithResult(project, Result.SUCCESS);

        assertThat(getConsoleLog(run)).contains(NO_FILES_FOUND_ERROR_MESSAGE,
                "Ignore errors and continue processing");
    }

    @EnumSource
    @ParameterizedTest(name = "{index} => Freestyle job with parser {0}")
    @DisplayName("Report error and fail build in freestyle job when no input files are found")
    void shouldFailBuildWhenNoFilesHaveBeenFoundInFreestyleJob(final CoverageParser parser) {
        FreeStyleProject project = createFreestyleJob(parser, r -> r.setFailOnError(true));

        verifyFailureWhenNoFilesFound(project);
    }

    @EnumSource
    @ParameterizedTest(name = "{index} => Pipeline with parser {0}")
    @DisplayName("Report error and fail build in pipeline when no input files are found")
    void shouldFailBuildWhenNoFilesHaveBeenFoundInPipeline(final CoverageParser parser) {
        WorkflowJob job = createPipeline();

        setPipelineScript(job,
                "recordCoverage tools: [[parser: '" + parser.name() + "', pattern: '**/*xml']], "
                        + "failOnError: 'true'");

        verifyFailureWhenNoFilesFound(job);
    }

    private void verifyFailureWhenNoFilesFound(final ParameterizedJob<?, ?> project) {
        Run<?, ?> run = buildWithResult(project, Result.FAILURE);

        assertThat(getConsoleLog(run)).contains(NO_FILES_FOUND_ERROR_MESSAGE,
                "Failing build due to some errors during recording of the coverage");
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

        verifyJaCoCoAction(build.getAction(CoverageBuildAction.class));
    }

    private static void verifyJaCoCoAction(final CoverageBuildAction coverageResult) {
        assertThat(coverageResult.getAllValues(Baseline.PROJECT)).extracting(Value::getMetric)
                .containsExactly(MODULE,
                        PACKAGE,
                        Metric.FILE,
                        Metric.CLASS,
                        METHOD,
                        LINE,
                        INSTRUCTION,
                        BRANCH,
                        COMPLEXITY,
                        COMPLEXITY_DENSITY,
                        LOC);
        assertThat(coverageResult.getMetricsForSummary())
                .containsExactly(Metric.LINE, Metric.BRANCH, COMPLEXITY_DENSITY, Metric.LOC);
        assertThat(coverageResult.getAllValues(Baseline.PROJECT))
                .contains(createLineCoverageBuilder()
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
        assertThat(coverageResult.getAllValues(Baseline.PROJECT))
                .contains(createLineCoverageBuilder()
                        .setCovered(JACOCO_ANALYSIS_MODEL_COVERED + JACOCO_CODING_STYLE_COVERED)
                        .setMissed(JACOCO_ANALYSIS_MODEL_MISSED + JACOCO_CODING_STYLE_MISSED)
                        .build());
    }

    @Test
    void shouldRecordOneCoberturaResultInFreestyleJob() {
        FreeStyleProject project = createFreestyleJob(CoverageParser.COBERTURA, COBERTURA_HIGHER_COVERAGE_FILE);

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

        verifyCoberturaAction(build.getAction(CoverageBuildAction.class));
    }

    private static void verifyCoberturaAction(final CoverageBuildAction coverageResult) {
        assertThat(coverageResult.getAllValues(Baseline.PROJECT))
                .contains(new CoverageBuilder().setMetric(Metric.LINE).setCovered(COBERTURA_COVERED_LINES)
                        .setMissed(COBERTURA_MISSED_LINES)
                        .build());
    }

    @Test
    void shouldRecordCoberturaAndJacocoResultsInFreestyleJob() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE,
                COBERTURA_HIGHER_COVERAGE_FILE);

        CoverageRecorder recorder = new CoverageRecorder();

        var cobertura = new CoverageTool();
        cobertura.setParser(CoverageParser.COBERTURA);
        cobertura.setPattern(COBERTURA_HIGHER_COVERAGE_FILE);

        var jacoco = new CoverageTool();
        jacoco.setParser(CoverageParser.JACOCO);
        jacoco.setPattern(JACOCO_ANALYSIS_MODEL_FILE);

        recorder.setTools(List.of(jacoco, cobertura));
        project.getPublishersList().add(recorder);

        verifyForOneCoberturaAndOneJacoco(project);
    }

    @Test
    void shouldRecordCoberturaAndJacocoResultsInPipeline() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, COBERTURA_HIGHER_COVERAGE_FILE);

        setPipelineScript(job,
                "recordCoverage tools: ["
                        + "[parser: 'COBERTURA', pattern: '" + COBERTURA_HIGHER_COVERAGE_FILE + "'],"
                        + "[parser: 'JACOCO', pattern: '" + JACOCO_ANALYSIS_MODEL_FILE + "']"
                        + "]");

        verifyForOneCoberturaAndOneJacoco(job);
    }

    @Test
    void shouldRecordCoberturaAndJacocoResultsInDeclarativePipeline() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, COBERTURA_HIGHER_COVERAGE_FILE);

        job.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "    agent any\n"
                + "    stages {\n"
                + "        stage('Test') {\n"
                + "            steps {\n"
                + "                 recordCoverage(tools: [\n"
                + "                     [parser: 'COBERTURA', pattern: '" + COBERTURA_HIGHER_COVERAGE_FILE + "'],\n"
                + "                     [parser: 'JACOCO', pattern: '" + JACOCO_ANALYSIS_MODEL_FILE + "']\n"
                + "                 ])\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}", true));

        verifyForOneCoberturaAndOneJacoco(job);
    }

    private void verifyForOneCoberturaAndOneJacoco(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getAllValues(Baseline.PROJECT))
                .contains(createLineCoverageBuilder()
                        .setCovered(JACOCO_ANALYSIS_MODEL_COVERED + COBERTURA_COVERED_LINES)
                        .setMissed(JACOCO_ANALYSIS_MODEL_MISSED)
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
        assertThat(coverageResult.getAllValues(Baseline.PROJECT))
                .filteredOn(Value::getMetric, MUTATION)
                .first()
                .isInstanceOfSatisfying(MutationValue.class, m -> {
                    assertThat(m.getKilled()).isEqualTo(222);
                    assertThat(m.getTotal()).isEqualTo(246);
                });
    }

    private static CoverageBuilder createLineCoverageBuilder() {
        return new CoverageBuilder().setMetric(Metric.LINE);
    }

    @Test
    void shouldRecordResultsWithDifferentId() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, COBERTURA_HIGHER_COVERAGE_FILE);

        setPipelineScript(job,
                "recordCoverage "
                        + "tools: [[parser: 'COBERTURA', pattern: '" + COBERTURA_HIGHER_COVERAGE_FILE + "']],"
                        + "id: 'cobertura', name: 'Cobertura Results'\n"
                        + "recordCoverage "
                        + "tools: ["
                        + "[parser: 'JACOCO', pattern: '" + JACOCO_ANALYSIS_MODEL_FILE + "']],"
                        + "id: 'jacoco', name: 'JaCoCo Results'\n");

        Run<?, ?> build = buildSuccessfully(job);

        List<CoverageBuildAction> coverageResult = build.getActions(CoverageBuildAction.class);
        assertThat(coverageResult).hasSize(2);

        assertThat(coverageResult).element(0).satisfies(
                a -> {
                    assertThat(a.getUrlName()).isEqualTo("cobertura");
                    assertThat(a.getDisplayName()).isEqualTo("Cobertura Results");
                    verifyCoberturaAction(a);
                }
        );
        assertThat(coverageResult).element(1).satisfies(
                a -> {
                    assertThat(a.getUrlName()).isEqualTo("jacoco");
                    assertThat(a.getDisplayName()).isEqualTo("JaCoCo Results");
                    verifyJaCoCoAction(a);
                });

        // FIXME: verify that two different trend charts are returned!
    }
}
