package io.jenkins.plugins.coverage;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.plugins.git.GitScm;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.Job;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.AdapterThreshold.AdapterThresholdTarget;
import io.jenkins.plugins.coverage.CoveragePublisher.Threshold.GlobalThreshold.GlobalThresholdTarget;

/**
 * Base class for all UI tests. Provides several helper methods that can be used by all tests.
 */
class UiTest extends AbstractJUnitTest {
    static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    static final String JACOCO_FROM_COMMIT_XML = "jacoco-f52a5169.xml";
    static final String RESOURCES_FOLDER = "/";
    static final String REPO_URL = "https://github.com/jenkinsci/code-coverage-api-plugin.git";
    static final String COMMIT_ID = "f52a51691598600e2f42eee354ee6a540e008a72";

    /**
     * Returns a job without any reports in its configuration.
     *
     * @param configuration
     *         if build should fail due to no reports
     *
     * @return a job without any reports
     */
    FreeStyleJob getJobWithoutAnyReports(InCaseNoReportsConfiguration configuration) {
        if (configuration == InCaseNoReportsConfiguration.FAIL) {
            return createJobWithConfiguration(JobConfiguration.NO_REPORTS_SHOULD_FAIL);
        }
        else {
            return createJobWithConfiguration(JobConfiguration.NO_REPORTS);
        }
    }

    /**
     * Returns a job without any builds but with a report in its configuration.
     *
     * @return a job without any builds but with a report in its configuration
     */
    FreeStyleJob getJobWithReportInConfiguration() {
        return createJobWithConfiguration(JobConfiguration.FIRST_BUILD_ONE_JACOCO);
    }

