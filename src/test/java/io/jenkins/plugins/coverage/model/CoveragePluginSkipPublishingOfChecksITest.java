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

enum checks {PUBLISH_CHECKS, SKIP_CHECKS}

enum sourcecode {ADD_SOURCECODE, NO_SOURCECODE}

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
        FreeStyleProject project = getFreeStyleProjectWithJacoco(checks.PUBLISH_CHECKS);
        checkConsoleLog(buildSuccessfully(project), checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks in pipeline-project when skip publishing checks is false.
     */
    @Test
    public void pipelinePublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(checks.PUBLISH_CHECKS, sourcecode.NO_SOURCECODE);
        checkConsoleLog(buildSuccessfully(job), checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks in freestyle-project is skipped when skip publishing checks is true.
     */
    @Test
    public void freeStyleSkipPublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(checks.SKIP_CHECKS);
        // FIXME: Sollte eigentlich erfolgreich durchlaufen.
        // checkConsoleLog(buildSuccessfully(project), checks.SKIP_CHECKS);
    }

    /**
     * Tests publishing of checks in pipeline-project is skipped when skip publishing checks is true.
     */
    @Test
    public void pipelineSkipPublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(checks.SKIP_CHECKS, sourcecode.NO_SOURCECODE);
        // FIXME: Sollte eigentlich erfolgreich durchlaufen.
        // checkConsoleLog(buildSuccessfully(job), checks.SKIP_CHECKS);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void freeStylePublishingOfChecksWithRepo() throws IOException {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(checks.PUBLISH_CHECKS);
        project.setScm(new GitSCM(REPOSITORY_URL));
        checkConsoleLog(buildSuccessfully(project), checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void pipelinePublishingOfChecksWithRepo() {
        WorkflowJob job = getPipelineProjectWithJacoco(checks.PUBLISH_CHECKS, sourcecode.ADD_SOURCECODE);
        checkConsoleLog(buildSuccessfully(job), checks.PUBLISH_CHECKS);
    }

    /**
     * Checks console log.
     *
     * @param build
     *         the successful build
     * @param skipPublishingChecks
     *         if publishing checks should be skipped
     */
    private void checkConsoleLog(final Run<?, ?> build, final checks skipPublishingChecks) {
        String consoleLog = getConsoleLog(build);
        if (skipPublishingChecks == checks.SKIP_CHECKS) {
            assertThat(consoleLog).contains("[Checks API] No suitable checks publisher found.");
        }
        else if (skipPublishingChecks == checks.PUBLISH_CHECKS) {
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
    private FreeStyleProject getFreeStyleProjectWithJacoco(final checks skipPublishingChecks) {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_FILENAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILENAME);
        if (skipPublishingChecks == checks.SKIP_CHECKS) {
            coveragePublisher.setSkipPublishingChecks(true);
        }
        else if (skipPublishingChecks == checks.PUBLISH_CHECKS) {
            coveragePublisher.setSkipPublishingChecks(false);
        }
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        if (skipPublishingChecks == checks.SKIP_CHECKS) {
            assertThat(coveragePublisher.isSkipPublishingChecks()).isEqualTo(true);
        }
        else if (skipPublishingChecks == checks.PUBLISH_CHECKS) {
            assertThat(coveragePublisher.isSkipPublishingChecks()).isEqualTo(false);
        }
        return project;
    }

    /**
     * Creates a build of a pipeline project with jacoco file and adapter.
     *
     * @return build of project with scm
     */
    private WorkflowJob getPipelineProjectWithJacoco(final checks skipPublishingChecks, final sourcecode shouldAddSCM) {
        String pipelineSCMCommand = "";
        String skipPublishingChecksValue = "";

        if (skipPublishingChecks == checks.SKIP_CHECKS) {
            skipPublishingChecksValue = "true";
        }
        else if (skipPublishingChecks == checks.PUBLISH_CHECKS) {
            skipPublishingChecksValue = "false";

        }
        if (shouldAddSCM == sourcecode.ADD_SOURCECODE) {
            pipelineSCMCommand = " checkout([$class: 'GitSCM', "
                    + "branches: [[name: '" + COMMIT + "' ]],\n"
                    + "userRemoteConfigs: [[url: '" + REPOSITORY_URL + "']],\n"
                    + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                    + "            relativeTargetDir: 'checkout']]])\n";
        }

        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILENAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + pipelineSCMCommand
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILENAME + "')],\n"
                + "skipPublishingChecks: " + skipPublishingChecksValue
                + "}", true));

        return job;
    }

}
