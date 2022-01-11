import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.coverage.AvailableMetrics;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.Metrics;

public class MetricsTest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    /**
     * Test for checking the Metrics by verifying its project overview, 
     * metrics distribution and metrics details
     * Uses a project with two different jacoco files, each one used in another build.
     * First build uses {@link MetricsTest#JACOCO_ANALYSIS_MODEL_XML},
     * Second build uses {@link MetricsTest#JACOCO_CODINGSTYLE_XML}.
     */
    @Test
    public void runFullMetricsTest() {
        Build secondBuild = createSuccessfulJobWithDiffererntJacocos().getLastBuild();
        Metrics metrics = new Metrics(secondBuild);
        metrics.open();
        AvailableMetrics availableMetrics = metrics.openAvailableMetrics();
        
       
        verifyProjectOverview("");
        verifyMetricsDistribution("");
        verifyMetricsDetails("");
    }

    private void verifyMetricsDetails(final String s) {
    }

    private void verifyMetricsDistribution(final String s) {
    }

    private void verifyProjectOverview(final String s) {
    }

    //TODO: temporary; should later be used for a couple of tests
    /**
     *  Builds a project with two different jacoco files, each one used in another build.
     *  First build uses {@link MetricsTest#JACOCO_ANALYSIS_MODEL_XML},
     *  Second build uses {@link MetricsTest#JACOCO_CODINGSTYLE_XML}.
     * @return
     */
    private FreeStyleJob createSuccessfulJobWithDiffererntJacocos() {
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
        return job;
    }

}
