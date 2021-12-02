package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests if publishing of checks can be skipped.
 */
public class CoveragePluginSkipPublishingITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Tests if publishing of checks is skipped when skip publishing checks flag is set.
     */
    @Test
    public void skipPublishingOfChecks() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(coveragePublisher.isSkipPublishingChecks()).isTrue();
        assertThat(build.getNumber()).isEqualTo(1);

    }

}
