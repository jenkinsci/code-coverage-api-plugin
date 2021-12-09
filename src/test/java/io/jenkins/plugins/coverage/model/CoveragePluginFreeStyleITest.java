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
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * FreeStyle integration tests for the CoveragePlugin.
 * @author Johannes Walter, Katharina Winkler
 */
public class CoveragePluginFreeStyleITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_BIG_DATA = "jacoco-analysis-model.xml";
    private static final String JACOCO_SMALL_DATA = "jacoco.xml";
    private static final String JACOCO_MINI_DATA = "jacocoModifiedMini.xml";
    private static final String COBERTURA_BIG_DATA = "coverage-with-lots-of-data.xml";
    private static final TestUtil TEST_UTIL = new TestUtil();

    /**
     * Tests the freestyle job with Jacoco Adapter and no input files.
     */
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

    /**
     * Tests the freestyle job with Jacoco Adapter and one input files.
     */
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

    /**
     * Tests the freestyle job with Jacoco Adapter and two input files.
     */
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
                .isEqualTo(new Coverage(12_166, 12_736 - 12_166));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3322, 3750 - 3322));
    }

    /**
     * Tests the freestyle job with Cobertura Adapter and no input files.
     */
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

    /**
     * Tests the freestyle job with Cobertura Adapter and one input files.
     */
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

    /**
     * Tests the freestyle job with Cobertura Adapter and two input files.
     */
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

    /**
     * Tests the freestyle job with Jacoco and Cobertura Adapter and input files for each.
     */
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

    /**
     * Tests the health report whether is healthy and successful.
     */
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

    /**
     * Tests the health report whether the build fails and is unhealthy.
     */
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

    /**
     * Tests whether the build fails if coverage is decreasing.
     */
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

    /**
     * Tests whether the build doesn't fail if coverage is decreasing.
     */
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
     * Tests whether the publishing of checks is skipped.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())
                .contains("Skipping checks")).isEqualTo(true);
    }

    /**
     * Tests whether the publishing of checks not is skipped.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream()).contains("Skipping checks")).isEqualTo(false);
    }

    /**
     * Tests whether the publishing of checks is skipped by default.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream()).contains("Skipping checks")).isEqualTo(false);
    }

    /**
     * Tests the delta computing of two builds each with different input files.
     */
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

    /**
     * Tests the delta computing of two builds each one the same input file.
     */
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

    /**
     * Tests the delta computing of one build.
     */
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

    /**
     * Tests whether the delta computing uses only the current and previous build.
     */
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

    /**
     * Tests the reference build when there's only one single build.
     */
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

    /**
     * Tests whether the reference build is the previous build.
     */
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

    /**
     * Tests if reports are aggregated.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("A total of 1 reports were found");
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(11_399, 11_947 - 11_399));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3306, 3620 - 3306));
    }

    /**
     * Tests if reports are not aggregated.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("A total of 2 reports were found");
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(11_399, 11_947 - 11_399));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(3306, 3620 - 3306));
    }


    /**
     * Tests the reports whether build successful if no reports are found.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coveragePublisher.isFailNoReports()).isFalse();
    }

    /**
     * Tests if the setFailNoReports is set true, the build will fail.
     * @throws IOException from testUtil.getLogFromInputStream
     */
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
        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
        assertThat(coveragePublisher.isFailNoReports()).isTrue();
    }

    /**
     * Tests the global quality gates whether the build is successful if all gates are met.
     */
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

    /**
     * Tests the global quality gates whether the build is unstable.
     */
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

    /**
     * Tests the global quality gates whether the build is successful but unhealthy.
     */
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

    /**
     * Tests the global quality gates whether the build fails if unhealthy.
     * @throws IOException from testUtil.getLogFromInputStream {@link InputStream}
     */
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

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    /**
     * Tests the adapter quality gates whether the build is successful if all gates are met.
     */
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

    /**
     * Tests the adapter quality gates whether the build is unstable.
     */
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

    /**
     * Tests the adapter quality gates whether the build is successful but unhealthy.
     */
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

    /**
     * Tests the adapter quality gates whether the build fails when unhealthy.
     */
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

        assertThat(TEST_UTIL.getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    /**
     * Tests the source code rendering.
     */
    @Test
    public void sourceCodeRenderingAndCopying() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_BIG_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_BIG_DATA);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);

        CoverageViewModel model = action.getTarget();

        CoverageViewModel.CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "Package", "File", "Class", "Method", "Line", "Instruction", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                21, 306, 344, 1801, 6083, 26_283, 1661
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 1, 5, 48, 285, 1036, 214
        );
    }
}
