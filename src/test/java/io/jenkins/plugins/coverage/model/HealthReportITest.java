package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test for HealthReports.
 */

enum Thresholds {SET_THRESHOLDS_TO_RETURN_UNSTABLE_BUILD, DONT_SET_ANY_THRESHOLDS}

public class HealthReportITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    private static final int UNSTABLE_LINE_THRESHOLD = 100;

    /**
     * No Build should succeed, no thresholds set, HealthScore should be 100%.
     */
    @Test
    public void freestyleProjectShouldReturnSuccess() {
        FreeStyleProject project = createFreeStyleProject(Thresholds.DONT_SET_ANY_THRESHOLDS);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        verifyHealthReportSuccess(build);
    }

    /**
     * Build should be unstable, thresholds set, HealthScore is 0%.
     */
    @Test
    public void freestyleProjectShouldReturnUnstable() {
        FreeStyleProject project = createFreeStyleProject(Thresholds.SET_THRESHOLDS_TO_RETURN_UNSTABLE_BUILD);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
        verifyHealthReportUnstable(build);

    }

    /**
     * Used to create a freestyle project and apply/don't apply thresholds.
     *
     * @param thresholdsApplied
     *         to apply/don't apply thresholds
     *
     * @return project
     */
    private FreeStyleProject createFreeStyleProject(final Thresholds thresholdsApplied) {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        if (thresholdsApplied == Thresholds.SET_THRESHOLDS_TO_RETURN_UNSTABLE_BUILD) {
            List<Threshold> thresholds = new ArrayList<>();
            Threshold lineThreshold = new Threshold("Line");
            lineThreshold.setUnstableThreshold(UNSTABLE_LINE_THRESHOLD);
            thresholds.add(lineThreshold);
            coveragePublisher.setGlobalThresholds(thresholds);
        }
        project.getPublishersList().add(coveragePublisher);
        return project;
    }

    /**
     * Build of pipeline project should succeed and HealthScore is 100%.
     */
    @Test
    public void pipelineShouldReturnSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILE_NAME + "')],"
                + "   globalThresholds: [[failUnhealthy: false, thresholdTarget: 'Line']]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);

        verifyHealthReportSuccess(build);
    }

    /**
     * Build of pipeline project should be unstable and HealthScore is 0%.
     */
    @Test
    public void pipelineShouldReturnUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILE_NAME + "')],"
                + "   globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', " + ", unstableThreshold: "
                + UNSTABLE_LINE_THRESHOLD + "]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        verifyHealthReportUnstable(build);
    }

    /**
     * Verifies details of health report of successful build.
     *
     * @param build
     *         which is successful
     */
    private void verifyHealthReportSuccess(final Run<?, ?> build) {
        HealthReport healthReport = build.getAction(CoverageBuildAction.class).getHealthReport();
        assertThat(healthReport.getScore()).isEqualTo(100);
        assertThat(healthReport.getLocalizableDescription().toString()).isEqualTo("Coverage Healthy score is 100%");
        assertThat(healthReport.getIconUrl()).isEqualTo("health-80plus.png");
    }

    /**
     * Verifies details of health report of unstable build.
     *
     * @param build
     *         which is unstable
     */
    private void verifyHealthReportUnstable(final Run<?, ?> build) {
        HealthReport healthReport = build.getAction(CoverageBuildAction.class).getHealthReport();
        assertThat(healthReport.getIconUrl()).isEqualTo("health-00to19.png");
        assertThat(healthReport.getLocalizableDescription().toString()).isEqualTo("Coverage Healthy score is 0%");
        assertThat(healthReport.getScore()).isEqualTo(0);
    }

}
