import java.net.URL;

import org.jenkinsci.test.acceptance.junit.Resource;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Job;

class JobCreatorUtils {

    /**
     * Copies all files of given resources to workspace of a project.
     * @param job in whose workspace files should be copies
     * @param resources of files which should be copied
     */
    static protected void copyResourceFilesToWorkspace(final Job job, final String... resources) {
        for (String file : resources) {
            job.copyResource(file);
        }
    }


    /**
     * Return resource of given path.
     *
     * @param path
     *         which resource is requested
     *
     * @return resource of given path.
     */
    public Resource resource(final String path) {
        URL resource = this.getClass().getResource(path);
        if (resource == null) {
            throw new AssertionError("No such resource " + path + " for " + this.getClass().getName());
        }
        else {
            return new Resource(resource);
        }
    }

    /**
     * Check if Project is build successfully.
     */
    static protected Build buildSuccessfully(final Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }


}
