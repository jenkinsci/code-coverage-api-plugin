import org.junit.Test;

import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;

//TODO: ueberdenken ob tests so sinn machen & ausreichen

/**
 * Acceptance tests for CoveragePublisher.
 * Verifies if set options in CoveragePublisher are used and lead to excepted results.
 */
public class CoveragePublisherTest extends UiTest {

    @Test
    public void verifiesApplyThresholdRecursively() {
        //TODO
    }

    /**
     * Verifies that job with no report fails when setFailNoReports(true).
     */
    @Test
    public void verifiesFailOnNoReport() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setFailNoReports(true);
        job.save();
        buildWithErrors(job);
    }

    /**
     * Verifies that job with decreased coverage fails when setFailBuildIfCoverageDecreasedInChangeRequest(true).
     */
    @Test
    public void verifiesFailOnDecreasedCoverage() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        job.save();
        buildWithErrors(job);
    }


    @Test
    public void verifiesSkipPublishingChecks() {
        //TODO
    }

    @Test
    public void verifiesAdapterThresholdsAndFailOnUnhealthySetter() {
        //TODO
    }


    @Test
    public void verifiesGlobalThresholdsAndFailSetter() {
        //TODO
    }


    @Test
    public void verifySourceFileStoringLevel(){
        //TODO
    }

}
