package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Integration test for report aggregation.
 */
public class ReportAggregationITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "coverage-with-lots-of-data.xml";

    /**
     * Tests aggregation of reports.
     */
    @Test
    public void checkAggregationReports() {
        CoverageBuildAction containsAggregatedReport = getCoverageResultFromFreestyleJobWithCoberturaFile();
        //TODO: add and check assertions
        //assertThat(containsAggregatedReport.getLineCoverage()).isEqualTo(new Coverage(604, 960 - 604));
        //assertThat(containsAggregatedReport.getBranchCoverage()).isEqualTo(new Coverage(285, 628 - 285));
    }

    /**
     * Adds two Cobertura files to freestyle job.
     *
     * @return {@link CoverageBuildAction} with results of two cobertura files
     */
    CoverageBuildAction getCoverageResultFromFreestyleJobWithCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        coverageAdapters.add(coberturaReportAdapter);
        CoberturaReportAdapter coberturaReportAdapter2 = new CoberturaReportAdapter(COBERTURA_FILE_NAME_2);
        coverageAdapters.add(coberturaReportAdapter2);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(coverageAdapters);

        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);
        return build.getAction(CoverageBuildAction.class);
    }

    //CoverageBuildAction getCoverageResultFromPipelineJobWithCoberturaFile() {

    //    WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);
    //    job.setDefinition(new CpsFlowDefinition("node {"
    //            + "   publishCoverage adapters: [istanbulCobertura('**/cobertura-higher-coverage.xml, **/coverage-with-lots-of-data.xml')]"
    //            + "}", true));

    //    Run<?, ?> build = buildSuccessfully(job);
    //    return build.getAction(CoverageBuildAction.class);
    //}

}
