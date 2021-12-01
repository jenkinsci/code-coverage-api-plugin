package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerTest;

public class FailBuildIfCoverageDecreases extends IntegrationTestWithJenkinsPerTest {
    //TODO: refactoring


    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "cobertura-lower-coverage.xml";
    @Test
    public void shouldFailDueToDecreasingCoverageIsSetTrue() throws IOException {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        project.renameTo("Adrian");
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        //set FailBuildIfCoverageDecreasedInChangeRequest on true
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        //run first build
        Run<?, ?> build = buildSuccessfully(project);

        //prepare second build
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME_2);

        CoberturaReportAdapter reportAdapter2 = new CoberturaReportAdapter(
                COBERTURA_FILE_NAME_2);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);

        coveragePublisher.setAdapters(Collections.singletonList(reportAdapter2));
        project.getPublishersList().replace(coveragePublisher);

        //run second build in same project
        Run<?, ?> currentBuild2 = buildWithResult(project, Result.FAILURE);
    }

    @Test
    public void shouldSucceedDueToDecreasingCoverageIsSetFalse() throws IOException {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        project.renameTo("Adrian");
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        //set FailBuildIfCoverageDecreasedInChangeRequest on true
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        //run first build
        Run<?, ?> build = buildSuccessfully(project);

        //prepare second build
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME_2);

        CoberturaReportAdapter reportAdapter2 = new CoberturaReportAdapter(
                COBERTURA_FILE_NAME_2);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(false);
        //adapters.add(jacocoReportAdapter2);
        coveragePublisher.setAdapters(Collections.singletonList(reportAdapter2));
        //coverageResult.getReferenceBuild();

        // coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().replace(coveragePublisher);

        //run second build in same project
        Run<?, ?> currentBuild2 = buildWithResult(project, Result.SUCCESS);

    }

}


