package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;

import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class ReportAggregationITest  extends IntegrationTestWithJenkinsPerSuite {
    //TODO

    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "coverage-with-lots-of-data.xml";


    @Test
    public void checkAggregationReports() {
        CoverageBuildAction containsAggregatedReport = getCoverageResultFromPipelineJobWithCoberturaFile(COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);
        assertThat(containsAggregatedReport.getLineCoverage()).isEqualTo(new Coverage(604, 960 - 604));
        assertThat(containsAggregatedReport.getBranchCoverage()).isEqualTo(new Coverage(285, 628 - 285));
    }


    CoverageBuildAction getCoverageResultFromFreestyleJobWithCoberturaFile(final String... filenames){
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, filenames);
        List<CoverageAdapter> coverageAdapters = new ArrayList<>();
        for (String filename : filenames) {
            CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(filename);
            coverageAdapters.add(coberturaReportAdapter);
        }

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(coverageAdapters);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);
        return build.getAction(CoverageBuildAction.class);
    }


    CoverageBuildAction getCoverageResultFromPipelineJobWithCoberturaFile(final String... filenames){

        WorkflowJob job = createPipelineWithWorkspaceFiles(filenames);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCobertura('**/cobertura-higher-coverage.xml, **/coverage-with-lots-of-data.xml')]"
                + "}", true));



        Run<?, ?> build = buildSuccessfully(job);
        return build.getAction(CoverageBuildAction.class);
    }


}
