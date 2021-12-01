package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test for HealthReports.
 */
public class HealthReportITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Build should succeed and HealthScore is 100%.
     */
    @Test
    public void shouldReturnSuccess() {

        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        List<Threshold> thresholds = new ArrayList<>();
        coveragePublisher.setGlobalThresholds(thresholds);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        HealthReport healthReport = build.getAction(CoverageAction.class).getHealthReport();
        assertThat(healthReport.getScore()).isEqualTo(100);
        assertThat(healthReport.getLocalizableDescription().toString()).isEqualTo("Coverage Healthy score is 100%");
        assertThat(healthReport.getIconUrl()).isEqualTo("health-80plus.png");
    }

    /**
     * Build should fail and HealthScore is 0%.
     */
    @Test
    public void shouldReturnFail() {

        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnstableThreshold(98);
        thresholds.add(lineThreshold);
        coveragePublisher.setGlobalThresholds(thresholds);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        HealthReport healthReport = build.getAction(CoverageAction.class).getHealthReport();
        assertThat(healthReport.getIconUrl()).isEqualTo("health-00to19.png");
        assertThat(healthReport.getLocalizableDescription().toString()).isEqualTo("Coverage Healthy score is 0%");
        assertThat(healthReport.getScore()).isEqualTo(0);
    }

    /**
     * Build should be unstable and HealthScore is 0%.
     */
    @Test
    public void shouldReturnUnstable() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95);
        lineThreshold.setUnstableThreshold(99);
        thresholds.add(lineThreshold);
        coveragePublisher.setGlobalThresholds(thresholds);

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        HealthReport healthReport = build.getAction(CoverageAction.class).getHealthReport();
        assertThat(healthReport.getIconUrl()).isEqualTo("health-00to19.png");
        assertThat(healthReport.getLocalizableDescription().toString()).isEqualTo("Coverage Healthy score is 0%");
        assertThat(healthReport.getScore()).isEqualTo(0);
    }

}
