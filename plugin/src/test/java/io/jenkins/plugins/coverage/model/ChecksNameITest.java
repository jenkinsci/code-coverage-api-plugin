package io.jenkins.plugins.coverage.model;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jvnet.hudson.test.TestExtension;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.ExtensionList;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksPublisher;
import io.jenkins.plugins.checks.api.ChecksPublisherFactory;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests setting of SCM checks name.
 */
class ChecksNameITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILENAME = "jacoco-analysis-model.xml";
    private static final String DEFAULT_CHECKS_NAME = "Code Coverage";

    @ParameterizedTest(name = "{index} => checksName={0}")
    @NullSource
    @ValueSource(strings = {"Custom Checks Name", ""})
    void freeStylePublishingOfChecks(final String checksName) {
        FreeStyleProject project = getFreeStyleProject(checksName);
        assertSuccessfulBuild(buildSuccessfully(project));
        checkPublisherDetails(checksName);
    }

    @ParameterizedTest(name = "{index} => checksName={0}")
    @NullSource
    @ValueSource(strings = {"Custom Checks Name", ""})
    void pipelinePublishingOfCheckName(final String checksName) {
        WorkflowJob job = getPipelineProject(checksName);
        assertSuccessfulBuild(buildSuccessfully(job));
        checkPublisherDetails(checksName);
    }

    /**
     * Checks console log.
     *
     * @param checksName
     *         check name to use for publishing to SCM
     */
    private void checkPublisherDetails(final String checksName) {
        ChecksDetails checksDetails = ExtensionList.lookupSingleton(InterceptingChecksPublisherFactory.class)
                .getPublisher().getChecksDetail();
        if (checksName != null) {
            assertThat(checksDetails.getName())
                    .isPresent()
                    .get()
                    .isEqualTo(checksName);
        }
        else {
            assertThat(checksDetails.getName())
                    .isPresent()
                    .get()
                    .isEqualTo(DEFAULT_CHECKS_NAME);
        }
    }

    /**
     * Creates freestyle project with jacoco file and adapter.
     *
     * @param checksName
     *         check name to use for publishing to SCM
     *         if null, uses default checks name
     *
     * @return {@link FreeStyleProject} with specified check name.
     */
    private FreeStyleProject getFreeStyleProject(final String checksName) {
        FreeStyleProject job = createFreeStyleProjectWithWorkspaceFiles(JACOCO_FILENAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILENAME);
        if (checksName != null) {
            coveragePublisher.setChecksName(checksName);
            assertThat(coveragePublisher.getChecksName()).isEqualTo(checksName);
        }
        else {
            assertThat(coveragePublisher.getChecksName()).isEqualTo(DEFAULT_CHECKS_NAME);
        }
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        job.getPublishersList().add(coveragePublisher);
        return job;
    }

    /**
     * Creates a build of a pipeline project with jacoco file and adapter.
     *
     * @param checksName
     *         check name to use for publishing to SCM
     *         if null, uses default checks name
     *
     * @return {@link WorkflowJob} with specified check name.
     */
    private WorkflowJob getPipelineProject(final String checksName) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILENAME);
        String checksNameArg = "";

        if (checksName != null) {
            checksNameArg = ",\n    checksName: '" + checksName + "'";
        }
        job.setDefinition(new CpsFlowDefinition("node {"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILENAME + "')]" + checksNameArg
                + "}", true));
        return job;
    }

    static class InterceptingChecksPublisher extends ChecksPublisher {

        private ChecksDetails checksDetail = null;

        @Override
        public void publish(final ChecksDetails checksDetails) {
            this.checksDetail = checksDetails;
        }

        public ChecksDetails getChecksDetail() {
            return this.checksDetail;
        }
    }

    @TestExtension
    public static class InterceptingChecksPublisherFactory extends ChecksPublisherFactory {

        private final InterceptingChecksPublisher publisher = new InterceptingChecksPublisher();

        @Override
        protected Optional<ChecksPublisher> createPublisher(final Run<?, ?> run, final TaskListener listener) {
            return Optional.of(publisher);
        }

        @Override
        protected Optional<ChecksPublisher> createPublisher(final Job<?, ?> job, final TaskListener listener) {
            return Optional.of(publisher);
        }

        public InterceptingChecksPublisher getPublisher() {
            return this.publisher;
        }
    }
}
