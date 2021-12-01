package io.jenkins.plugins.coverage.model;

import hudson.model.FreeStyleProject;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// IM Team pipeline job und freestyle job! AUFTEILEN!

// 1
//kein report, schlägt nicht fehl
//build schlägt fehl, wenn nix drin

// 2
// 0 Reports findet man im CoveragePublisherPipelineTest
// Die asserts gehen nur auf log, wir wollen mehr prüfen!!!

// 3
// Workflow job! mit job.setDefiniton


/*

 */

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
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
    }

    /**
     * Frage: Wie können wir den Build auf unhealtykeit überprüfenn?
     */
    @Test
    public void healthReportingUnhealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setFailUnhealthy(true);
        Threshold threshold = new Threshold("Line");
        //threshold.setFailUnhealthy(true);
        threshold.setUnhealthyThreshold(100);
        //coveragePublisher.setGlobalThresholds(Collections.singletonList(threshold));

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        jacocoReportAdapter.setThresholds(Collections.singletonList(threshold));

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        //Run<?, ?> build = buildSuccessfully(project);
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

    /**
     * Was sind checks, waurm werden sie geskippt und wo steht das?
     * Siehe Discord für link
     */
    @Test
    public void skipPublishingChecks() {
        assertThat(true).isEqualTo(true);
    }

    @Test
    public void agentInDocker() {

    }
}
