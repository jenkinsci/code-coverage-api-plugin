package io.jenkins.plugins.coverage;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Job;

/**
 * Base class for all UI tests. Provides several helper methods that can be used by all tests.
 */
class UiTest extends AbstractJUnitTest {
    static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    static final String JACOCO_OLD_COMMIT_XML = "jacoco-f52a5169.xml";
    static final String RESOURCES_FOLDER = "/";

    /**
     * Build job and check if it is successfully.
     * @param job to build
     * @return successful build
     */
    Build buildSuccessfully(Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }

    /**
     * Build job and check if it is unstable.
     * @param job to build
     * @return unstable build
     */
    Build buildUnstable(Job job) {
        return job.startBuild().waitUntilFinished().shouldBeUnstable();
    }

    /**
     * Build job and check if it failed.
     * @param job to build
     * @return failed build
     */
    Build buildWithErrors(Job job) {
        return job.startBuild().waitUntilFinished().shouldFail();
    }

    /**
     * Copies all files of given resources to workspace of a project.
     * @param job in whose workspace files should be copies
     * @param resources of files which should be copied
     */
    void copyResourceFilesToWorkspace(Job job, String... resources) {
        for (String file : resources) {
            job.copyResource(file);
        }
    }

}
