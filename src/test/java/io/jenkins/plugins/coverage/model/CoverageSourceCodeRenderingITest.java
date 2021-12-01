package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Tests if source code Rendering and copying works.
 */
public class CoverageSourceCodeRenderingITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Reads source code from git and adds it to project.
     */
    @Test
    public void SourceCodeCopyingTest() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

       project.setScm(new GitSCM("https://github.com/jenkinsci/code-coverage-api-plugin.git"));

        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

    }
}
