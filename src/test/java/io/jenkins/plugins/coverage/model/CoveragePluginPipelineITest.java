package io.jenkins.plugins.coverage.model;

import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pipeline integration tests for the CoveragePlugin.
 *
 * @author Johannes Walter, Katharina Winkler
 */
public class CoveragePluginPipelineITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_BIG_DATA = "jacoco-analysis-model.xml";
    private static final String JACOCO_SMALL_DATA = "jacoco.xml";
    private static final String JACOCO_MINI_DATA = "jacocoModifiedMini.xml";
    private static final String COBERTURA_SMALL_DATA = "cobertura-coverage.xml";
    private static final String COBERTURA_BIG_DATA = "coverage-with-lots-of-data.xml";
    private static final TestUtil TEST_UTIL = new TestUtil();

    /**
     * Tests the Pipeline with Jacoco Adapter and no input files.
     */
    @Test
    public void noJacocoFile() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    /**
     * Tests the Pipeline with Jacoco Adapter and one input files.
     */
    @Test
    public void oneJacocoFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the Pipeline with Jacoco Adapter and two input files.
     */
    @Test
    public void twoJacocoFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(11_400, 11_947 - 11_400));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3306, 3620 - 3306));
    }

    /**
     * Tests the Pipeline with Cobertura Adapter and no input files.
     */
    @Test
    public void noCoberturaFile() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    /**
     * Tests the Pipeline with Cobertura Adapter and one input files.
     */
    @Test
    public void oneCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_BIG_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(602, 958 - 602));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(285, 628 - 285));
    }

    /**
     * Tests the Pipeline with Cobertura Adapter and two input files.
     */
    @Test
    public void twoCoberturaFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_BIG_DATA, COBERTURA_SMALL_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(604, 960 - 604));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(285, 628 - 285));
    }

    /**
     * Tests the Pipeline with Jacoco and Cobertura Adapter and input files for each.
     */
    @Test
    public void oneJacocoOneCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, COBERTURA_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml'),jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6685, 7326 - 6685));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1946, 2503 - 1946));
    }

    /**
     * Tests the health reporting whether the build is successful and healthy.
     */
    @Test
    public void healthReportingHealthy() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 94.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
    }

    /**
     * Tests the health reporting whether the build is unstable.
     */
    @Test
    public void healthReportingUnstable() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.UNSTABLE);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
    }

    /**
     * Tests the health reporting whether the build is successful and unhealthy.
     */
    @Test
    public void healthReportingUnhealthySuccess() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
    }

    /**
     * Tests the health reporting whether the build is failed due to unstable and fail when unstable.
     */
    @Test
    public void healthReportingUnhealthyFailure() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failUnstable: true,"
                + "globalThresholds: [[thresholdTarget: 'Line', unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    /**
     * Tests the reports whether build successful if no reports are found.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void failNoReportsTrue() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles();
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    /**
     * Tests the reports whether build fails if no reports are found.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void failNoReportsFalse() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles();
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: false"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    /**
     * Tests the adapter quality gates whether the build fails if unhealthy.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void qualityGatesAdapterThresholdFailUnhealthy() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]"
                        + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    /**
     * Tests the adapter quality gates whether the build is successful if all gates are met.
     */
    @Test
    public void qualityGatesAdapterThresholdSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 42.0]])]"
                        + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the adapter quality gates whether the build is successful but unhealthy.
     */
    @Test
    public void qualityGatesAdapterThresholdSuccessUnhealthy() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]"
                        + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the adapter quality gates whether the build is unstable.
     */
    @Test
    public void qualityGatesAdapterThresholdUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unstableThreshold: 99.0]])]"
                        + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the global quality gates whether the build is successful if all gates are met.
     */
    @Test
    public void qualityGatesGlobalThresholdFailUnhealthy() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml')], "
                        + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line'"
                        + ", unhealthyThreshold: 100.0]]"
                        + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    /**
     * Tests the global quality gates whether the build is successful if all gates are met.
     */
    @Test
    public void qualityGatesGlobalThresholdSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml')], "
                        + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 0.0]]"
                        + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the global quality gates whether the build is successful but unhealthy.
     */
    @Test
    public void qualityGatesGlobalThresholdSuccessUnhealthy() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]"
                        + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the global quality gates whether the build is unstable.
     */
    @Test
    public void qualityGatesGlobalThresholdUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml')], "
                        + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unstableThreshold: 100.0]]"
                        + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests whether the build fails if coverage is decreasing.
     */
    @Test
    public void failDecreasingCoverageTrue() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));
        buildSuccessfully(job);

        cleanWorkspace(job);
        copyFilesToWorkspace(job, JACOCO_MINI_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "   failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));

        buildWithResult(job, Result.FAILURE);
    }

    /**
     * Tests whether the build doesn't fail if coverage is decreasing.
     */
    @Test
    public void failDecreasingCoverageFalse() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));
        buildSuccessfully(job);

        cleanWorkspace(job);
        copyFilesToWorkspace(job, JACOCO_MINI_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "   failBuildIfCoverageDecreasedInChangeRequest: false"
                + "}", true));

        buildWithResult(job, Result.SUCCESS);
    }

    /**
     * Tests whether the publishing of checks is skipped.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void skipPublishingChecksTrue() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "skipPublishingChecks: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);



        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream()))
                .doesNotContain("No suitable checks publisher found");
    }

    /**
     * Tests whether the publishing of checks is not skipped.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void skipPublishingChecksFalse() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "skipPublishingChecks: false"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("No suitable checks publisher found");
    }

    /**
     * Tests the delta computing of two builds each one input file.
     */
    @Test
    public void deltaComputation() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        buildSuccessfully(workflowJob);

        cleanWorkspace(workflowJob);
        copyFilesToWorkspace(workflowJob, JACOCO_MINI_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]\n"
                + "discoverReferenceBuild(referenceJob: '" + workflowJob.getFullName() + "')"
                + "}", true));

        Run<?, ?> secondBuild = buildSuccessfully(workflowJob);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("-0.002");
    }

    /**
     * Tests the delta computing of two builds each one the same input file.
     */
    @Test
    public void deltaComputationZeroDelta() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        buildSuccessfully(workflowJob);

        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]\n"
                + "discoverReferenceBuild(referenceJob: '" + workflowJob.getFullName() + "')"
                + "}", true));

        Run<?, ?> secondBuild = buildSuccessfully(workflowJob);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("+0.000");
    }

    /**
     * Tests the delta computing of one build.
     */
    @Test
    public void deltaComputationSingleBuild() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(workflowJob);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("n/a");
    }

    /**
     * Tests whether the delta computing uses only the current and previous build.
     */
    @Test
    public void deltaComputationUseOnlyPreviousAndCurrent() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_SMALL_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        buildSuccessfully(workflowJob);

        cleanWorkspace(workflowJob);
        copyFilesToWorkspace(workflowJob, JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]\n"
                + "discoverReferenceBuild(referenceJob: '" + workflowJob.getFullName() + "')"
                + "}", true));

        buildSuccessfully(workflowJob);

        cleanWorkspace(workflowJob);
        copyFilesToWorkspace(workflowJob, JACOCO_MINI_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]\n"
                + "discoverReferenceBuild(referenceJob: '" + workflowJob.getFullName() + "')"
                + "}", true));

        Run<?, ?> thirdBuild = buildSuccessfully(workflowJob);

        CoverageBuildAction coverageResult = thirdBuild.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("-0.002");
    }

    /**
     * Tests the reference build when there's only one single build.
     */
    @Test
    public void referenceBuildSingleBuild() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_SMALL_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(workflowJob);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isEmpty();
    }

    /**
     * Tests whether the reference build is the previous build.
     */
    @Test
    public void referenceBuildReferenceIsPrevious() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));
        Run<?, ?> firstBuild = buildSuccessfully(job);

        cleanWorkspace(job);
        copyFilesToWorkspace(job, JACOCO_MINI_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> secondBuild = buildWithResult(job, Result.SUCCESS);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isPresent();
        assertThat(coverageResult.getReferenceBuild().get()).isEqualTo(firstBuild);
    }

    /**
     * Tests if reports are aggregated.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void reportAggregationTrue() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("A total of 1 reports were found");
    }

    /**
     * Tests if reports are not aggregated.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     */
    @Test
    public void reportAggregationFalse() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter(mergeToOneReport: false, path: '*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("A total of 2 reports were found");
    }

    /**
     * Tests the source code rendering.
     */
    @Test
    public void sourceCodeRendering() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);

        CoverageViewModel model = action.getTarget();

        CoverageViewModel.CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "Package", "File", "Class", "Method", "Line", "Instruction", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                21, 306, 344, 1801, 6083, 26_283, 1_661
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 1, 5, 48, 285, 1_036, 214
        );
    }

    /**
     * Tests the declarative pipeline.
     */
    @Test
    public void declarativePipeline() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);

        workflowJob.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "agent any\n"
                + "stages {\n"
                + "stage('Test') {\n"
                + "steps {\n"
                + "publishCoverage(\n"
                + "adapters: [jacocoAdapter('" + "**/*.xml" + "')])}}}}", true));
        Run<?, ?> build = buildSuccessfully(workflowJob);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Tests the multiple invocations with no tags set.
     * @throws IOException from {@link CoverageProcessor}
     * @throws ClassNotFoundException from {@link CoverageProcessor}
     */
    @Test
    public void withNoTag() throws IOException, ClassNotFoundException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);

        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('" + JACOCO_BIG_DATA + "')]\n"
                + "publishCoverage adapters: [jacocoAdapter('" + JACOCO_SMALL_DATA + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(workflowJob);
        CoverageResult result = CoverageProcessor.recoverCoverageResult(build);

        Optional<String> tagKey1 = result.getChildrenReal().keySet().stream()
                        .filter(x -> x.contains(JACOCO_BIG_DATA))
                                .findFirst();
        assertThat(tagKey1).isPresent();
        assertThat(result.getChildrenReal().get(tagKey1.get()).getTag()).isEqualTo(null);

        Optional<String> tagKey2 = result.getChildrenReal().keySet().stream()
                .filter(x -> x.contains(JACOCO_SMALL_DATA))
                .findFirst();
        assertThat(tagKey2).isPresent();
        assertThat(result.getChildrenReal().get(tagKey2.get()).getTag()).isEqualTo(null);
    }

    /**
     * Tests the multiple invocations with tags set.
     * @throws IOException from {@link CoverageProcessor}
     * @throws ClassNotFoundException from {@link CoverageProcessor}
     */
    @Test
    public void withTag() throws IOException, ClassNotFoundException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_BIG_DATA + "')],"
                + "   tag: 'Elephant'\n"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_SMALL_DATA + "')],"
                + "   tag: 'Tiger'\n"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(workflowJob);

        CoverageResult result = CoverageProcessor.recoverCoverageResult(build);

        Optional<String> tagKey1 = result.getChildrenReal().keySet().stream()
                .filter(x -> x.contains(JACOCO_BIG_DATA))
                .findFirst();
        assertThat(tagKey1).isPresent();
        assertThat(result.getChildrenReal().get(tagKey1.get()).getTag()).isEqualTo("Elephant");

        Optional<String> tagKey2 = result.getChildrenReal().keySet().stream()
                .filter(x -> x.contains(JACOCO_SMALL_DATA))
                .findFirst();
        assertThat(tagKey2).isPresent();
        assertThat(result.getChildrenReal().get(tagKey2.get()).getTag()).isEqualTo("Tiger");
    }
}
