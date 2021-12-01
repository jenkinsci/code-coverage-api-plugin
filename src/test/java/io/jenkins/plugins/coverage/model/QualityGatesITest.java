package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.analysis.core.model.ResultAction;
import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class QualityGatesITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    //TODO: refactoring

    @Test
    public void shouldReturnSuccess() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(50);
        lineThreshold.setUnstableThreshold(80);
        thresholds.add(lineThreshold);
        coveragePublisher.setGlobalThresholds(thresholds);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildWithResult(project, Result.SUCCESS); //Unhealthy 90, unstable 95
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo(null);
       // assertThat(healthReport.getIconUrl()).isEqualTo("health-80plus.png");
    }

    @Test
    public void shouldReturnFail() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95);
        lineThreshold.setUnstableThreshold(98);
        thresholds.add(lineThreshold);
        coveragePublisher.setGlobalThresholds(thresholds);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE); //Unhealthy 90, unstable 95
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo("Build unstable because following metrics did not meet stability target: [Line {unstableThreshold=98.0, unhealthyThreshold=95.0}].");
    }

    @Test
    public void shouldReturnFailDueToFailOnUnhealthy() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95);
        lineThreshold.setUnstableThreshold(98);
        lineThreshold.setFailUnhealthy(true);
        thresholds.add(lineThreshold);
        coveragePublisher.setGlobalThresholds(thresholds);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE); //Unhealthy 90, unstable 95
        HealthReport healthReport = build.getAction(CoverageAction.class).getHealthReport();

        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo("Build unstable because following metrics did not meet stability target: [Line {unstableThreshold=98.0, unhealthyThreshold=95.0}].");

    }

    @Test
    public void shouldReturnUnstable() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
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
        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE); //Unhealthy 90, unstable 95
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo("Build unstable because following metrics did not meet stability target: [Line {unstableThreshold=99.0, unhealthyThreshold=95.0}].");

    }

}
