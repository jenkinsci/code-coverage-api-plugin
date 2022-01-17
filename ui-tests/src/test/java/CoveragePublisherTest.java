import org.junit.Test;

import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.GlobalThresholdTarget;

//TODO: ueberdenken ob tests so sinn machen & ausreichen

/**
 * Acceptance tests for CoveragePublisher. Verifies if set options in CoveragePublisher are used and lead to excepted
 * results.
 */
public class CoveragePublisherTest extends UiTest {

    @Test
    public void testApplyThresholdRecursively() {
        //TODO
    }

    /**
     * Verifies that job with no report fails when setFailNoReports(true).
     */
    @Test
    public void testFailOnNoReport() {
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
    public void testFailOnDecreasedCoverage() {
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

    /**
     * Test if build fails if setFailUnhealthy is true and thresholds set.
     */
    @Test
    public void testAdapterThresholdsAndFailOnUnhealthySetter() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setSkipPublishingChecks(false);
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        AdapterThreshold threshold = jacocoAdapter.createThresholdsPageArea(AdapterThresholdTarget.INSTRUCTION,
                97,
                99, false);
        threshold.setFailUnhealthy(true);
        job.save();
        //TODO: Solle eigentlich Failen aber ist nur unstable
        buildWithErrors(job);
    }

    /**
     * Test if global thresholds are set.
     */
    @Test
    public void testGlobalThresholdsAndFailSetter() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setSkipPublishingChecks(false);
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        coveragePublisher.createGlobalThresholdsPageArea(GlobalThresholdTarget.INSTRUCTION,
                97,
                99, false);
        job.save();
        buildUnstable(job);
    }

    @Test
    public void testSourceFileStoringLevel() {
        //TODO
    }

}
