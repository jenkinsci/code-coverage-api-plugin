import java.util.HashMap;

import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoverageSummary;
import io.jenkins.plugins.coverage.JobStatus;

import static org.assertj.core.api.Assertions.*;

public class SummaryTest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    @Test
    public void verifyGeneratedTrendChart() {
        //createBuildWithCoverage();
        getTrendChartFromJobWithMultipleBuilds();
        //failBuild();
    }


    private void createBuildWithCoverage() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        build.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        HashMap<String, Double> coverage = cs.getCoverage();

        assertThat(coverage)
                .hasSize(2)
                .containsKeys("Line", "Branch")
                .containsValues(95.52, 88.59);
    }

    private void getTrendChartFromJobWithMultipleBuilds() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();
        Build build = JobCreatorUtils.buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);
        job.save();
        Build build2 = JobCreatorUtils.buildSuccessfully(job);
        build2.open();
        CoverageSummary cs = new CoverageSummary(build, "coverage");
        cs.openReferenceBuild();

        System.out.println("HI");
    }

    private void failBuild() {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        JobCreatorUtils.copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        jacocoAdapter.setMergeToOneReport(true);
        jacocoAdapter.createGlobalThresholdsPageArea("Instruction", 4, 4, false);
        coveragePublisher.setApplyThresholdRecursively(true);
        coveragePublisher.setFailUnhealthy(true);
        coveragePublisher.setFailUnstable(true);
        coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        coveragePublisher.setFailNoReports(true);
        coveragePublisher.setFailNoReports(true);
        job.save();
        Build build = JobCreatorUtils.buildWithErrors(job);
        build.open();
        System.out.println("HI");

    }
}
