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
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import io.jenkins.plugins.coverage.source.SourceFileResolver.SourceFileResolverLevel;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class CoveragePluginFreestyleITest extends IntegrationTestWithJenkinsPerSuite {

    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    /** Test with no adapters */
    @Test
    public void freestyleWithEmptyAdapters() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.emptyList());
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /** Test with JacocoAdapter and no files */
    @Test
    public void freestyleJacocoWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /** Test with one Jacoco file */
    @Test
    public void freestyleJacocoWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

//        assertLineCoverageResults(Arrays.asList(TOTAL_LINES_JACOCO_ANALYSIS_MODEL),
//                Arrays.asList(COVERED_LINES_JACOCO_ANALYSIS_MODEL), coverageResult);
    }

    /** Test with two Jacoco files */
    @Test
    public void freestyleJacocoWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /** Test with two Jacoco files and two adapters */
    @Test
    public void freestyleJacocoWithTwoFilesAndTwoAdapters() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapterOne = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapterOne, jacocoReportAdapterTwo));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /** Test with Cobertura Adapter and no files */
    @Test
    public void freestyleCoberturaWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isNull();
    }

    @Test
    public void freestyleCoberturaWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED),
                build);
    }

    @Test
    public void freestyleCoberturaWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL, CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED, CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED),
                build);
    }

    @Test
    public void freestyleWithJacocoAdapterAndCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.emptyList(),
                Collections.emptyList(),
                build);
    }

    @Test
    public void freestyleWithCoberturaAndJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(coberturaReportAdapter, jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getNumber()).isEqualTo(1);
        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED, CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED),
                build);
    }

    @Test
    public void freestyleZeroReportsFail() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void freestyleZeroReportsOkay() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(false);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
    }

    @Test
    public void freestyleQualityGatesSuccessful() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
    }

    @Test
    public void freestyleQualityGatesSuccessfulUnhealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(0);
    }

    @Test
    public void freestyleQualityGatesUnstable() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnstableThreshold(99f);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
    }

    @Test
    public void freestyleQualityGatesFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);
    }

    @Test
    public void freestyleHealthReport() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(100);
    }

    // TODO: build2 result is not failing as expected
    @Test
    public void freestyleFailWhenCoverageDecreases() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        coveragePublisherTwo.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> secondBuild = buildWithResult(project, Result.FAILURE);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    @Test
    public void freestyleSkipPublishingChecks() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setSkipPublishingChecks(true);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getLog(1000))
                .doesNotContain("[Checks API] No suitable checks publisher found.");
    }

    @Test
    public void freestylePublishingChecks() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setSkipPublishingChecks(false);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getLog(1000))
                .contains("[Checks API] No suitable checks publisher found.");
    }

    @Test
    public void freestyleSourceCodeRendering() {
        // TODO: How to test ? maybe see CoveragePublisherPipelineTest last two tests ?

        FreeStyleProject project = createFreeStyleProject();

        // build 1
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        DefaultSourceFileResolver sourceFileResolverNeverStore = new DefaultSourceFileResolver(
                SourceFileResolverLevel.NEVER_STORE);
        coveragePublisher.setSourceFileResolver(sourceFileResolverNeverStore);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> secondBuild = buildWithResult(project, Result.FAILURE);
    }

    @Test
    public void freestyleSourceCodeCopying() {
        // TODO: How to test ? Difference to rendering ? maybe see CoveragePublisherPipelineTest last two tests ?
    }

    @Test
    public void freestyleDeltaComputation() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> secondBuild = buildSuccessfully(project);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    @Test
    public void freestyleReferenceBuildPresent() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> secondBuild = buildSuccessfully(project);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isPresent();
        Run<?, ?> referenceBuild = coverageResult.getReferenceBuild().get();
        assertThat(referenceBuild).isEqualTo(firstBuild);
    }

    @Test
    public void freestyleReferenceBuildEmpty() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isEmpty();
    }

    @Test
    public void freestyleReportAggregation() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        int covered = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED;
        int total = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL;
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(covered, total - covered));
    }

    @Test
    public void coverageFreeStyleOnAgent() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);

        copySingleFileToAgentWorkspace(agent, project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        CoveragePluginITestUtil.verifySimpleCoverageNode(project);
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
