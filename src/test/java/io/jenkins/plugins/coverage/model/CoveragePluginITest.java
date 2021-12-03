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

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    private static final String JACOCO_FILE_NAME_2 = "jacoco-codingstyle.xml";

    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "coverage-with-lots-of-data.xml";

    /** Example integration test for a pipeline with code coverage. */
    @Test
    public void pipelineForOneJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifyForOneJacoco(job);
    }

    /**
     * Pipeline integration test with no file.
     */
    @Test
    public void pipelineForNoJacoco() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifyForNoFile(job);
    }

    /**
     * Pipeline integration test with two jacoco files.
     */
    @Test
    public void pipelineForTwoJacoco() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME, JACOCO_FILE_NAME_2);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifyForTwoJacoco(job);
    }

    /**
     * Pipeline integration test with no adapter.
     */
    @Test
    public void pipelineForNoAdapter() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "}", true));

        verifyForNoAdapter(job);
    }

    /**
     * Freestyle integration test with one jacoco file.
     */
    @Test
    public void freestyleForOneJacoco() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
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
        copyFilesToWorkspace(project, JACOCO_FILE_NAME, JACOCO_FILE_NAME_2);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        JacocoReportAdapter jacocoReportAdapter2 = new JacocoReportAdapter(JACOCO_FILE_NAME_2);
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
        verifyForNoAdapter(project);
    }

    /**
     * Freestyle integration test with one cobertura file.
     */
    @Test
    public void freestyleForOneCobertura() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
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
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        CoberturaReportAdapter coberturaReportAdapter2 = new CoberturaReportAdapter(COBERTURA_FILE_NAME_2);

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
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('**/*.xml')]"
                + "}", true));

        verifyForOneCobertura(job);
    }

    /**
     * Pipeline integration test with two cobertura files.
     */
    @Test
    public void pipelineForTwoCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_NAME_2, COBERTURA_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('**/*.xml')]"
                + "}", true));

        verifyForTwoCobertura(job);
    }

    /**
     * Freestyle integration test with one cobertura and one jacoco file.
     */
    @Test
    public void freestyleForOneCoberturaAndOneJacoco() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME, COBERTURA_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coverageAdapters.add(jacocoReportAdapter);

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
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
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME, COBERTURA_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml'), istanbulCoberturaAdapter('**/*.xml')]"
                + "}", true));

        verifyForOneCoberturaAndOneJacoco(job);
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
                .isEqualTo(new Coverage(6083, 6368 - 6083));
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
                .isEqualTo(new Coverage(6377, 6691 - 6377));
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
                .isEqualTo(new Coverage(2, 0));
        //CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

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
        //CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        //TODO
        //assertThat(coverageResult.getLineCoverage())
        //        .isEqualTo(new Coverage(472, 246));
    }

    /**
     * Verifies project with one cobertura and one jacoco file.
     *
     * @param project
     *         the project with added files
     */
    private void verifyForOneCoberturaAndOneJacoco(final ParameterizedJob<?, ?> project) {
        Run<?, ?> build = buildSuccessfully(project);
        //CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        //TODO
        //assertThat(coverageResult.getLineCoverage())
        //        .isEqualTo(new Coverage(6085, 285));
    }

}
