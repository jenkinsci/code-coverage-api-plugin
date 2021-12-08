package io.jenkins.plugins.coverage.model;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * FreeStyle integration tests for the CoveragePlugin
 */
// TODO: Dateien wieder verschieben und im Adapter anderen Pfad
public class CoveragePluginFreeStyleITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_BIG_DATA = "jacoco-analysis-model.xml";
    private static final String JACOCO_SMALL_DATA = "jacoco.xml";
    private static final String JACOCO_MINI_DATA = "jacocoModifiedMini.xml";
    private static final String COBERTURA_SMALL_DATA = "cobertura-coverage.xml";
    private static final String COBERTURA_BIG_DATA = "coverage-with-lots-of-data.xml";

    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    @Test
    public void noJacocoInputFile() {
        FreeStyleProject project = createFreeStyleProject();
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    @Test
    public void oneJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));

    }

    @Test
    public void twoJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coverageAdapterList = new ArrayList<>();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coverageAdapterList.add(jacocoReportAdapter);
        JacocoReportAdapter jacocoReportAdapter2 = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coverageAdapterList.add(jacocoReportAdapter2);
        coveragePublisher.setAdapters(coverageAdapterList);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(12166, 12736 - 12166));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3322, 3750 - 3322));
    }

    @Test
    public void noCoberturaInputFile() {
        FreeStyleProject project = createFreeStyleProject();
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    @Test
    public void oneCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(602, 958 - 602));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(285, 628 - 285));
    }

    @Test
    public void twoCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coverageAdapterList = new ArrayList<>();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_BIG_DATA);
        coverageAdapterList.add(coberturaReportAdapter);
        CoberturaReportAdapter coberturaReportAdapter2 = new CoberturaReportAdapter(COBERTURA_BIG_DATA);
        coverageAdapterList.add(coberturaReportAdapter2);
        coveragePublisher.setAdapters(coverageAdapterList);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(1204, 1916 - 1204));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(570, 1256 - 570));
    }

    @Test
    public void oneJacocoOneCobertura() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);
        copyFilesToWorkspace(project, COBERTURA_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coverageAdapterList = new ArrayList<>();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_BIG_DATA);
        coverageAdapterList.add(coberturaReportAdapter);
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coverageAdapterList.add(jacocoReportAdapter);
        coveragePublisher.setAdapters(coverageAdapterList);

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6685, 7326 - 6685));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1946, 2503 - 1946));
    }

    @Test
    public void healthReportingHealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailUnhealthy(true);
        Threshold threshold = new Threshold("Line");
        threshold.setUnhealthyThreshold(20);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        jacocoReportAdapter.setThresholds(Collections.singletonList(threshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
    }

    @Test
    public void healthReportingUnhealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailUnhealthy(true);
        Threshold threshold = new Threshold("Line");
        threshold.setUnhealthyThreshold(100);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        jacocoReportAdapter.setThresholds(Collections.singletonList(threshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
        assertThat(x.getBuildHealth()).isEqualTo(null);
    }

    @Test
    public void failIfCoverageDecreasesTrue() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        buildSuccessfully(project);
        copyFilesToWorkspace(project, JACOCO_MINI_DATA);
        project.getPublishersList().clear();
        JacocoReportAdapter jacocoMiniReportAdapter = new JacocoReportAdapter(JACOCO_MINI_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoMiniReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void failIfCoverageDecreasesFalse() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(false);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        buildSuccessfully(project);
        copyFilesToWorkspace(project, JACOCO_MINI_DATA);
        project.getPublishersList().clear();
        JacocoReportAdapter jacocoMiniReportAdapter = new JacocoReportAdapter(JACOCO_MINI_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoMiniReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void skipPublishingChecksTrue() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setSkipPublishingChecks(true);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
        System.out.println(build.getLogText().readAll().toString());

        Scanner s = new Scanner(build.getLogInputStream()).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        assertThat(result.contains("Skipping checks")).isEqualTo(true);
    }

    @Test
    public void skipPublishingChecksFalse() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setSkipPublishingChecks(false);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));

        assertThat(getLogFromInputStream(build.getLogInputStream()).contains("Skipping checks")).isEqualTo(false);
    }


    @Test
    public void skipPublishingChecksStandard() throws IOException {
        assertThat(true).isEqualTo(true);
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));

        assertThat(getLogFromInputStream(build.getLogInputStream()).contains("Skipping checks")).isEqualTo(false);
    }

    private String getLogFromInputStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Test
    public void deltaComputation() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);
        copyFilesToWorkspace(project, JACOCO_MINI_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter adapterFirstBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterFirstBuild));
        project.getPublishersList().add(coveragePublisher);

        buildSuccessfully(project);

        project.getPublishersList().clear();
        JacocoReportAdapter adapterSecondBuild = new JacocoReportAdapter(JACOCO_MINI_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterSecondBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageBuildAction = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(2);
        assertThat(coverageBuildAction.getDelta(CoverageMetric.LINE)).isEqualTo("-0.002");
    }

    @Test
    public void deltaComputationZeroDelta() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter adapterFirstBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterFirstBuild));
        project.getPublishersList().add(coveragePublisher);

        buildSuccessfully(project);

        project.getPublishersList().clear();
        JacocoReportAdapter adapterSecondBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterSecondBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageBuildAction = build.getAction(CoverageBuildAction.class);


        assertThat(build.getNumber()).isEqualTo(2);
        assertThat(coverageBuildAction.getDelta(CoverageMetric.LINE)).isEqualTo("+0.000");
    }

    @Test
    public void deltaComputationSingleBuild() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter adapterFirstBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterFirstBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageBuildAction = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageBuildAction.getDelta(CoverageMetric.LINE)).isEqualTo("n/a");
    }

    @Test
    public void deltaComputationUseOnlyPreviousAndCurrent() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);
        copyFilesToWorkspace(project, JACOCO_MINI_DATA);
        copyFilesToWorkspace(project, JACOCO_SMALL_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter adapterFirstBuild = new JacocoReportAdapter(JACOCO_SMALL_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterFirstBuild));
        project.getPublishersList().add(coveragePublisher);

        buildSuccessfully(project);

        project.getPublishersList().clear();

        JacocoReportAdapter adapterSecondBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterSecondBuild));
        project.getPublishersList().add(coveragePublisher);

        buildSuccessfully(project);

        project.getPublishersList().clear();
        JacocoReportAdapter adapterThirdBuild = new JacocoReportAdapter(JACOCO_MINI_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterThirdBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageBuildAction = build.getAction(CoverageBuildAction.class);


        assertThat(build.getNumber()).isEqualTo(3);
        assertThat(coverageBuildAction.getDelta(CoverageMetric.LINE)).isEqualTo("-0.002");
    }

    @Test
    public void referenceBuildSingleBuild() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter adapterFirstBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterFirstBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageBuildAction = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageBuildAction.getReferenceBuild()).isEmpty();
    }

    @Test
    public void referenceBuildReferenceIsPrevious() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter adapterFirstBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterFirstBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> referenceBuild = buildSuccessfully(project);

        project.getPublishersList().clear();
        JacocoReportAdapter adapterSecondBuild = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(adapterSecondBuild));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageBuildAction = build.getAction(CoverageBuildAction.class);


        assertThat(build.getNumber()).isEqualTo(2);
        assertThat(coverageBuildAction.getReferenceBuild()).isPresent();
        assertThat(coverageBuildAction.getReferenceBuild().get()).isEqualTo(referenceBuild);
     }

    @Test
    public void reportAggregation() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);
        copyFilesToWorkspace(project, JACOCO_MINI_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        jacocoReportAdapter.setMergeToOneReport(true);

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("A total of 1 reports were found");
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(11399, 11947 - 11399));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3306, 3620 - 3306));
    }

    @Test
    public void reportAggregationFalse() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);
        copyFilesToWorkspace(project, JACOCO_MINI_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        jacocoReportAdapter.setMergeToOneReport(false);

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("A total of 2 reports were found");
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(11399, 11947 - 11399));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3306, 3620 - 3306));
    }

    @Test
    public void agentInDocker() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);

        copySingleFileToAgentWorkspace(agent, project, JACOCO_BIG_DATA, JACOCO_BIG_DATA);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    @Test
    public void failNoReportsFalse() throws IOException {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailNoReports(false);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coveragePublisher.isFailNoReports()).isFalse();
    }

    @Test
    public void failNoReportsTrue() throws IOException {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailNoReports(true);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
        assertThat(coveragePublisher.isFailNoReports()).isTrue();
    }

    @Test
    public void qualityGatesGlobalThresholdSuccess() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        Threshold globalThreshold = new Threshold("Line");
        globalThreshold.setUnstableThreshold(20);
        globalThreshold.setUnhealthyThreshold(40);
        globalThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(globalThreshold));

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
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
    public void qualityGatesGlobalThresholdUnstable() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        Threshold globalThreshold = new Threshold("Line");
        globalThreshold.setUnstableThreshold(99);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(globalThreshold));

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
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
    public void qualityGatesGlobalThresholdSuccessUnhealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        Threshold globalThreshold = new Threshold("Line");
        globalThreshold.setUnstableThreshold(20);
        globalThreshold.setUnhealthyThreshold(100);
        globalThreshold.setFailUnhealthy(false);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(globalThreshold));

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
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
    public void qualityGatesGlobalThresholdFailUnhealthy() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        Threshold globalThreshold = new Threshold("Line");
        globalThreshold.setUnstableThreshold(20);
        globalThreshold.setUnhealthyThreshold(100);
        globalThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(globalThreshold));

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void qualityGatesAdapterThresholdSuccess() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        Threshold adapterThreshold = new Threshold("Line");
        adapterThreshold.setUnstableThreshold(20);
        adapterThreshold.setUnhealthyThreshold(40);
        adapterThreshold.setFailUnhealthy(true);
        jacocoReportAdapter.setThresholds(Collections.singletonList(adapterThreshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
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
    public void qualityGatesAdapterThresholdUnstable() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        Threshold adapterThreshold = new Threshold("Line");
        adapterThreshold.setUnstableThreshold(99);
        adapterThreshold.setUnhealthyThreshold(40);
        adapterThreshold.setFailUnhealthy(true);
        jacocoReportAdapter.setThresholds(Collections.singletonList(adapterThreshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
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
    public void qualityGatesAdapterThresholdSuccessUnhealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        Threshold adapterThreshold = new Threshold("Line");
        adapterThreshold.setUnstableThreshold(20);
        adapterThreshold.setUnhealthyThreshold(99);
        adapterThreshold.setFailUnhealthy(false);
        jacocoReportAdapter.setThresholds(Collections.singletonList(adapterThreshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
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
    public void qualityGatesAdapterThresholdFailUnhealthy() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        Threshold adapterThreshold = new Threshold("Line");
        adapterThreshold.setUnstableThreshold(20);
        adapterThreshold.setUnhealthyThreshold(99);
        adapterThreshold.setFailUnhealthy(true);
        jacocoReportAdapter.setThresholds(Collections.singletonList(adapterThreshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
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
}
