package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Integration test for checking if build failes when coverage decreases.
 */
public class FailBuildIfCoverageDecreases extends IntegrationTestWithJenkinsPerSuite {

    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "cobertura-lower-coverage.xml";

    /**
     * Integration test for checking if build failes when coverage decreases.
     *
     * @throws IOException
     *         if creating Project throws Exception
     */
    @Test
    public void shouldReturnSuccessAndFailureDependingOnFailBuildIfCoverageDecreases() throws IOException {
        FreeStyleProject projectIfDecreasesSetFailTrue = createProjectWithDecreasedCoverage(COBERTURA_FILE_NAME,
                COBERTURA_FILE_NAME_2, true);
        buildWithResult(projectIfDecreasesSetFailTrue, Result.FAILURE);
        FreeStyleProject projectIfDecreasesSetFailFalse = createProjectWithDecreasedCoverage(COBERTURA_FILE_NAME,
                COBERTURA_FILE_NAME_2, false);
        buildWithResult(projectIfDecreasesSetFailFalse, Result.SUCCESS);
    }

    /**
     * Creates Project with second build containing decreased coverage.
     *
     * @param filename
     *         with higher coverage
     * @param filenameOfDecreasedCoverage
     *         with decreased coverage
     * @param setFailIfCoverageDecreased
     *         to set if build should fail when coverage decreases
     */
    FreeStyleProject createProjectWithDecreasedCoverage(final String filename, final String filenameOfDecreasedCoverage,
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

}


