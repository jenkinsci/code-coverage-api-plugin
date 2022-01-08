import java.net.URISyntaxException;
import java.net.URL;


import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.Resource;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.CoverageSummary;
import io.jenkins.plugins.coverage.Irgendwie;

public class UITest extends AbstractJUnitTest {
    private static final String SOURCE_VIEW_FOLDER = "/source-view/";

    @SuppressFBWarnings("BC")
    private static final String FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Creates a first test which returns ...
     * (-> siehe jelly) //TODO: javadoc hier später anpassen
     */
    @Test
    public void createJob() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage/jacoco-analysis-model.xml");
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(FILE_NAME);
        /*jacocoAdapter.setMergeToOneReport(true);
        jacocoAdapter.createGlobalThresholdsPageArea("Instruction", 4, 4, false);
        coveragePublisher.setApplyThresholdRecursively(true);
        coveragePublisher.setFailUnhealthy(true);
        coveragePublisher.setFailUnstable(true);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        coveragePublisher.setFailNoReports(true);
        coveragePublisher.setFailNoReports(true);
        coveragePublisher.setSourceFileResolver(SourceFileResolver.NEVER_SAVE_SOURCE_FILES);*/
        job.save();
        Build build = buildSuccessfully(job);
        build.open();
        CoverageSummary summary = new CoverageSummary(build, "coverage");
        //cs.openCoverageReport();
        CoverageReport report = summary.openCoverageReport();
        report.getActiveTab();
        report.openCoverageTable();
        //report.openCoverageTree();

        //report.verfiesOverview();
        //String coverageTable = report.getCoverageTable();
        //String coverageDetails = report.getCoverageDetails();

        //String coverageTrend = report.getCoverageTrend();
        //String coverageOverview = report.getCoverageOverview();

        //boolean coverageTreeVisible = report.isCoverageTreeVisible();

       // cr.verfiesOverview();

        //Irgendwie CodeCoverage = new Irgendwie(build, "coverage");
        //CodeCoverage.open();
    }



    @Test
    public void createJobForPreparingFirstTestsOfCoverageReport() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        copyResourceFilesToWorkspace(job, "/io.jenkins.plugins.coverage/jacoco-analysis-model.xml");
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(FILE_NAME);
        job.save();
        Build build = buildSuccessfully(job);
        build.open();
        CoverageSummary summary = new CoverageSummary(build, "coverage");
        CoverageReport report = summary.openCoverageReport();
        report.getActiveTab();
        report.openCoverageTable();
        report.openCoverageTree();
        String coverageTrend = report.getCoverageTrend();
        String coverageOverview = report.getCoverageOverview();

    }

    /**
     * Copies all files of given resources to workspace of a project.
     * @param job in whose workspace files should be copies
     * @param resources of files which should be copied
     */
    protected void copyResourceFilesToWorkspace(final Job job, final String... resources) {
        for (String file : resources) {
            job.copyResource(file);
        }
    }

    /**
     * Return resource of given path.
     * @param path which resource is requested
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
    protected Build buildSuccessfully(final Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }

    /*
    private static final String REPOSITORY_URL = "https://github.com/jenkinsci/git-forensics-plugin.git";

    private WorkflowJob createPipelineJob(final String... resourcesToCopy) {
        WorkflowJob job = jenkins.jobs.create(WorkflowJob.class);
        //TODO: copy to workspace
        job.script.set("node {\n"
                + "  checkout([$class: 'GitSCM', branches: [[name: '28af63def44286729e3b19b03464d100fd1d0587' ]],\n"
                + "     userRemoteConfigs: [[url: '" + REPOSITORY_URL + "']]])\n"
                + "  publishCoverage adapters: [jacocoAdapter('TODO: jacoco-analysis-model.xml')] \n"
                + "} \n");
        return job;
    }
    */

        /*
    private byte[] readAllBytes(final String fileName) {
        try {
            return Files.readAllBytes(getPath(fileName));
        }
        catch (IOException | URISyntaxException e) {
            throw new AssertionError("Can't read resource " + fileName, e);
        }

        Path getPath(final String name) throws URISyntaxException {
        URL resource = getClass().getResource(name);
        if (resource == null) {
            throw new AssertionError("Can't find resource " + name);
        }
        return Paths.get(resource.toURI());
    }

    }*/

}

