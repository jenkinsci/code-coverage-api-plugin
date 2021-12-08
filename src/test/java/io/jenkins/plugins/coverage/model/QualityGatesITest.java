package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for QualityGates/thresholds being respected.
 */
public class QualityGatesITest extends IntegrationTestWithJenkinsPerSuite {
//TODO: fix pipeline project tests

    private static final float NOT_ACHIEVED_UNHEALTHY_THRESHOLD = 99.9f;
    private static final float NOT_ACHIEVED_UNSTABLE_THRESHOLD = 99.9f;
    private static final float ACHIEVED_UNHEALTHY_THRESHOLD = 50f;
    private static final float ACHIEVED_UNSTABLE_THRESHOLD = 80f;

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Tests if when QualityGates being fulfilled, build returns success without failure message.
     */
    @Test
    public void freeStyleShouldMeetQualityTargets() {
        FreeStyleProject project = createFreeStyleProjectWithOneLineThresholds(ACHIEVED_UNHEALTHY_THRESHOLD,
                ACHIEVED_UNSTABLE_THRESHOLD);
        verifiesBuildStatus(project);
    }

    /**
     * Tests if when QualityGates for unstable not being fulfilled, build returns unstable with failure message.
     */
    @Test
    public void freeStyleShouldNotMeetQualityTargets() {

        FreeStyleProject project = createFreeStyleProjectWithOneLineThresholds(NOT_ACHIEVED_UNHEALTHY_THRESHOLD,
                NOT_ACHIEVED_UNSTABLE_THRESHOLD);

        verifiesBuildStatusAndFailMessage(NOT_ACHIEVED_UNHEALTHY_THRESHOLD, NOT_ACHIEVED_UNSTABLE_THRESHOLD, project);
    }

    /**
     * Creates a freestyle project with one line threshold.
     *
     * @param unhealthy
     *         threshold for line coverage
     * @param unstable
     *         threshold for line coverage
     *
     * @return freestyle project with one line threshold
     */
    FreeStyleProject createFreeStyleProjectWithOneLineThresholds(final float unhealthy, final float unstable) {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setGlobalThresholds(createThresholdsContainingOneLineThreshold(unhealthy, unstable));
        project.getPublishersList().add(coveragePublisher);
        return project;
    }

    /**
     * Creates threshold containing one line threshold.
     *
     * @param unhealthy
     *         threshold for line coverage
     * @param unstable
     *         threshold for line coverage
     *
     * @return threshold containing one line threshold
     */
    List<Threshold> createThresholdsContainingOneLineThreshold(final float unhealthy, final float unstable) {
        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(unhealthy);
        lineThreshold.setUnstableThreshold(unstable);
        thresholds.add(lineThreshold);
        return thresholds;
    }

    /**
     * Tests if build succeeds, when line thresholds within range.
     */
    @Test
    public void pipelineShouldMeetQualityTargets() {
        WorkflowJob job = createPipelineWithLineThreshold(ACHIEVED_UNHEALTHY_THRESHOLD, ACHIEVED_UNSTABLE_THRESHOLD);
        verifiesBuildStatus(job);
    }

    /**
     * Tests if build is unstable, when line thresholds above coverage.
     */
    @Test
    public void pipelineShouldNotMeetQualityTargets() {

        WorkflowJob job = createPipelineWithLineThreshold(NOT_ACHIEVED_UNHEALTHY_THRESHOLD,
                NOT_ACHIEVED_UNSTABLE_THRESHOLD);
        verifiesBuildStatusAndFailMessage(NOT_ACHIEVED_UNHEALTHY_THRESHOLD, NOT_ACHIEVED_UNSTABLE_THRESHOLD, job);
    }

    /**
     * Verifies if build is successful and has no fail message.
     *
     * @param job
     *         job to fest with
     */
    private void verifiesBuildStatus(final ParameterizedJob<?, ?> job) {
        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        String message = build.getAction(CoverageBuildAction.class).getFailMessage();
        assertThat(message).isEqualTo(null);
    }

    /**
     * Verifies build status and fail message of job.
     *
     * @param unhealthyThreshold
     *         value of unhealthy threshold
     * @param unstableThreshold
     *         value of unstable threshold
     * @param job
     *         job to test with
     */
    private void verifiesBuildStatusAndFailMessage(final float unhealthyThreshold, final float unstableThreshold,
            final ParameterizedJob<?, ?> job) {
        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);

        //FIXME: bug? - test should run successfully too by using CoverageBuildAction.class
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo(
                "Build unstable because following metrics did not meet stability target: [Line {unstableThreshold="
                        + unstableThreshold
                        + ", unhealthyThreshold=" + unhealthyThreshold + "}].");
    }

    /**
     * Creates pipeline project with jacoco adapter and global threshold.
     *
     * @param unhealthyThreshold
     *         value of unhealthy threshold
     * @param unstableThreshold
     *         value of unstable threshold
     *
     * @return pipeline project
     */
    private WorkflowJob createPipelineWithLineThreshold(final float unhealthyThreshold, final float unstableThreshold) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILE_NAME + "')],"
                + "   globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: "
                + unhealthyThreshold + ", unstableThreshold: " + unstableThreshold + "]]"
                + "}", true));
        return job;
    }
}
