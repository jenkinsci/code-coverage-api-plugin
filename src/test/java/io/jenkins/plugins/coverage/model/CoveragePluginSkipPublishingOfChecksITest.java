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

import static org.assertj.core.api.Assertions.*;

/**
 * Tests if publishing of checks can be skipped.
 */
public class CoveragePluginSkipPublishingOfChecksITest extends IntegrationTestWithJenkinsPerSuite {

    /**
     * Tests publishing of checks is skipped when skip publishing checks is true.
     */
    @Test
    public void skipPublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(true);

        checkConsoleLog(project, "Skip publishing Coverage report....");
    }

    /**
     * Tests publishing of checks when skip publishing checks is false.
     */
    @Test
    public void publishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(false);

        checkConsoleLog(project, "Publishing Coverage report....");
    }

    /**
     * Checks console log.
     *
     * @param project
     *         the project with console log
     * @param expectedConsoleLog
     *         the expected console log
     */
    private void checkConsoleLog(final FreeStyleProject project, final String expectedConsoleLog) {
        Run<?, ?> build = buildSuccessfully(project);

        String consoleLog = getConsoleLog(build);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(consoleLog).contains(expectedConsoleLog);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is true.
     */
    @Test
    public void skipPublishingOfChecksWithRepo() throws IOException {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(true);
        project.setScm(new GitSCM("https://github.com/jenkinsci/analysis-model.git"));
        checkConsoleLog(project, "Skip publishing Coverage report....");

    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void publishingOfChecksWithRepo() throws IOException {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(false);
        project.setScm(new GitSCM("https://github.com/jenkinsci/analysis-model.git"));
        checkConsoleLog(project, "Publishing Coverage report....");

    }

    /**
     * Creates freestyle project with jacoco file and adapter.
     *
     * @param skipPublishingChecks
     *         if publishing checks should be skipped
     *
     * @return {@link FreeStyleProject} with jacoco file and adapter
     */
    private FreeStyleProject getFreeStyleProjectWithJacoco(final boolean skipPublishingChecks) {
        final String jacocoFileName = "jacoco-analysis-model.xml";
        FreeStyleProject project = createFreeStyleProject();

        copyFilesToWorkspace(project, jacocoFileName);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(jacocoFileName);
        coveragePublisher.setSkipPublishingChecks(skipPublishingChecks);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        assertThat(coveragePublisher.isSkipPublishingChecks()).isEqualTo(skipPublishingChecks);
        return project;
    }
}
