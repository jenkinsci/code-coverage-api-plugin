import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.JobStatus;

/**
 * Should in the end contain all tests.
 */
public class UITest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    @SuppressFBWarnings("BC")
    private static final String FILE_NAME = "jacoco-analysis-model.xml";



    @Test
    public void tryChangeingFailBuild() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        JobCreatorUtils.buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        JobCreatorUtils.buildSuccessfully(job);

        /*
        Test Trendcharts, test CoverageReport, test CoverageSummary

        //SummaryTest.verify()
        //SummaryTest.verify()
        //CoverageReportTest.verify()
        //CoverageReportTest.verify()

         */
        job.configure();
        jacocoAdapter.createGlobalThresholdsPageArea("Instruction", 4, 4, false);
        coveragePublisher.setApplyThresholdRecursively(true);
        coveragePublisher.setFailUnhealthy(true);
        coveragePublisher.setFailUnstable(true);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        coveragePublisher.setFailNoReports(true);
        coveragePublisher.setFailNoReports(true);
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        JobCreatorUtils.buildWithErrors(job);


        //SummaryTest.verify()
        //SummaryTest.verify()
        //SummaryTest.verify()
        //SummaryTest.verify()


        // test fail on no report
        //job.configure();
        //FIXME
        //coveragePublisher.setFailNoReports(true);
        //TODO: refactor
        //coveragePublisher.deleteAdapter();


        job.save();
        JobCreatorUtils.buildSuccessfully(job);

    }



}

