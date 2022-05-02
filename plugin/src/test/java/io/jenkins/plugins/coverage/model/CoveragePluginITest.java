package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
    /**
     * Symbol of cobertura adapter in pipeline.
     */
    private static final String COBERTURA_ADAPTER = "istanbulCoberturaAdapter";
    /**
     * Symbol of jacoco adapter in pipeline.
     */
    private static final String JACOCO_ADAPTER = "jacocoAdapter";

    /**
     * Pipeline integration test with no adapter.
     */
    @Disabled("Bug")
    @Test
    void pipelineForNoAdapter() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: []"
                + "}", true));

        verifyForNoAdapter(job);
    }

    /**
     * Freestyle integration test with no adapter.
     */
    @Disabled("Bug")
    @Test
    void freestyleForNoAdapter() {
        FreeStyleProject project = createFreeStyleProject();
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        project.getPublishersList().add(coveragePublisher);
        verifyForNoAdapter(project);
    }

    /**
     * Pipeline integration test with no file.
     */
    @Test
    void pipelineForNoJacoco() {
        WorkflowJob job = createPipeline();
        job.setDefinition(getCpsFlowDefinitionWithAdapter(JACOCO_ADAPTER));

        verifyForNoFile(job);
    }

    /** Example integration test for a pipeline with code coverage. */
    @Test
    void pipelineForOneJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);
        job.setDefinition(getCpsFlowDefinitionWithAdapter(JACOCO_ADAPTER));

        verifyForOneJacoco(job);
    }

    /**
     * Pipeline integration test with two jacoco files.
     */
    @Test
    void pipelineForTwoJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODINGSTYLE_FILE);
        job.setDefinition(getCpsFlowDefinitionWithAdapter(JACOCO_ADAPTER));

        verifyForTwoJacoco(job);
    }

    /**
     * Freestyle integration test with no jacoco file.
     */
    @Test
    void freestyleForNoJacoco() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifyForNoFile(project);
    }

    /**
     * Freestyle integration test with one jacoco file.
     */
    @Test
    void freestyleForOneJacoco() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifyForOneJacoco(project);
    }

    /**
     * Freestyle integration test with two jacoco files.
     */
    @Test
    void freestyleForTwoJacoco() {

        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODINGSTYLE_FILE);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE);
        JacocoReportAdapter jacocoReportAdapter2 = new JacocoReportAdapter(JACOCO_CODINGSTYLE_FILE);
        List<CoverageAdapter> reportAdapters = new ArrayList<>();
        reportAdapters.add(jacocoReportAdapter);
        reportAdapters.add(jacocoReportAdapter2);
        coveragePublisher.setAdapters(reportAdapters);
        project.getPublishersList().add(coveragePublisher);

        verifyForTwoJacoco(project);
    }

    /**
     * Pipeline integration test with no cobertura file.
     */
    @Test
    void pipelineForNoCobertura() {
        WorkflowJob job = createPipeline();
        job.setDefinition(getCpsFlowDefinitionWithAdapter(COBERTURA_ADAPTER));

        verifyForNoFile(job);
    }

    /**
     * Pipeline integration test with one cobertura file.
     */
    @Test
    void pipelineForOneCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE_FILE);
        job.setDefinition(getCpsFlowDefinitionWithAdapter(COBERTURA_ADAPTER));

        verifyForOneCobertura(job);
    }

    /**
     * Pipeline integration test with two cobertura files.
     */
    @Test
    void pipelineForTwoCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE_FILE,
                COBERTURA_WITH_LOTS_OF_DATA_FILE);

        job.setDefinition(getCpsFlowDefinitionWithAdapter(COBERTURA_ADAPTER));

        verifyForTwoCobertura(job);
    }

    /**
     * Freestyle integration test with no cobertura file.
     */
    @Test
    void freestyleForNoCobertura() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("");
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifyForNoFile(project);
    }

    /**
     * Freestyle integration test with one cobertura file.
     */
    @Test
    void freestyleForOneCobertura() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_HIGHER_COVERAGE_FILE);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_HIGHER_COVERAGE_FILE);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifyForOneCobertura(project);
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
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml'), istanbulCoberturaAdapter('**/*.xml')]"
                + "}", true));

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
     * Creates a script with adapter set to wildcard.
     *
     * @param adapter
     *         publish coverage adapter
     *
     * @return {@link CpsFlowDefinition} with set jacoco adapter
     */
    private CpsFlowDefinition getCpsFlowDefinitionWithAdapter(final String adapter) {
        return new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [" + adapter + "('**/*.xml')]"
                + "}", true);
    }

    /**
     * Verifies project with one jacoco file.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForOneJacoco(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(JACOCO_COVERED_LINES, JACOCO_ALL_LINES - JACOCO_COVERED_LINES));
    }

    /**
     * Verifies project with two jacoco files.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForTwoJacoco(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(BOTH_JACOCO_COVERED_LINES, BOTH_JACOCO_ALL_LINES - BOTH_JACOCO_COVERED_LINES));
    }

    /**
     * Verifies project with no adapter.
     *
     * @param project
     *         the project with no adapter
     */
    private void verifyForNoAdapter(final ParameterizedJob<?, ?> project) {
        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Verifies project with no files.
     *
     * @param project
     *         the project with no files
     */
    private void verifyForNoFile(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);
        assertThat(action).isEqualTo(null);
    }

    /**
     * Verifies project with one cobertura file.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForOneCobertura(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(COBERTURA_COVERED_LINES, COBERTURA_ALL_LINES - COBERTURA_COVERED_LINES));

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
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(472, 722 - 472));
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
                .isEqualTo(new Coverage(JACOCO_COBERTURA_COVERED_LINES,
                        JACOCO_COBERTURA_ALL_LINES - JACOCO_COBERTURA_COVERED_LINES));
    }

}
