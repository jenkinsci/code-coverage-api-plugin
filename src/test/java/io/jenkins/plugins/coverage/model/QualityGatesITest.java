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
 * Integration test for QualityGates/thresholds being respected.
 */
public class QualityGatesITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Tests if when QualityGates being fulfilled, build returns success without failure message.
     */
    @Test
    public void shouldReturnSuccess() {
        FreeStyleProject project = createFreeStyleProjectWithOneLineThresholds(50, 80);
        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
        String message = build.getAction(CoverageBuildAction.class).getFailMessage();
        assertThat(message).isEqualTo(null);
    }

    /**
     * Tests if when QualityGates for unstable not being fulfilled, build returns unstable with failure message.
     */
    @Test
    public void shouldReturnUnstable() {
        FreeStyleProject project = createFreeStyleProjectWithOneLineThresholds(100, 100);
        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        //FIXME: bug? - test should run successfully too by using CoverageBuildAction.class
        String message = build.getAction(CoverageAction.class).getFailMessage();
        assertThat(message).isEqualTo(
                "Build unstable because following metrics did not meet stability target: [Line {unstableThreshold=100.0, unhealthyThreshold=100.0}].");
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
}
