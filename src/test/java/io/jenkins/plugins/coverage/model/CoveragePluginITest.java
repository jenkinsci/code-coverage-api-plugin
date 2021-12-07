package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

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
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    public static final int JACOCO_MISSED_LINES = 6083;
    public static final int JACOCO_ALL_LINES = 6368;
    public static final int BOTH_JACOCO_MISSED_LINES = 6377;
    public static final int BOTH_JACOCO_ALL_LINES = 6691;
    public static final int COBERTURA_COVERED_LINES = 2;
    public static final int COBERTURA_ALL_LINES = 2;
    public static final int JACOCO_COBERTURA_COVERED_LINES = 6085;
    public static final int JACOCO_COBERTURA_ALL_LINES = 6370;
    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_FILE = "jacoco-codingstyle.xml";
    private static final String COBERTURA_HIGHER_COVERAGE_FILE = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_WITH_LOTS_OF_DATA_FILE = "../coverage-with-lots-of-data.xml";

    /** Example integration test for a pipeline with code coverage. */
    @Test
    public void pipelineForOneJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);
        job.setDefinition(getCpsFlowDefinitionWithJacocoAdapter());

        verifyForOneJacoco(job);
    }

    /**
     * Pipeline integration test with no file.
     */
    @Test
    public void pipelineForNoJacoco() {
        WorkflowJob job = createPipeline();
        job.setDefinition(getCpsFlowDefinitionWithJacocoAdapter());

        verifyForNoFile(job);
    }

    /**
     * Pipeline integration test with two jacoco files.
     */
    @Test
    public void pipelineForTwoJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODINGSTYLE_FILE);
        job.setDefinition(getCpsFlowDefinitionWithJacocoAdapter());

        verifyForTwoJacoco(job);
    }

    /**
     * Pipeline integration test with no adapter.
     */
    @Test
    public void pipelineForNoAdapter() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: []"
                + "}", true));

        verifyForNoAdapter(job);
    }

    /**
     * Freestyle integration test with one jacoco file.
     */
    @Test
    public void freestyleForOneJacoco() {
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
    public void freestyleForTwoJacoco() {

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
     * Freestyle integration test with no adapter.
     */
    @Test
    public void freestyleForNoAdapter() {
        FreeStyleProject project = createFreeStyleProject();
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        project.getPublishersList().add(coveragePublisher);
        verifyForNoAdapter(project);
    }

    /**
     * Freestyle integration test with one cobertura file.
     */
    @Test
    public void freestyleForOneCobertura() {
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
    @Test
    public void freestyleForTwoCobertura() {

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
     * Pipeline integration test with one cobertura file.
     */
    @Test
    public void pipelineForOneCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_HIGHER_COVERAGE_FILE);
        job.setDefinition(getCpsFlowDefinitionWithCoberturaAdapter());

        verifyForOneCobertura(job);
    }

    /**
     * Pipeline integration test with two cobertura files.
     */
    @Test
    public void pipelineForTwoCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_WITH_LOTS_OF_DATA_FILE,
                COBERTURA_HIGHER_COVERAGE_FILE);
        job.setDefinition(getCpsFlowDefinitionWithCoberturaAdapter());

        verifyForTwoCobertura(job);
    }

    /**
     * Freestyle integration test with one cobertura and one jacoco file.
     */
    @Test
    public void freestyleForOneCoberturaAndOneJacoco() {
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
     * Pipeline integration test with one cobertura and one jacoco file.
     */
    @Test
    public void pipelineForOneCoberturaAndOneJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, COBERTURA_HIGHER_COVERAGE_FILE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml'), istanbulCoberturaAdapter('**/*.xml')]"
                + "}", true));

        verifyForOneCoberturaAndOneJacoco(job);
    }

    /**
     * Creates a script with jacoco adapter set to wildcard.
     *
     * @return {@link CpsFlowDefinition} with set jacoco adapter
     */
    private CpsFlowDefinition getCpsFlowDefinitionWithJacocoAdapter() {
        return new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true);
    }

    /**
     * Creates a script with cobertura adapter set to wildcard.
     *
     * @return {@link CpsFlowDefinition} with set cobertura adapter
     */
    private CpsFlowDefinition getCpsFlowDefinitionWithCoberturaAdapter() {
        return new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('**/*.xml')]"
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

        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(JACOCO_MISSED_LINES, JACOCO_ALL_LINES - JACOCO_MISSED_LINES));
    }

    /**
     * Verifies project with two jacoco files.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForTwoJacoco(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(BOTH_JACOCO_MISSED_LINES, BOTH_JACOCO_ALL_LINES - BOTH_JACOCO_MISSED_LINES));
    }

    /**
     * Verifies project with no adapter.
     *
     * @param project
     *         the project with no adapter
     */
    private void verifyForNoAdapter(final ParameterizedJob<?, ?> project) {
        //TODO: Build should fail
        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    /**
     * Verifies project with no files.
     *
     * @param project
     *         the project with no files
     */
    private void verifyForNoFile(final ParameterizedJob<?, ?> project) {

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    /**
     * Verifies project with one cobertura file.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForOneCobertura(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);
        assertThat(build.getNumber()).isEqualTo(1);

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
        assertThat(build.getNumber()).isEqualTo(1);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        //TODO
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(COBERTURA_COVERED_LINES, COBERTURA_ALL_LINES - COBERTURA_COVERED_LINES));
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
