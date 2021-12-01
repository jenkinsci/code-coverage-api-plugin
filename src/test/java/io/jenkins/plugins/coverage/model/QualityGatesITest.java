package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for QualityGates/tresholds beeing respected.
 */
public class QualityGatesITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Tests if when QualityGates beeing fullfilled, build returns success without failure message.
     */
    @Test
    public void shouldReturnSuccess() {
        FreeStyleProject project = createFreeStyleProjectWithOneLineTresholds(50, 80);
        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo(null);
    }

    /**
     * Tests if when QualityGates for unstable not beeing fullfilled, build returns unstable with failure message.
     */
    @Test
    public void shouldReturnUnstable() {
        FreeStyleProject project = createFreeStyleProjectWithOneLineTresholds(100, 100);
        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo(
                "Build unstable because following metrics did not meet stability target: [Line {unstableThreshold=100.0, unhealthyThreshold=100.0}].");
    }

    /**
     * Creates a freestyle project with one line treshold
     * @param unhealthy treshold for line coverage
     * @param unstable treshold for line coverage
     * @return freestyle project with one line treshold
     */
    FreeStyleProject createFreeStyleProjectWithOneLineTresholds(final float unhealthy, final float unstable) {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher.setGlobalThresholds(createTresholdsContainingOneLineTreshold(unhealthy, unstable));
        project.getPublishersList().add(coveragePublisher);
        return project;
    }

    /**
     * Creates Tresholds containing one line treshold.
     * @param unhealthy treshold for line coverage
     * @param unstable treshold for line coverage
     * @return tresholds containing one line treshold
     */
    List<Threshold> createTresholdsContainingOneLineTreshold(final float unhealthy, final float unstable) {
        List<Threshold> thresholds = new ArrayList<>();
        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(unhealthy);
        lineThreshold.setUnstableThreshold(unstable);
        thresholds.add(lineThreshold);
        return thresholds;
    }
}
