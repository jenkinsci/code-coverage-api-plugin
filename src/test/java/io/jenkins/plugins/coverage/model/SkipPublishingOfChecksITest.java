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
 * Enum to set skipping of publishing of checks.
 */
enum Checks { PUBLISH_CHECKS, SKIP_CHECKS}

/**
 * Enum to set if SCM is used or not.
 */
enum Sourcecode { ADD_SOURCECODE, NO_SOURCECODE}

/**
 * Tests if publishing of checks can be skipped.
 */
public class SkipPublishingOfChecksITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILENAME = "jacoco-analysis-model.xml";
    private static final String REPOSITORY_URL = "https://github.com/jenkinsci/analysis-model.git";
    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";

    /**
     * Tests publishing of checks in freestyle-project when skip publishing checks is false.
     */
    @Test
    public void freeStylePublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(Checks.PUBLISH_CHECKS);
        checkConsoleLog(buildSuccessfully(project), Checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks in pipeline-project when skip publishing checks is false.
     */
    @Test
    public void pipelinePublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(Checks.PUBLISH_CHECKS, Sourcecode.NO_SOURCECODE);
        checkConsoleLog(buildSuccessfully(job), Checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks in freestyle-project is skipped when skip publishing checks is true.
     */
    @Test
    public void freeStyleSkipPublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(Checks.SKIP_CHECKS);
        checkConsoleLog(buildSuccessfully(project), Checks.SKIP_CHECKS);
    }

    /**
     * Tests publishing of checks in pipeline-project is skipped when skip publishing checks is true.
     */
    @Test
    public void pipelineSkipPublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(Checks.SKIP_CHECKS, Sourcecode.NO_SOURCECODE);
        checkConsoleLog(buildSuccessfully(job), Checks.SKIP_CHECKS);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void freeStylePublishingOfChecksWithRepo() throws IOException {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(Checks.PUBLISH_CHECKS);
        project.setScm(new GitSCM(REPOSITORY_URL));
        checkConsoleLog(buildSuccessfully(project), Checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks with source code when skip publishing checks is false.
     */
    @Test
    public void pipelinePublishingOfChecksWithRepo() {
        WorkflowJob job = getPipelineProjectWithJacoco(Checks.PUBLISH_CHECKS, Sourcecode.ADD_SOURCECODE);
        checkConsoleLog(buildSuccessfully(job), Checks.PUBLISH_CHECKS);
    }

    /**
     * Checks console log.
     *
     * @param build
     *         the successful build
     * @param skipPublishingChecks
     *         if publishing checks should be skipped
     */
    private void checkConsoleLog(final Run<?, ?> build, final Checks skipPublishingChecks) {
        String consoleLog = getConsoleLog(build);
        if (skipPublishingChecks == Checks.PUBLISH_CHECKS) {
            assertThat(consoleLog).contains("[Checks API] No suitable checks publisher found.");
        }
        else if (skipPublishingChecks == Checks.SKIP_CHECKS) {
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
    private FreeStyleProject getFreeStyleProjectWithJacoco(final Checks skipPublishingChecks) {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_FILENAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILENAME);
        if (skipPublishingChecks == Checks.SKIP_CHECKS) {
            coveragePublisher.setSkipPublishingChecks(true);
            assertThat(coveragePublisher.isSkipPublishingChecks()).isEqualTo(true);
        }
        else if (skipPublishingChecks == Checks.PUBLISH_CHECKS) {
            coveragePublisher.setSkipPublishingChecks(false);
            assertThat(coveragePublisher.isSkipPublishingChecks()).isEqualTo(false);
        }
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        return project;
    }

    /**
     * Creates a build of a pipeline project with jacoco file and adapter.
     *
     * @param skipPublishingChecks
     *         to set if publishing checks should be skipped
     * @param shouldAddSCM
     *         to set if SCM should be added
     *
     * @return build of project with scm
     */
    private WorkflowJob getPipelineProjectWithJacoco(final Checks skipPublishingChecks, final Sourcecode shouldAddSCM) {
        String pipelineSCMCommand = "";
        String skipPublishingChecksValue = "";

        if (skipPublishingChecks == Checks.SKIP_CHECKS) {
            skipPublishingChecksValue = "true";
        }
        else if (skipPublishingChecks == Checks.PUBLISH_CHECKS) {
            skipPublishingChecksValue = "false";

        }
        if (shouldAddSCM == Sourcecode.ADD_SOURCECODE) {
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
                + "    skipPublishingChecks: " + skipPublishingChecksValue
                + "}", true));

        return job;
    }

}
