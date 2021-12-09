package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Integration test for delta computation of reference builds.
 */
public class DeltaComputationVsReferenceBuildITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_FILE = "jacoco-codingstyle.xml";

    /**
     * Checks if delta can be computed for reference build.
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    @Ignore
    @Test
    public void freestyleProjectTryCreatingReferenceBuildWithDeltaComputation()
            throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);

        CoveragePublisher coveragePublisherFirstBuild = new CoveragePublisher();
        coveragePublisherFirstBuild.setAdapters(
                Collections.singletonList(new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE)));

        project.getPublishersList().add(coveragePublisherFirstBuild);

        //run first build
        Run<?, ?> firstBuild = buildSuccessfully(project);
        ReferenceBuild referenceBuild = new ReferenceBuild(firstBuild, Collections.emptyList());

        //verify reference build owner
        assertThat(referenceBuild.getOwner()).isEqualTo(firstBuild);

        //prepare second build
        copyFilesToWorkspace(project, JACOCO_CODINGSTYLE_FILE);

        CoveragePublisher coveragePublisherSecondBuild = new CoveragePublisher();

        coveragePublisherSecondBuild.setAdapters(Collections.singletonList(new JacocoReportAdapter(
                JACOCO_CODINGSTYLE_FILE)));
        project.getPublishersList().add(coveragePublisherSecondBuild);

        //run second build
        Run<?, ?> secondBuild = buildSuccessfully(project);
        verifyDeltaComputation(firstBuild, secondBuild);
    }

    /**
     * Verifies delta of first and second build of job.
     *
     * @param firstBuild
     *         of project
     * @param secondBuild
     *         of project with reference
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    private void verifyDeltaComputation(final Run<?, ?> firstBuild, final Run<?, ?> secondBuild)
            throws IOException, ClassNotFoundException {
        assertThat(secondBuild.getAction(CoverageBuildAction.class)).isNotNull();
        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild().get()).isEqualTo(firstBuild);

        CoverageResult resultFirstBuild = CoverageProcessor.recoverCoverageResult(firstBuild);
        CoverageResult resultSecondBuild = CoverageProcessor.recoverCoverageResult(secondBuild);
        assertThat(resultSecondBuild.hasDelta(CoverageElement.CONDITIONAL)).isTrue();
        assertThat(resultFirstBuild.hasDelta(CoverageElement.CONDITIONAL)).isFalse();

        //Coverage result is different depending on using pipeline or freestyle project
        assertThat(resultSecondBuild.getDeltaResults().get(CoverageElement.CONDITIONAL)).isEqualTo(5.6865005f);
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
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_ANALYSIS_MODEL_FILE
                + "')]"
                + "}", true));

        Run<?, ?> firstBuild = buildSuccessfully(job);
        copyFilesToWorkspace(job, JACOCO_CODINGSTYLE_FILE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('" + JACOCO_CODINGSTYLE_FILE + "')]\n"
                + "discoverReferenceBuild(referenceJob: '" + job.getName() + "')"
                + "}", true));

        Run<?, ?> secondBuild = buildSuccessfully(job);

        verifyDeltaComputation(firstBuild, secondBuild);
    }

}
