package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import hudson.model.Descriptor.FormException;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage plugin using pipelines.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class CoveragePluginPipelineITest extends IntegrationTestWithJenkinsPerSuite {

    // TODO: @Michael bitte verifizieren
    /**
     * Docker rule describing a JavaGitContainer.
     */
    @Rule
    public final DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    /**
     * Tests a pipeline job with no files present, using a jacoco adapter.
     */
    @Test
    public void pipelineJacocoWithNoFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isNull();
    }

    /**
     * Tests a pipeline job with one jacoco file present.
     */
    @Test
    public void pipelineJacocoWithOneFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED),
                build);
    }

    /**
     * Tests a pipeline job with two jacoco files present.
     */
    @Test
    public void pipelineJacocoWithTwoFiles() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /**
     * Tests a pipeline job with no files present, using a cobertura adapter.
     */
    @Test
    public void pipelineCoberturaWithNoFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isNull();
    }

    /**
     * Tests a pipeline job with one cobertura file present.
     */
    @Test
    public void pipelineCoberturaWithOneFile() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED),
                build);
    }

    /**
     * Tests a pipeline job with two cobertura files present.
     */
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

    /**
     * Tests a pipeline job with a cobertura file as well as a jacoco file present.
     */
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

    /**
     * Tests a pipeline job failing while set up parameter failNoReports and containing no reports.
     */
    @Test
    public void pipelineZeroReportsFail() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('*.xml')], failNoReports: true, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.FAILURE);
    }

    /**
     * Tests a pipeline job succeeding while parameter failNoReports is not set and containing no reports.
     */
    @Test
    public void pipelineZeroReportsOkay() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter('*.xml')], failNoReports: false, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS);
    }

    /**
     * Tests a pipeline job succeeding while containing a quality gate.
     */
    @Test
    public void pipelineQualityGatesSuccess() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', thresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    /**
     * Tests a pipeline job failing while containing a quality gate.
     */
    @Test
    public void pipelineQualityGatesFail() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml', thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.FAILURE, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    // TODO: Michi - Bitte dokumentieren
    @Test
    public void pipelineQualityGatesSuccessUnhealthy() {
        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', thresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(0);
    }

    /**
     * Tests a pipeline job resulting unstable while containing a quality gate.
     */
    @Test
    public void pipelineQualityGatesUnstable() {
        createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml', thresholds: [[thresholdTarget: 'Line', unstableThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.UNSTABLE, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    /**
     * Tests a pipeline job failing while parameter failBuildIfCoverageDecreasedInChangeRequest is set and coverage decreases.
     */
    @Test
    public void pipelineFailWhenCoverageDecreases() {


        WorkflowJob job = createPipelineWithWorkspaceFiles(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" +  CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME + "')], failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));

        Run<?, ?> secondBuild = buildWithResult(job, Result.FAILURE);

        assertThat(build.getNumber()).isEqualTo(1);

        // TODO: @Hafner: Delta is correctly computed but build still is successful. To test uncomment the following lines and replace FAILURE check in second build with SUCCESS.
        CoverageBuildAction secondCoverageBuild = secondBuild.getAction(CoverageBuildAction.class);
        assertThat(secondCoverageBuild.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    /**
     * Tests a pipeline job with decreased logs while parameter skipPublishingChecks is set.
     * @throws IOException if build log cannot be read
     */
    @Test
    public void pipelineSkipPublishingChecks() throws IOException {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], skipPublishingChecks: true, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .doesNotContain("[Checks API] No suitable checks publisher found.");
    }

    /**
     * Tests a pipeline job with extending logs while parameter skipPublishingChecks is not set.
     * @throws IOException if build log cannot be read
     */
    @Test
    public void pipelinePublishingChecks() throws IOException {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], skipPublishingChecks: false, sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .contains("[Checks API] No suitable checks publisher found.");
    }

    /**
     * Tests the health report of a pipeline job.
     */
    @Test
    public void pipelineHealthReport() {

        Run<?, ?> build = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(100);
    }

    /**
     * Tests whether the coverage result of two files in a pipeline job are aggregated.
     */
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

    /**
     * Tests the delta computation of two pipeline jobs.
     */
    @Test
    public void pipelineDeltaComputation() {

        Run<?, ?> firstBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        Run<?, ?> secondBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   discoverReferenceBuild(referenceJob:'" + firstBuild.getParent().getName() + "')\n"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')\n"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        CoverageBuildAction secondCoverageBuild = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(secondCoverageBuild.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    /**
     * Tests whether a reference build is correctly set in a second pipeline build.
     */
    @Test
    public void pipelineReferenceBuildPresent() {

        Run<?, ?> firstBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        Run<?, ?> secondBuild = createPipelineJobAndAssertBuildResult(
                "node {"
                        + "   discoverReferenceBuild(referenceJob:'" + firstBuild.getParent().getName() + "')\n"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                        + "}", Result.SUCCESS, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        CoverageBuildAction secondCoverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(secondCoverageResult.getReferenceBuild()).isPresent();
        Run<?, ?> referenceBuild = secondCoverageResult.getReferenceBuild().get();
        assertThat(referenceBuild).isEqualTo(firstBuild);
    }

    /**
     * Tests whether a reference build is correctly not set in a single pipeline build.
     */
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
    public void pipelineOnAgentNode() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        WorkflowJob job = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, job, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        Run<?, ?> build = buildSuccessfully(job);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED),
                build);

        //TODO: Here assertions are missing that check for the source code.
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

    private Run<?, ?> createPipelineJobAndAssertBuildResult(final String jobDefinition, final Result expectedBuildResult,
            final String... fileNames) {
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

    private DumbSlave createDockerContainerAgent(final DockerContainer dockerContainer) {
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
        catch (Exception e) {
            throw new AssumptionViolatedException("Failed to create docker container", e);
        }
    }
}
