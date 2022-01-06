import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Resource;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.WorkflowJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.Irgendwie;

public class UITest extends AbstractJUnitTest {
    private static final String SOURCE_VIEW_FOLDER = "/source-view/";

    @SuppressFBWarnings("BC")
    private static final String FILE_NAME = "jacoco-analysis-model.xml";
    private static final String REPOSITORY_URL = "https://github.com/jenkinsci/git-forensics-plugin.git";
    

    private byte[] readAllBytes(final String fileName) {
        try {
            return Files.readAllBytes(getPath(fileName));
        }
        catch (IOException | URISyntaxException e) {
            throw new AssertionError("Can't read resource " + fileName, e);
        }
    }

    Path getPath(final String name) throws URISyntaxException {
        URL resource = getClass().getResource(name);
        if (resource == null) {
            throw new AssertionError("Can't find resource " + name);
        }
        return Paths.get(resource.toURI());
    }


    public Resource resource(String path) {
        URL resource = this.getClass().getResource(path);
        if (resource == null) {
            throw new AssertionError("No such resource " + path + " for " + this.getClass().getName());
        } else {
            return new Resource(resource);
        }
    }

    @Test
    public void helloWorld() {
        // WorkflowJob job = createJob(FILE_NAME);
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);

        copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage/" + "jacoco-analysis-model.xml");

        job.addPublisher(CoveragePublisher.class, publisher -> {
            publisher.createAdapterPageArea("Jacoco").setReportFilePath("**/*.xml");
        });

        job.save();
        Build build = buildSuccessfully(job);
        Irgendwie CodeCoverage = new Irgendwie(build, "codecoverage");
        System.out.println("f");
        CodeCoverage.open();
        //assertThat(CodeCoverage.existsCodeCoverageInformation()).isTrue();

    }


    protected void copyResourceFilesToWorkspace(final Job job, final String... resources) {
        for (String file : resources) {
            job.copyResource(file);
        }
    }



    protected Build buildSuccessfully(final Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }

    private WorkflowJob createJob(final String... resourcesToCopy) {
        WorkflowJob job = jenkins.jobs.create(WorkflowJob.class);

      /*  for (String resource : resourcesToCopy) {
            job.copyResource(CODE_COVERAGE_PLUGIN_PREFIX + resource);
        }*/
        //job.sandbox.check();
        job.script.set("node {\n"
                + "  checkout([$class: 'GitSCM', branches: [[name: '28af63def44286729e3b19b03464d100fd1d0587' ]],\n"
                + "     userRemoteConfigs: [[url: '" + REPOSITORY_URL + "']]])\n"
                + "  publishCoverage adapters: [jacocoAdapter('**/jacoco-analysis-model.xml')] \n"
                + "} \n");



        return job;
    }

}

