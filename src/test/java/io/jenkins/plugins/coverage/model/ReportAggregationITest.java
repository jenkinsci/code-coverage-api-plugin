package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for report aggregation.
 */
public class ReportAggregationITest extends IntegrationTestWithJenkinsPerSuite {
//TODO: Assertions ändern

    private static final String COBERTURA_FILE_NAME = "cobertura-lower-coverage.xml";
    private static final String COBERTURA_ANOTHER_FILE_FILE_NAME = "../coverage-with-lots-of-data.xml";

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    private static final String JACOCO_ANOTHER_FILE_FILE_NAME = "jacoco-codingstyle.xml";

    /**
     * Checks aggregated coverage result for freestyle job with two jacoco files.
     */
    @Test
    public void checkCoverageResultFromFreestyleJobWithJacocoFiles() throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME, JACOCO_ANOTHER_FILE_FILE_NAME);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        JacocoReportAdapter adapter1 = new JacocoReportAdapter(JACOCO_FILE_NAME);
        JacocoReportAdapter adapter2 = new JacocoReportAdapter(JACOCO_ANOTHER_FILE_FILE_NAME);
        adapter1.setMergeToOneReport(true);
        adapter2.setMergeToOneReport(true);
        coverageAdapters.add(adapter1);
        coverageAdapters.add(adapter2);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(coverageAdapters);

        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);
        verifyJacocoReportAggregation(build);
    }

    @Test
    public void checkCoverageResultFromPipelineJobWithJacocoFiles() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME, JACOCO_ANOTHER_FILE_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('mergeToOneReport: true," + JACOCO_FILE_NAME + "'),"
                + "jacocoAdapter('mergeToOneReport: true," + JACOCO_ANOTHER_FILE_FILE_NAME + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        verifyJacocoReportAggregation(build);
    }

    @Test
    public void checkCoverageResultFromPipelineJobWithCoberturaFiles() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_NAME, COBERTURA_ANOTHER_FILE_FILE_NAME);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('mergeToOneReport: true,"
                + COBERTURA_FILE_NAME + "'),"
                + "istanbulCoberturaAdapter('mergeToOneReport: true," + COBERTURA_ANOTHER_FILE_FILE_NAME + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        verifyCoberturaReportAggregation(build);
    }

    /**
     * Checks aggregated coverage result for freestyle job with two jacoco files.
     */
    @Test
    public void checkCoverageResultFromFreestyleJobWithCoberturaFiles() throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME, COBERTURA_ANOTHER_FILE_FILE_NAME);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        CoberturaReportAdapter adapter1 = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        CoberturaReportAdapter adapter2 = new CoberturaReportAdapter(COBERTURA_ANOTHER_FILE_FILE_NAME);

        adapter1.setMergeToOneReport(true);
        adapter2.setMergeToOneReport(true);
        coverageAdapters.add(adapter1);
        coverageAdapters.add(adapter2);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(coverageAdapters);

        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);
        verifyCoberturaReportAggregation(build);
    }

    private void verifyCoberturaReportAggregation(final Run<?, ?> build) throws IOException, ClassNotFoundException {
        CoverageResult aggregatedResult = CoverageProcessor.recoverCoverageResult(build);
        aggregatedResult.isAggregatedLevel();
        assertThat(aggregatedResult.getCoverage(CoverageElement.LINE).toString()).isEqualTo("050.00 (2/4)");
        assertThat(aggregatedResult.getCoverage(CoverageElement.CONDITIONAL).toString()).isEqualTo("000.00 (0/4)");

    }

    private void verifyJacocoReportAggregation(final Run<?, ?> build) throws IOException, ClassNotFoundException {
        CoverageResult aggregatedResult = CoverageProcessor.recoverCoverageResult(build);
        aggregatedResult.isAggregatedLevel();
        assertThat(aggregatedResult.getCoverage(CoverageElement.LINE).toString()).isEqualTo("095.16 (5825/6121)");
        assertThat(aggregatedResult.getCoverage(CoverageElement.CONDITIONAL).toString()).isEqualTo(
                "088.63 (1653/1865)");
    }

}
