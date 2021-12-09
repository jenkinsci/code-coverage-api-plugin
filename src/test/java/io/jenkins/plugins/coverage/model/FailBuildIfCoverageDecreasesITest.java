package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

enum CoverageDecreasedAction { FAIL_BUILD, DONT_FAIL_BUILD}

/**
 * Integration test for checking if build failes when coverage decreases.
 */
public class FailBuildIfCoverageDecreasesITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String COBERTURA_HIGHER_COVERAGE = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_LOWER_COVERAGE = "cobertura-lower-coverage.xml";

    /**
     * Integration test freestyle projects for checking if build failes when coverage decreases.
     *
     * @throws IOException
     *         if creating Project throws Exception
     */
    @Test
    public void freestyleProjectTestBuildResultDependingOnFailBuildIfCoverageDecreases() throws IOException {
        FreeStyleProject freestyleProjectIfDecreasesSetFailTrue = createFreestyleProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, CoverageDecreasedAction.FAIL_BUILD);
        buildWithResult(freestyleProjectIfDecreasesSetFailTrue, Result.FAILURE);
        FreeStyleProject projectIfDecreasesSetFailFalse = createFreestyleProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, CoverageDecreasedAction.DONT_FAIL_BUILD);
        buildWithResult(projectIfDecreasesSetFailFalse, Result.SUCCESS);

    }

    /**
     * Integration test for pipeline projects for checking if build failes when coverage decreases.
     */
    @Test
    public void pipelineProjectTestBuildResultDependingOnFailBuildIfCoverageDecreases() {
        WorkflowJob pipelineProjectIfDecreasesSetFailTrue = createPipelineProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, CoverageDecreasedAction.FAIL_BUILD);
        buildWithResult(pipelineProjectIfDecreasesSetFailTrue, Result.FAILURE);

        WorkflowJob pipelineProjectIfDecreasesSetFailFalse = createPipelineProjectWithDecreasedCoverage(
                COBERTURA_HIGHER_COVERAGE,
                COBERTURA_LOWER_COVERAGE, CoverageDecreasedAction.DONT_FAIL_BUILD);
        buildWithResult(pipelineProjectIfDecreasesSetFailFalse, Result.SUCCESS);
    }

    /**
     * Creates freestyle-project with second build containing decreased coverage.
     *
     * @param filename
     *         with higher coverage
     * @param filenameOfDecreasedCoverage
     *         with decreased coverage
     * @param coverageDecreased
     *         to set if build should fail when coverage decreases
     *
     * @return {@link FreeStyleProject} with decreased Coverage
     * @throws IOException
     *         if publisher list of project is missing
     */
    FreeStyleProject createFreestyleProjectWithDecreasedCoverage(final String filename,
            final String filenameOfDecreasedCoverage,
            final CoverageDecreasedAction coverageDecreased)
            throws IOException {

        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, filename);
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(filename);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        //run first build
        buildSuccessfully(project);

        //prepare second build
        copyFilesToWorkspace(project, filenameOfDecreasedCoverage);

        CoberturaReportAdapter reportAdapter2 = new CoberturaReportAdapter(
                filenameOfDecreasedCoverage);

        //set FailBuildIfCoverageDecreasedInChangeRequest on true
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(
                coverageDecreased == CoverageDecreasedAction.FAIL_BUILD);

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
     * @param coverageDecreased
     *         to set if build should fail when coverage decreases
     *
     * @return {@link WorkflowJob} with decreased coverage
     */
    WorkflowJob createPipelineProjectWithDecreasedCoverage(final String filename,
            final String filenameOfDecreasedCoverage,
            final CoverageDecreasedAction coverageDecreased) {

        WorkflowJob job = createPipelineWithWorkspaceFiles(filename, filenameOfDecreasedCoverage);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + filename + "')]"
                + "}", true));
        buildSuccessfully(job);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + filenameOfDecreasedCoverage + "')],"
                + "   failBuildIfCoverageDecreasedInChangeRequest: " + (coverageDecreased
                == CoverageDecreasedAction.FAIL_BUILD)
                + "}", true));

        return job;
    }

}


