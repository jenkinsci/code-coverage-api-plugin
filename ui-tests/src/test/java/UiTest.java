import java.net.URL;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Resource;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Job;

class UiTest extends AbstractJUnitTest {

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
