package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Tests if 0 reports fail vs ok.
 */
public class CoveragePlugin0vsOkITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Adapter reads no file and failNoReports is set true.
     */
    @Test
    public void noFileShouldFail() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Adapter reads no file and failNoReports is set false.
     */
    @Test
    public void noFileShouldSucceed() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setFailNoReports(false);
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.SUCCESS);
    }

    /**
     * Adapter reads one file and failNoReports is set true.
     */
    @Test
    public void WithFileShouldFail() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);
        // Sollte der Build wirklich failen?
        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Adapter reads one file and failNoReports is set false.
     */
    @Test
    public void WithFileShouldSucceed() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setFailNoReports(false);
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.SUCCESS);
    }
}
