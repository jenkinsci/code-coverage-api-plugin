package io.jenkins.plugins.coverage.model;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.apache.http.impl.auth.GGSSchemeBase;
import org.codehaus.groovy.tools.shell.util.JAnsiHelper;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/*
Pipeline integration tests for coverage api plugin

 */
public class CoveragePluginPipelineITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    private static final String JACOCO_BIG_DATA = "jacoco-analysis-model.xml";
    private static final String JACOCO_SMALL_DATA = "jacoco.xml";
    private static final String JACOCO_MINI_DATA = "jacocoModifiedMini.xml";
    private static final String COBERTURA_SMALL_DATA = "cobertura-coverage.xml";
    private static final String COBERTURA_BIG_DATA = "coverage-with-lots-of-data.xml";

    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";
    private static final String REPOSITORY = "https://github.com/jenkinsci/analysis-model.git";

    /** Docker container for java-maven builds. Contains also git to check out from an SCM. */
    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    @Test
    public void noJacocoFile() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

       Run<?, ?> build = buildSuccessfully(job);
       assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    public void oneJacocoFile(){
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
                .isEqualTo(new Coverage(11400, 11947 - 11400));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3306, 3620 - 3306));
    }

    @Test
    public void noCoberturaFile() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    public void oneCobertura(){
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

    @Test
    public void healthReportingHealthy(){
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

    @Test
    public void healthReportingUnstable(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.UNSTABLE);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
    }

   @Test
    public void healthReportingUnhealthySuccess(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 96.0]]"
                + "}", true));

       Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
       HealthReportingAction x = build.getAction(HealthReportingAction.class);

       assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
       assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
    }

    @Test
    public void healthReportingUnhealthyFailure(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failUnstable: true,"
                + "globalThresholds: [[thresholdTarget: 'Line', unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    public void failNoReportsTrue() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles();
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void failNoReportsFalse() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles();
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: false"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    private String getLogFromInputStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Test
    public void qualityGatesAdapterThresholdFailUnhealthy() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]}"
                , true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);

        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void qualityGatesAdapterThresholdSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 42.0]])]}"
                , true));

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

    @Test
    public void qualityGatesAdapterThresholdSuccessUnhealthy() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]}"
                , true));

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

    @Test
    public void qualityGatesAdapterThresholdUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unstableThreshold: 99.0]])]}"
                , true));

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

    @Test
    public void qualityGatesGlobalThresholdFailUnhealthy() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml')], "
                        + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 100.0]]}"
                , true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);

        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void qualityGatesGlobalThresholdSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml')], "
                        + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 0.0]]}"
                , true));

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

    @Test
    public void qualityGatesGlobalThresholdSuccessUnhealthy() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]}"
                , true));

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

    @Test
    public void qualityGatesGlobalThresholdUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml')], "
                        + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unstableThreshold: 100.0]]}"
                , true));

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

    @Test
    public void failDecreasingCoverageTrue(){
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

    @Test
    public void skipPublishingChecksTrue () throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "skipPublishingChecks: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);



        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));

        assertThat(getLogFromInputStream(build.getLogInputStream())).doesNotContain("No suitable checks publisher found");
    }

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

        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No suitable checks publisher found");
    }

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

    @Test
    public void deltaComputationZeroDelta(){
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

    @Test
    public void deltaComputationSingleBuild(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(workflowJob);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("n/a");
    }

    @Test
    public void deltaComputationUseOnlyPreviousAndCurrent(){
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

    @Test
    public void reportAggregationTrue() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("A total of 1 reports were found");
    }

    @Test
    public void reportAggregationFalse() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter(mergeToOneReport: false, path: '*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("A total of 2 reports were found");
    }

    @Test
    public void agentInDocker() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, JACOCO_BIG_DATA, JACOCO_BIG_DATA);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        String consoleLog = getConsoleLog(build);
        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("git checkout -f " + COMMIT);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    private WorkflowJob createPipelineOnAgent() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node('docker') {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c' ]],\n"
                + "userRemoteConfigs: [[url: '" + "https://github.com/jenkinsci/analysis-model.git" + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_BIG_DATA + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}", true));

        return job;
    }

    /**
     * Creates a docker container agent.
     *
     * @param dockerContainer
     *         the docker container of the agent
     *
     * @return A docker container agent.
     */
    @SuppressWarnings({"PMD.AvoidCatchingThrowable", "IllegalCatch"})
    protected DumbSlave createDockerContainerAgent(final DockerContainer dockerContainer) {
        try {
            SystemCredentialsProvider.getInstance().getDomainCredentialsMap().put(Domain.global(),
                    Collections.singletonList(
                            new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "dummyCredentialId",
                                    null, "test", "test")
                    )
            );
            DumbSlave agent = new DumbSlave("docker", "/home/test",
                    new SSHLauncher(dockerContainer.ipBound(22), dockerContainer.port(22), "dummyCredentialId"));
            agent.setNodeProperties(Collections.singletonList(new EnvironmentVariablesNodeProperty(
                    new EnvironmentVariablesNodeProperty.Entry("JAVA_HOME", "/usr/lib/jvm/java-8-openjdk-amd64/jre"))));
            getJenkins().jenkins.addNode(agent);
            getJenkins().waitOnline(agent);

            return agent;
        }
        catch (Throwable e) {
            throw new AssumptionViolatedException("Failed to create docker container", e);
        }
    }

    @Test
    public void sourceCodeRenderingAndCopying() {
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
                21, 306, 344, 1801, 6083, 26283, 1661
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 1, 5, 48, 285, 1036, 214
        );
    }

    @Test
    public void sourceCodeRenderingAndCopyingAgent() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, JACOCO_BIG_DATA, JACOCO_BIG_DATA);

        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);

        String consoleLog = getConsoleLog(build);

        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("git checkout -f " + COMMIT);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);

        CoverageViewModel model = action.getTarget();

        CoverageViewModel.CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "Package", "File", "Class", "Method", "Line", "Instruction", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                21, 306, 344, 1801, 6083, 26283, 1661
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 1, 5, 48, 285, 1036, 214
        );
    }

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
