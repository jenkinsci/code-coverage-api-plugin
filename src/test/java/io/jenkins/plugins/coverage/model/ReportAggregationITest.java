package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
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
import io.jenkins.plugins.coverage.adapter.JavaXMLCoverageReportAdapter;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for report aggregation.
 */

enum UsedAdapter { COBERTURA, JACOCO}

public class ReportAggregationITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String COBERTURA_LOWER_COVERAGE_XML = "cobertura-lower-coverage.xml";
    private static final String COBERTURA_LOTS_OF_DATA_XML = "cobertura-lots-of-data.xml";

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
    public void freeStyleProjectCheckReportAggregationWithJacocoFiles() throws ClassNotFoundException, IOException {
        FreeStyleProject project = createFreeStyleProjectWithSpecifiedAdapterAndFiles(UsedAdapter.JACOCO,
                JACOCO_CODINGSTYLE_XML, JACOCO_ANALYSIS_MODEL_XML);

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
    public void pipelineProjectCheckReportAggregationWithJacocoFiles() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineProjectWithSpecifiedAdapterAndFiles(UsedAdapter.JACOCO, JACOCO_CODINGSTYLE_XML,
                JACOCO_ANALYSIS_MODEL_XML);

        Run<?, ?> build = buildSuccessfully(job);
        verifyJacocoReportAggregation(build);
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
     * Checks aggregated coverage result for freestyle job with two jacoco files.
     *
     * @throws IOException
     *         due to verifyJacocoReportAggregation()
     * @throws ClassNotFoundException
     *         due to verifyJacocoReportAggregation()
     */
    @Test
    @Ignore
    public void freeStyleProjectCheckReportAggregationWithCoberturaFiles() throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProjectWithSpecifiedAdapterAndFiles(UsedAdapter.COBERTURA,
                COBERTURA_LOWER_COVERAGE_XML, COBERTURA_LOTS_OF_DATA_XML);
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
    public void pipelineProjectCheckReportAggregationWithCoberturaFiles() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineProjectWithSpecifiedAdapterAndFiles(UsedAdapter.COBERTURA,
                COBERTURA_LOWER_COVERAGE_XML, COBERTURA_LOTS_OF_DATA_XML);

        Run<?, ?> build = buildSuccessfully(job);
        verifyCoberturaReportAggregation(build);
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
        assertThat(aggregatedResult.getCoverage(CoverageElement.LINE).toString()).isEqualTo("065.18 (526/807)");
        assertThat(aggregatedResult.getCoverage(CoverageElement.CONDITIONAL).toString()).isEqualTo("048.50 (259/534)");

    }

    /**
     * Used to create a freestyle project which creates a build with an aggregated report.
     *
     * @param usedAdapter
     *         which is used fo
     * @param firstFile
     *         for build
     * @param secondFile
     *         for build
     *
     * @return project with merged reports
     */
    private FreeStyleProject createFreeStyleProjectWithSpecifiedAdapterAndFiles(final UsedAdapter usedAdapter,
            final String firstFile, final String secondFile) {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, firstFile, secondFile);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();
        JavaXMLCoverageReportAdapter adapter1;
        JavaXMLCoverageReportAdapter adapter2;
        if (usedAdapter == UsedAdapter.COBERTURA) {
            adapter1 = new CoberturaReportAdapter(firstFile);
            adapter2 = new CoberturaReportAdapter(secondFile);
        }
        else {
            adapter1 = new JacocoReportAdapter(firstFile);
            adapter2 = new JacocoReportAdapter(secondFile);
        }

        adapter1.setMergeToOneReport(true);
        adapter2.setMergeToOneReport(true);

        coverageAdapters.add(adapter1);
        coverageAdapters.add(adapter2);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(coverageAdapters);

        project.getPublishersList().add(coveragePublisher);
        return project;
    }

    /**
     * Used to create a pipeline project which creates a build with an aggregated reports.
     *
     * @param usedAdapter
     *         which is used fo
     * @param firstFile
     *         for build
     * @param secondFile
     *         for build
     *
     * @return pipeline project with merged reports
     */
    private WorkflowJob createPipelineProjectWithSpecifiedAdapterAndFiles(final UsedAdapter usedAdapter,
            final String firstFile, final String secondFile) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(firstFile, secondFile);
        String adapterValue = (usedAdapter == UsedAdapter.JACOCO) ? "jacocoAdapter" : "istanbulCoberturaAdapter";
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [" + adapterValue + "('mergeToOneReport: true," + firstFile
                + "'),"
                + adapterValue + "('mergeToOneReport: true," + secondFile + "')]"
                + "}", true));
        return job;
    }
}
