package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests if publishing of checks can be skipped.
 */
class SkipPublishingOfChecksITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILENAME = "jacoco-analysis-model.xml";

    /**
     * Tests publishing of checks in freestyle-project when skip publishing checks is false.
     */
    @Test
    void freeStylePublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(Checks.PUBLISH_CHECKS);
        checkConsoleLog(buildSuccessfully(project), Checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks in pipeline-project when skip publishing checks is false.
     */
    @Test
    void pipelinePublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(Checks.PUBLISH_CHECKS);
        checkConsoleLog(buildSuccessfully(job), Checks.PUBLISH_CHECKS);
    }

    /**
     * Tests publishing of checks in freestyle-project is skipped when skip publishing checks is true.
     */
    @Test
    void freeStyleSkipPublishingOfChecks() {
        FreeStyleProject project = getFreeStyleProjectWithJacoco(Checks.SKIP_CHECKS);
        checkConsoleLog(buildSuccessfully(project), Checks.SKIP_CHECKS);
    }

    /**
     * Tests publishing of checks in pipeline-project is skipped when skip publishing checks is true.
     */
    @Test
    void pipelineSkipPublishingOfChecks() {
        WorkflowJob job = getPipelineProjectWithJacoco(Checks.SKIP_CHECKS);
        checkConsoleLog(buildSuccessfully(job), Checks.SKIP_CHECKS);
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
     *
     * @return build of project with scm
     */
    private WorkflowJob getPipelineProjectWithJacoco(final Checks skipPublishingChecks) {
        String skipPublishingChecksValue = "";

        if (skipPublishingChecks == Checks.SKIP_CHECKS) {
            skipPublishingChecksValue = "true";
        }
        else if (skipPublishingChecks == Checks.PUBLISH_CHECKS) {
            skipPublishingChecksValue = "false";
        }

        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILENAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILENAME + "')],\n"
                + "    skipPublishingChecks: " + skipPublishingChecksValue
                + "}", true));

        return job;
    }

    /**
     * Enum to set skipping of publishing of checks.
     */
    enum Checks { PUBLISH_CHECKS, SKIP_CHECKS}
}
