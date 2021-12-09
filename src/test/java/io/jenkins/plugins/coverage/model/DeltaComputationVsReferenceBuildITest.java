package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Integration test for delta computation of reference builds.
 */
public class DeltaComputationVsReferenceBuildITest extends IntegrationTestWithJenkinsPerSuite {


    private static final String COBERTURA_LOWER_COVERAGE_XML = "cobertura-lower-coverage.xml";
    private static final String COBERTURA_HIGHER_COVERAGE_XML = "cobertura-higher-coverage.xml";

    /**
     * Checks if delta can be computed for reference build.
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    @Test
    public void freestyleProjectTryCreatingReferenceBuildWithDeltaComputation()
            throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(COBERTURA_LOWER_COVERAGE_XML);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_LOWER_COVERAGE_XML);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        //run first build
        Run<?, ?> firstBuild = buildSuccessfully(project);

        //prepare second build
        copyFilesToWorkspace(project, COBERTURA_HIGHER_COVERAGE_XML);

        CoberturaReportAdapter coberturaReportAdapter2 = new CoberturaReportAdapter(
                COBERTURA_HIGHER_COVERAGE_XML);

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter2));

        project.getPublishersList().add(coveragePublisher);

        createReferenceBuild(project, firstBuild);
    }

    /**
     * Verifies delta of first and second build of job.
     *
     * @param job
     *         with two builds to test with
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    private void verifyDeltaComputation(final Job<?, ?> job) throws IOException, ClassNotFoundException {
        CoverageResult resultFirstBuild = CoverageProcessor.recoverCoverageResult(job.getBuildByNumber(1));
        CoverageResult resultSecondBuild = CoverageProcessor.recoverCoverageResult(job.getBuildByNumber(2));
        assertThat(resultSecondBuild.hasDelta(CoverageElement.CONDITIONAL)).isTrue();
        assertThat(resultFirstBuild.hasDelta(CoverageElement.CONDITIONAL)).isFalse();
        assertThat(resultSecondBuild.getDeltaResults().get(CoverageElement.CONDITIONAL)).isEqualTo(100);
        assertThat(resultSecondBuild.getDeltaResults().get(CoverageElement.LINE)).isEqualTo(50);
        assertThat(resultSecondBuild.getDeltaResults().get(CoverageElement.FILE)).isEqualTo(0);
    }

    /**
     * Starts a new build with given job and stores first build for reference.
     *
     * @param job
     *         that creates second build
     * @param firstBuild
     *         of project for reference
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    private void createReferenceBuild(final Job<?, ?> job, final Run<?, ?> firstBuild)
            throws IOException, ClassNotFoundException {
        ReferenceBuild referenceBuild = new ReferenceBuild(firstBuild, Collections.emptyList());
        referenceBuild.onLoad(firstBuild);

        Run<?, ?> secondBuild = buildWithResult((ParameterizedJob<?, ?>) job, Result.SUCCESS);
        referenceBuild.onAttached(secondBuild);

        verifyDeltaComputation(job);
    }

    /**
     * Checks if delta can be computed with reference build in pipeline project.
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    @Test
    public void pipelineCreatingReferenceBuildWithDeltaComputation() throws IOException, ClassNotFoundException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_LOWER_COVERAGE_XML);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [istanbulCoberturaAdapter('" + COBERTURA_LOWER_COVERAGE_XML
                + "')]"
                + "}", true));

        buildSuccessfully(job);
        copyFilesToWorkspace(job, COBERTURA_HIGHER_COVERAGE_XML);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [istanbulCoberturaAdapter('" + COBERTURA_HIGHER_COVERAGE_XML + "')]\n"
                + "discoverReferenceBuild(referenceJob: '" + job.getName() + "')"
                + "}", true));

        buildSuccessfully(job);

        verifyDeltaComputation(job);
    }

}