    /**
     * Creates job with source code from a set commit and its jacoco file.
     *
     * @param sourceFileResolver
     *         level of source code storing
     *
     * @return job with source code and correct jacoco file
     */
    FreeStyleJob getJobWithReportAndSourceCode(final SourceFileResolver sourceFileResolver) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);

        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        coveragePublisher.setSourceCodeEncoding("UTF-8")
                .addSourceDirectory("checkout/src/main/java")
                .setSourceFileResolver(sourceFileResolver);

        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        jacocoAdapter.setReportFilePath(JACOCO_FROM_COMMIT_XML);

        job.useScm(GitScm.class)
                .url(REPO_URL)
                .branch(COMMIT_ID)
                .localDir("checkout");
        job.save();

        return job;
    }

    /**
     * Creates {@link FreeStyleJob} with threshold and jacoco adapter.
     *
     * @param unhealthyThreshold
     *         for threshold
     * @param unstableThreshold
     *         for threshold
     * @param failUnhealthy
     *         should build fail on unhealthy status
     * @param thresholdLevel
     *         level of threshold
     *
     * @return Job with threshold and jacoco adapter.
     */
    FreeStyleJob getJobWithAdapterThresholdAndFailOnUnhealthySetter(int unhealthyThreshold, int unstableThreshold,
            boolean failUnhealthy, ThresholdLevel thresholdLevel) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);
        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        if (thresholdLevel == ThresholdLevel.ADAPTER) {
            jacocoAdapter.createThresholdsPageArea(AdapterThresholdTarget.LINE,
                    unhealthyThreshold,
                    unstableThreshold, failUnhealthy);
        }
        else if (thresholdLevel == ThresholdLevel.GLOBAL) {
            coveragePublisher.createGlobalThresholdsPageArea(GlobalThresholdTarget.LINE,
                    unhealthyThreshold,
                    unstableThreshold, failUnhealthy);
        }
        job.save();
        return job;
    }

    /**
     * Returns a job with first build and second configuration, both with different reports.
     *
     * @param configuration
     *         to set if second build should fail due to decreasing coverage
     *
     * @return a job with first build and second configuration, both with different reports
     */
    FreeStyleJob getJobWithFirstBuildAndDifferentReports(final InCaseCoverageDecreasedConfiguration configuration) {
        if (configuration == InCaseCoverageDecreasedConfiguration.FAIL) {
            return createJobWithConfiguration(JobConfiguration.SECOND_BUILD_FAILED_DUE_TO_COVERAGE_DECREASED);
        }
        else {
            return createJobWithConfiguration(JobConfiguration.SECOND_BUILD_SUCCESSFUL_WITH_JACOCO);
        }
    }

    /**
     * Creates a job and its configuration depending on {@link JobConfiguration}.
     *
     * @param jobConfiguration
     *         which is needed
     *
     * @return job with chosen configuration
     */
    private FreeStyleJob createJobWithConfiguration(final JobConfiguration jobConfiguration) {

        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        CoveragePublisher coveragePublisher = job.addPublisher(CoveragePublisher.class);

        if (jobConfiguration == JobConfiguration.NO_REPORTS_SHOULD_FAIL
                || jobConfiguration == JobConfiguration.NO_REPORTS) {
            if (jobConfiguration == JobConfiguration.NO_REPORTS_SHOULD_FAIL) {
                coveragePublisher.setFailNoReports(true);
            }

            job.save();
            return job;

        }

        Adapter jacocoAdapter = coveragePublisher.createAdapterPageArea("Jacoco");
        copyResourceFilesToWorkspace(job, RESOURCES_FOLDER);
        jacocoAdapter.setReportFilePath(JACOCO_ANALYSIS_MODEL_XML);
        job.save();

        if (jobConfiguration == JobConfiguration.FIRST_BUILD_ONE_JACOCO) {
            return job;
        }

        buildSuccessfully(job);
        job.configure();
        jacocoAdapter.setReportFilePath(JACOCO_CODINGSTYLE_XML);

        if (jobConfiguration == JobConfiguration.SECOND_BUILD_FAILED_DUE_TO_COVERAGE_DECREASED) {
            coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        }

        job.save();
        return job;
    }

    /**
     * Build job and check if it is successfully.
     *
     * @param job
     *         to build
     *
     * @return successful build
     */
    Build buildSuccessfully(final Job job) {
        return job.startBuild().waitUntilFinished().shouldSucceed();
    }

    /**
     * Build job and check if it is unstable.
     *
     * @param job
     *         to build
     *
     * @return unstable build
     */
    Build buildUnstable(final Job job) {
        return job.startBuild().waitUntilFinished().shouldBeUnstable();
    }

    /**
     * Build job and check if it failed.
     *
     * @param job
     *         to build
     *
     * @return failed build
     */
    Build buildWithErrors(final Job job) {
        return job.startBuild().waitUntilFinished().shouldFail();
    }

    /**
     * Copies all files of given resources to workspace of a project.
     *
     * @param job
     *         in whose workspace files should be copies
     * @param resources
     *         of files which should be copied
     */
    void copyResourceFilesToWorkspace(final Job job, final String... resources) {
        for (String file : resources) {
            job.copyResource(file);
        }
    }

    /**
     * Enum for JobConfiguration for creating different FreeStyle jobs.
     */
    private enum JobConfiguration {
        NO_REPORTS,
        NO_REPORTS_SHOULD_FAIL,
        FIRST_BUILD_ONE_JACOCO,
        SECOND_BUILD_SUCCESSFUL_WITH_JACOCO,
        SECOND_BUILD_FAILED_DUE_TO_COVERAGE_DECREASED
    }

    /**
     * Enum for Job Configuration in case no reports are found.
     */
    enum InCaseNoReportsConfiguration {
        FAIL,
        DONT_FAIL
    }

    /**
     * Enum for Job Configuration in case coverage decreased in reference build.
     */
    enum InCaseCoverageDecreasedConfiguration {
        FAIL,
        DONT_FAIL
    }

    /**
     * Enum for threshold level.
     */
    enum ThresholdLevel {
        ADAPTER,
        GLOBAL
    }

}
