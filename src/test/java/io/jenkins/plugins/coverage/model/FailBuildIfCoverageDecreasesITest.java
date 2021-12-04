package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Integration test for checking if build failes when coverage decreases.
 */
public class FailBuildIfCoverageDecreasesITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String COBERTURA_HIGHER_COVERAGE = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_LOWER_COVERAGE = "cobertura-lower-coverage.xml";

    /**
     * Integration test for checking if build failes when coverage decreases.
     *
     * @throws IOException
     *         if creating Project throws Exception
     */
    @Test
    public void shouldReturnSuccessAndFailureDependingOnFailBuildIfCoverageDecreases() throws IOException {
        FreeStyleProject freestyleProjectIfDecreasesSetFailTrue = createFreestyleProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, true);
        buildWithResult(freestyleProjectIfDecreasesSetFailTrue, Result.FAILURE);
        FreeStyleProject projectIfDecreasesSetFailFalse = createFreestyleProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, false);
        buildWithResult(projectIfDecreasesSetFailFalse, Result.SUCCESS);
        WorkflowJob pipelineProjectIfDecreasesSetFailTrue = createPipelineProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, true);
        buildWithResult(pipelineProjectIfDecreasesSetFailTrue, Result.FAILURE);

        WorkflowJob pipelineProjectIfDecreasesSetFailFalse = createPipelineProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, false);
        buildWithResult(pipelineProjectIfDecreasesSetFailFalse, Result.SUCCESS);
    }

    /**
     * Creates freestyle-project with second build containing decreased coverage.
     *
     * @param filename
     *         with higher coverage
     * @param filenameOfDecreasedCoverage
     *         with decreased coverage
     * @param setFailIfCoverageDecreased
     *         to set if build should fail when coverage decreases
     *
     * @return {@link FreeStyleProject} with decreased Coverage
     * @throws IOException
     *         if publisher list of project is missing
     */
    FreeStyleProject createFreestyleProjectWithDecreasedCoverage(final String filename,
            final String filenameOfDecreasedCoverage,
            final boolean setFailIfCoverageDecreased)
            throws IOException {

        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, filename);
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        //set FailBuildIfCoverageDecreasedInChangeRequest on true
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(setFailIfCoverageDecreased);

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(filename);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        //run first build
        Run<?, ?> firstBuild = buildSuccessfully(project);

        //prepare second build
        copyFilesToWorkspace(project, filenameOfDecreasedCoverage);

        CoberturaReportAdapter reportAdapter2 = new CoberturaReportAdapter(
                filenameOfDecreasedCoverage);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(setFailIfCoverageDecreased);

        coveragePublisher.setAdapters(Collections.singletonList(reportAdapter2));
        project.getPublishersList().replace(coveragePublisher);

        return project;
    }

    /**
     * Creates pipeline-project with second build containing decreased coverage.
     *
     * @param filename
     *         with higher coverage
     * @param filenameOfDecreasedCoverage
     *         with decreased coverage
     * @param setFailIfCoverageDecreased
     *         to set if build should fail when coverage decreases
     *
     * @return {@link WorkflowJob} with decreased Coverage
     */
    WorkflowJob createPipelineProjectWithDecreasedCoverage(final String filename,
            final String filenameOfDecreasedCoverage,
            final boolean setFailIfCoverageDecreased) {

        WorkflowJob job = createPipelineWithWorkspaceFiles(filename, filenameOfDecreasedCoverage);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + filename + "')],"
                + "   failBuildIfCoverageDecreasedInChangeRequest: " + setFailIfCoverageDecreased
                + "}", true));
        buildSuccessfully(job);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + filenameOfDecreasedCoverage + "')],"
                + "   failBuildIfCoverageDecreasedInChangeRequest: " + setFailIfCoverageDecreased
                + "}", true));

        return job;
    }

}


