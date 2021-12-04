package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
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
    private static final String JACOCO_FILENAME = "jacoco-analysis-model.xml";
    private static final String REPOSITORY_URL = "https://github.com/jenkinsci/analysis-model.git";
    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";

    /**
     * Tests publishing of checks in freestyle-project when skip publishing checks is false.
     */
    @Test
    public void freeStylePublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(false);
        checkConsoleLog(buildSuccessfully(project), false);
    }

    /**
     * Tests publishing of checks in pipeline-project when skip publishing checks is false.
     */
    @Test
    public void pipelinePublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(false);
        checkConsoleLog(buildSuccessfully(job), false);
    }

    /**
     * Tests publishing of checks in freestyle-project is skipped when skip publishing checks is true.
     */
    @Test
    public void freeStyleSkipPublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(true);
        checkConsoleLog(buildSuccessfully(project), true);

    }

    /**
     * Tests publishing of checks in pipeline-project is skipped when skip publishing checks is true.
     */
    @Test
    public void pipelineSkipPublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(true);
        checkConsoleLog(buildSuccessfully(job), true);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void freeStylePublishingOfChecksWithRepo() throws IOException {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(false);
        project.setScm(new GitSCM(REPOSITORY_URL));
        checkConsoleLog(buildSuccessfully(project), false);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void pipelinePublishingOfChecksWithRepo() {
        checkConsoleLog(getPipelineProjectWithSCM(false), false);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is true.
     */
    @Test
    public void freeStyleSkipPublishingOfChecksWithRepo() throws IOException {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(true);
        project.setScm(new GitSCM(REPOSITORY_URL));
        checkConsoleLog(buildSuccessfully(project), true);

    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is true.
     */
    @Test
    public void pipelineSkipPublishingOfChecksWithRepo() {
        checkConsoleLog(getPipelineProjectWithSCM(true), true);
    }

    /**
     * Checks console log.
     *
     * @param build
     *         the successful build
     * @param skipPublishingChecks
     *         if publishing checks should be skipped
     */
    private void checkConsoleLog(final Run<?, ?> build, final boolean skipPublishingChecks) {
        String consoleLog = getConsoleLog(build);
        if (skipPublishingChecks) {
            assertThat(consoleLog).contains("Skip publishing Coverage report....");
        }
        else {
            assertThat(consoleLog).contains("Publishing Coverage report....");

        }
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
        FreeStyleProject project = createFreeStyleProject();

        copyFilesToWorkspace(project, JACOCO_FILENAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILENAME);
        coveragePublisher.setSkipPublishingChecks(skipPublishingChecks);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        assertThat(coveragePublisher.isSkipPublishingChecks()).isEqualTo(skipPublishingChecks);
        return project;
    }

    /**
     * Creates pipeline project with jaoco file and adapter.
     *
     * @param skipPublishingChecks
     *         if publishing checks should be skipped
     *
     * @return {@link FreeStyleProject} with jacoco file and adapter
     */
    private WorkflowJob getPipelineProjectWithJacoco(final boolean skipPublishingChecks) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILENAME);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILENAME + "')], "
                + "   skipPublishingChecks: " + skipPublishingChecks
                + "}", true));

        return job;
    }

    /**
     * Creates a build of a pipeline project with jaoco file and adapter.
     *
     * @return build
     */
    private Run<?, ?> getPipelineProjectWithSCM(final boolean skipPublishingChecks) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILENAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + COMMIT + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + REPOSITORY_URL + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILENAME
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD'),\n"
                + "skipPublishingChecks: " + skipPublishingChecks
                + "}", true));

        return buildSuccessfully(job);
    }

}
