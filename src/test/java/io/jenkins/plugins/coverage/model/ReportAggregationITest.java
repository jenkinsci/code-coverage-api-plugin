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

    private static final String COBERTURA_LOWER_COVERAGE_XML = "cobertura-lower-coverage.xml";
    private static final String COVERAGE_WITH_LOTS_OF_DATA_XML = "../coverage-with-lots-of-data.xml";

    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";

    /**
     * Checks aggregated coverage result for freestyle job with two jacoco files.
     *
     * @throws IOException
     *         due to verifyJacocoReportAggregation()
     * @throws ClassNotFoundException
     *         due to verifyJacocoReportAggregation()
     */
    @Test
    public void checkCoverageResultFromFreestyleJobWithJacocoFiles() throws ClassNotFoundException, IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_XML, JACOCO_CODINGSTYLE_XML);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        JacocoReportAdapter adapter1 = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_XML);
        JacocoReportAdapter adapter2 = new JacocoReportAdapter(JACOCO_CODINGSTYLE_XML);
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

    /**
     * Checks aggregated coverage result for pipeline job with two jacoco files.
     *
     * @throws IOException
     *         due to verifyJacocoReportAggregation()
     * @throws ClassNotFoundException
     *         due to verifyJacocoReportAggregation()
     */
    @Test
    public void checkCoverageResultFromPipelineJobWithJacocoFiles() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_XML, JACOCO_CODINGSTYLE_XML);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('mergeToOneReport: true," + JACOCO_ANALYSIS_MODEL_XML
                + "'),"
                + "jacocoAdapter('mergeToOneReport: true," + JACOCO_CODINGSTYLE_XML + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        verifyJacocoReportAggregation(build);
    }

    /**
     * Checks aggregated coverage result for freestyle job with two jacoco files.
     *
     * @throws IOException
     *         due to verifyJacocoReportAggregation()
     * @throws ClassNotFoundException
     *         due to verifyJacocoReportAggregation()
     */
    @Test
    public void checkCoverageResultFromFreestyleJobWithCoberturaFiles() throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_LOWER_COVERAGE_XML, COVERAGE_WITH_LOTS_OF_DATA_XML);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        CoberturaReportAdapter adapter1 = new CoberturaReportAdapter(COBERTURA_LOWER_COVERAGE_XML);
        CoberturaReportAdapter adapter2 = new CoberturaReportAdapter(COVERAGE_WITH_LOTS_OF_DATA_XML);

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

    /**
     * Checks aggregated coverage result for pipeline job with two cobertura files.
     *
     * @throws IOException
     *         due to verifyCoberturaReportAggregation()
     * @throws ClassNotFoundException
     *         due to verifyCoberturaReportAggregation()
     */
    @Test
    public void checkCoverageResultFromPipelineJobWithCoberturaFiles() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_LOWER_COVERAGE_XML,
                COVERAGE_WITH_LOTS_OF_DATA_XML);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('mergeToOneReport: true,"
                + COBERTURA_LOWER_COVERAGE_XML + "'),"
                + "istanbulCoberturaAdapter('mergeToOneReport: true," + COVERAGE_WITH_LOTS_OF_DATA_XML + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        verifyCoberturaReportAggregation(build);
    }

    /**
     * Verifies result of aggregated report of two jacoco files.
     *
     * @param build
     *         which contains aggregated report
     *
     * @throws IOException
     *         due to recoverCoverageResult()
     * @throws ClassNotFoundException
     *         due to recoverCoverageResult()
     */
    private void verifyJacocoReportAggregation(final Run<?, ?> build) throws IOException, ClassNotFoundException {
        CoverageResult aggregatedResult = CoverageProcessor.recoverCoverageResult(build);
        aggregatedResult.isAggregatedLevel();
        assertThat(aggregatedResult.getCoverage(CoverageElement.LINE).toString()).isEqualTo("095.16 (5825/6121)");
        assertThat(aggregatedResult.getCoverage(CoverageElement.CONDITIONAL).toString()).isEqualTo(
                "088.63 (1653/1865)");
    }

    /**
     * Verifies result of aggregated report of two cobertura files.
     *
     * @param build
     *         which contains aggregated report
     *
     * @throws IOException
     *         due to recoverCoverageResult()
     * @throws ClassNotFoundException
     *         due to recoverCoverageResult()
     */
    private void verifyCoberturaReportAggregation(final Run<?, ?> build) throws IOException, ClassNotFoundException {
        CoverageResult aggregatedResult = CoverageProcessor.recoverCoverageResult(build);
        aggregatedResult.isAggregatedLevel();
        assertThat(aggregatedResult.getCoverage(CoverageElement.LINE).toString()).isEqualTo("050.00 (2/4)");
        assertThat(aggregatedResult.getCoverage(CoverageElement.CONDITIONAL).toString()).isEqualTo("000.00 (0/4)");

    }

}
