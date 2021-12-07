package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Tests if 0 reports fail vs ok.
 */
public class CoveragePlugin0vsOkITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";

    /**
     * Adapter reads no file and failNoReports is set true in freestyle project.
     */
    @Test
    public void freeStyleNoFileAndFailNoReportsTrue() {
        FreeStyleProject project = createFreeStyleProjectWithFailNoReports(true, "");

        buildSuccessfully(project);
    }

    /**
     * Adapter reads no file and failNoReports is set false in freestyle project.
     */
    @Test
    public void freeStyleNoFileAndFailNoReportsFalse() {
        FreeStyleProject project = createFreeStyleProjectWithFailNoReports(false, "");

        buildSuccessfully(project);
    }

    /**
     * Adapter reads one file and failNoReports is set true in freestyle project.
     */
    @Test
    public void freeStyleWithFileAndFailNoReportsTrue() {
        FreeStyleProject project = createFreeStyleProjectWithFailNoReports(true, JACOCO_ANALYSIS_MODEL_FILE);

        buildSuccessfully(project);
    }

    /**
     * Adapter reads one file and failNoReports is set false in freestyle project.
     */
    @Test
    public void freeStyleWithFileAndFailNoReportsFalse() {
        FreeStyleProject project = createFreeStyleProjectWithFailNoReports(false, JACOCO_ANALYSIS_MODEL_FILE);

        buildSuccessfully(project);
    }

    /**
     * Adapter reads one file and failNoReports is set false in freestyle project.
     */
    @Test
    public void freeStyleWithFileWildcardAndFailNoReportsTrue() {
        FreeStyleProject project = createFreeStyleProjectWithFailNoReports(true, "**/*.xml");

        buildSuccessfully(project);
    }

    /**
     * Adapter reads one file and failNoReports is set false in freestyle project.
     */
    @Test
    public void freeStyleWithFileWildcardAndFailNoReportsFalse() {
        FreeStyleProject project = createFreeStyleProjectWithFailNoReports(false, "**/*.xml");

        buildSuccessfully(project);
    }

    /**
     * Adapter reads no file and failNoReports is set true in pipeline project.
     */
    @Test
    public void pipelineNoFileAndFailNoReportsTrue() {
        WorkflowJob job = getPipelineProjectWithJacoco(true, "");

        buildSuccessfully(job);
    }

    /**
     * Adapter reads no file and failNoReports is set false in pipeline project.
     */
    @Test
    public void pipelineNoFileAndFailNoReportsFalse() {
        WorkflowJob job = getPipelineProjectWithJacoco(false, "");

        buildSuccessfully(job);
    }

    /**
     * Adapter reads one file and failNoReports is set true in pipeline project.
     */
    @Test
    public void pipelineWithFileAndFailNoReportsTrue() {
        WorkflowJob job = getPipelineProjectWithJacoco(true, JACOCO_ANALYSIS_MODEL_FILE);

        buildSuccessfully(job);
    }

    /**
     * Adapter reads one file and failNoReports is set false in pipeline project.
     */
    @Test
    public void pipelineWithFileAndFailNoReportsFalse() {
        WorkflowJob job = getPipelineProjectWithJacoco(false, JACOCO_ANALYSIS_MODEL_FILE);

        buildSuccessfully(job);
    }

    /**
     * Adapter reads one file and failNoReports is set false in pipeline project.
     */
    @Test
    public void pipelineWithFileWildcardAndFailNoReportsTrue() {
        WorkflowJob job = getPipelineProjectWithJacoco(true, "**/*.xml");

        buildSuccessfully(job);
    }

    /**
     * Adapter reads one file and failNoReports is set false in pipeline project.
     */
    @Test
    public void pipelineWithFileWildcardAndFailNoReportsFalse() {
        WorkflowJob job = getPipelineProjectWithJacoco(false, "**/*.xml");

        buildSuccessfully(job);
    }

    /**
     * Creates a freestyle project with set fail with no reports and path to jacoco file.
     *
     * @param setFailNoReports
     *         sets FailNoReports to this value
     * @param pathToJacoco
     *         path to jacoco file
     *
     * @return {@link FreeStyleProject} with set parameters
     */
    private FreeStyleProject createFreeStyleProjectWithFailNoReports(final boolean setFailNoReports,
            final String pathToJacoco) {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(pathToJacoco);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setFailNoReports(setFailNoReports);
        project.getPublishersList().add(coveragePublisher);
        return project;
    }

    /**
     * Creates a pipeline project with set fail with no reports and path to jacoco file.
     *
     * @param setFailNoReports
     *         sets FailNoReports to this value
     * @param pathToJacoco
     *         path to jacoco file
     *
     * @return pipeline project with set parameters
     */
    private WorkflowJob getPipelineProjectWithJacoco(final boolean setFailNoReports,
            final String pathToJacoco) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + pathToJacoco + "')]\n "
                + "   failNoReports: " + setFailNoReports
                + "}", true));

        return job;
    }
}
