import org.junit.Test;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.GlobalThresholdTarget;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.FileCoverageTable;
import io.jenkins.plugins.coverage.MainPanel;

public class CoveragePublisherTest extends UiTest {

    @Test
    public void verifyApplyThresholdRecursively() {
        //TODO
    }

    @Test
    public void verifyFailOnNoReport() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        coveragePublisher.setFailNoReports(true);
        job.save();
        Build buildWithErrors = buildWithErrors(job);
    }

    @Test
    public void verifyFailOnDecreasedCoverage() {
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
    public void verifySkipPublishingChecks() {
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
