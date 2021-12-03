package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class CoveragePluginPipelineITest extends IntegrationTestWithJenkinsPerSuite {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);


    @Test
    public void pipelineJacocoWithNoFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                        + "}", Result.SUCCESS);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isNull();
    }

    @Test
    public void pipelineJacocoWithOneFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePluginITestUtil.verifySimpleCoverageNode(build,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL - CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED);
    }

    @Test
    public void pipelineJacocoWithTwoFiles() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    @Test
    public void pipelineCoberturaWithNoFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isNull();
    }

    @Test
    public void pipelineCoberturaWithOneFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePluginITestUtil.verifySimpleCoverageNode(build,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED, CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_TOTAL - CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED);
    }

    @Test
    public void pipelineCoberturaWithTwoFiles() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED),
                build);
    }

    @Test
    public void pipelineCoberturaAndJacocoFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml'), cobertura('**/*.xml')]"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL, CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED, CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED), build);
    }

    @Test
    public void pipelineZeroReportsFail() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('*.xml')], failNoReports: true, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.FAILURE);
    }

    @Test
    public void pipelineZeroReportsOkay() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('*.xml')], failNoReports: false, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS);
    }

    @Test
    public void pipelineQualityGatesSuccess() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', thresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    @Test
    public void pipelineQualityGatesSuccessUnhealthy() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', thresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(0);
    }

    @Test
    public void pipelineQualityGatesUnstable() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml', thresholds: [[thresholdTarget: 'Line', unstableThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.UNSTABLE, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    @Test
    public void pipelineFailWhenCoverageDecreases() {

        // TODO: fails
        Run<?, ?> firstBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], failBuildIfCoverageDecreasedInChangeRequest: true"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        Run<?, ?> secondBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], failBuildIfCoverageDecreasedInChangeRequest: true"
                        + "}", Result.FAILURE, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        assertThat(secondBuild.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void pipelineSkipPublishingChecks() throws IOException {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], skipPublishingChecks: true, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .doesNotContain("[Checks API] No suitable checks publisher found.");
    }

    @Test
    public void pipelinePublishingChecks() throws IOException {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], skipPublishingChecks: false, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .contains("[Checks API] No suitable checks publisher found.");
    }

    @Test
    public void pipelineQualityGatesFail() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml', thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.FAILURE, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    @Test
    public void pipelineHealthReport() {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(100);
    }

    @Test
    public void pipelineReportAggregation() {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        int covered = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED;
        int total = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL;
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(covered, total - covered));

        // TODO: Niko
    }

    @Test
    public void pipelineDeltaComputation() {

        // TODO: PipelineDeltaComputation: not working
        Run<?, ?> firstBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        Run<?, ?> secondBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        CoverageBuildAction secondCoverageBuild = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(secondCoverageBuild.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    @Test
    public void pipelineReferenceBuildPresent() {

        // TODO: doesnt work yet
        Run<?, ?> firstBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        Run<?, ?> secondBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        CoverageBuildAction secondCoverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(secondCoverageResult.getReferenceBuild()).isPresent();
        Run<?, ?> referenceBuild = secondCoverageResult.getReferenceBuild().get();
        assertThat(referenceBuild).isEqualTo(firstBuild);
    }

    @Test
    public void pipelineReferenceBuildEmpty() {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getReferenceBuild()).isEmpty();
    }

    @Test
    public void pipelineSourceCodeCopying() throws Exception {
        DumbSlave agent = j.createOnlineSlave();

        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline-source-code-copying-test");
        job.setDefinition(new CpsFlowDefinition("node {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c' ]],\n"
                + "userRemoteConfigs: [[url: '" + "https://github.com/jenkinsci/analysis-model.git" + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        String consoleLog = getConsoleLog(build);
    }

    @Test
    public void pipelineSourceCodeCopyingAlt() throws Exception {
        DumbSlave agent = j.createOnlineSlave();

        WorkflowJob job = j.createProject(WorkflowJob.class, "pipeline-source-code-copying-test");

        String script = "node {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c' ]],\n"
                + "userRemoteConfigs: [[url: '" + "https://github.com/jenkinsci/analysis-model.git" + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}";

//        FilePath workspace = agent.getWorkspaceFor(job);

        job.setDefinition(new CpsFlowDefinition(script, true));

        WorkflowRun r = Objects.requireNonNull(job.scheduleBuild2(0)).waitForStart();

        String relativeSourcePath = "package.json";

        Assert.assertNotNull(r);
        j.assertBuildStatus(Result.SUCCESS, j.waitForCompletion(r));

        File sourceFile = new File(r.getRootDir(),
                DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + relativeSourcePath.replaceAll(
                        "[^a-zA-Z0-9-_.]", "_"));

        Assert.assertTrue(sourceFile.exists());
    }

    @Test
    public void coveragePipelineOnAgentNode() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePluginITestUtil.verifySimpleCoverageNode(project);
    }

    private WorkflowJob createPipelineOnAgent() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node('docker') {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c' ]],\n"
                + "userRemoteConfigs: [[url: '" + "https://github.com/jenkinsci/analysis-model.git" + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}", true));
        return job;
    }

    private Run<?, ?> createPipelineJobAndAssertBuildResult(String jobDefinition, Result expectedBuildResult,
            String... fileNames) {
        WorkflowJob job;
        if (fileNames.length > 0) {
            job = createPipelineWithWorkspaceFiles(fileNames);
        }
        else {
            job = createPipeline();
        }
        job.setDefinition(new CpsFlowDefinition(jobDefinition, true));
        Run<?, ?> build = buildWithResult(job, expectedBuildResult);
        assertThat(build.getNumber()).isEqualTo(1);
        return build;
    }

    private DumbSlave createDockerContainerAgent(final DockerContainer dockerContainer, IntegrationTestWithJenkinsPerSuite integrationTestWithJenkinsPerSuite) {
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
                    new Entry("JAVA_HOME", "/usr/lib/jvm/java-8-openjdk-amd64/jre"))));
            getJenkins().jenkins.addNode(agent);
            getJenkins().waitOnline(agent);

            return agent;
        }
        catch (Throwable e) {
            throw new AssumptionViolatedException("Failed to create docker container", e);
        }
    }



}
