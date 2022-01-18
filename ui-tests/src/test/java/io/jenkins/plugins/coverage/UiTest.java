package io.jenkins.plugins.coverage;

import java.net.URL;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Resource;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Job;

/**
 * Base class for all UI tests. Provides several helper methods that can be used by all tests.
 */
class UiTest extends AbstractJUnitTest {
    static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    static final String RESOURCES_FOLDER = "/";

    /**
     * Build job and check if it is successfully.
     */
    Build buildSuccessfully(final Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }

    /**
     * Build job and check if it is unstable.
     */
    Build buildUnstable(final Job job) {
        return job.startBuild().waitUntilFinished().shouldBeUnstable();
    }

    /**
     * Build job and check if it fails.
     */
    Build buildWithErrors(final Job job) {
        return job.startBuild().waitUntilFinished().shouldFail();
    }

    /**
     * Copies all files of given resources to workspace of a project.
     * @param job in whose workspace files should be copies
     * @param resources of files which should be copied
     */
    void copyResourceFilesToWorkspace(final Job job, final String... resources) {
        for (String file : resources) {
            job.copyResource(file);
        }
    }

}
