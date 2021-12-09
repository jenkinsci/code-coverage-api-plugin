package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.Publisher;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage plugin using freestyle projects.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class CoveragePluginFreestyleITest extends IntegrationTestWithJenkinsPerSuite {

    /**
     * Tests a freestyle project with no files present, using no adapters.
     */
    @Test
    public void freestyleWithEmptyAdapters() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult).isNull();
    }

    /**
     * Tests a freestyle project with no files present, using a jacoco adapter.
     */
    @Test
    public void freestyleJacocoWithNoFiles() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult).isNull();
    }

    /**
     * Tests a freestyle project with one jacoco file present.
     */
    @Test
    public void freestyleJacocoWithOneFile() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED), build);
    }

    /**
     * Tests a freestyle project with two jacoco files present.
     */
    @Test
    public void freestyleJacocoWithTwoFiles() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /**
     * Tests a freestyle project with two jacoco file present using two jacoco adapters.
     */
    @Test
    public void freestyleJacocoWithTwoFilesAndTwoAdapters() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /**
     * Tests a freestyle project with no files present, using a cobertura adapter.
     */
    @Test
    public void freestyleCoberturaWithNoFiles() {
        Run<?, ?> build = createFreestyleProjectWithCoberturaAdapatersAndAssertBuildResult(Result.SUCCESS);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isNull();
    }

    /**
     * Tests a freestyle project with one cobertura file present.
     */
    @Test
    public void freestyleCoberturaWithOneFile() {
        Run<?, ?> build = createFreestyleProjectWithCoberturaAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED),
                build);
    }

    /**
     * Tests a freestyle project with two cobertura files present.
     */
    @Test
    public void freestyleCoberturaWithTwoFiles() {
        Run<?, ?> build = createFreestyleProjectWithCoberturaAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL,
                        CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED,
                        CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED),
                build);
    }

    /**
     * Tests a freestyle project with a cobertura file present using a jacoco adapter resulting in coverages with value
     * 0.
     */
    @Test
    public void freestyleWithJacocoAdapterAndCoberturaFile() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.emptyList(),
                Collections.emptyList(),
                build);
    }

    /**
     * Tests a freestyle project with a cobertura file as well as a jacoco file present.
     */
    @Test
    public void freestyleWithCoberturaAndJacocoFile() {
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher publisher = createPublisherWithAdapter(jacocoReportAdapter, coberturaReportAdapter);

        Run<?, ?> build = createFreestyleProjectAndAssertBuildResult(publisher, Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED),
                build);
    }

    /**
     * Tests a freestyle project failing while set up parameter failNoReports and containing no reports.
     */
    @Test
    public void freestyleZeroReportsFail() {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setFailNoReports(true);
        createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.FAILURE);
    }

    /**
     * Tests a freestyle project succeeding while parameter failNoReports is not set and containing no reports.
     */
    @Test
    public void freestyleZeroReportsOkay() {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setFailNoReports(false);
        createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS);
    }

    /**
     * Tests a freestyle project succeeding while containing a passed quality gate.
     */
    @Test
    public void freestyleQualityGatesSuccessful() {
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95f);
        lineThreshold.setFailUnhealthy(true);

        createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    /**
     * Tests a freestyle project resulting with unhealthy health report while containing a failed quality gate.
     */
    @Test
    public void freestyleQualityGatesSuccessfulUnhealthy() {
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));

        Run<?, ?> build = createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(0);
    }

    /**
     * Tests a freestyle project resulting unstable while containing a failed quality gate.
     */
    @Test
    public void freestyleQualityGatesUnstable() {
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnstableThreshold(99f);
        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));

        createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.UNSTABLE,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    /**
     * Tests a freestyle project failing while containing a failed quality gate.
     */
    @Test
    public void freestyleQualityGatesFail() {
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        lineThreshold.setFailUnhealthy(true);
        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));

        createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.FAILURE,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);
    }

    /**
     * Tests the health report of a freestyle project.
     */
    @Test
    public void freestyleHealthReport() {
        Run<?, ?> build = createFreestyleProjectWithCoberturaAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(100);
    }

    /**
     * Tests a freestyle project failing while parameter failBuildIfCoverageDecreasedInChangeRequest is set and coverage
     * decreases.
     */
    // TODO: Bug in coverage-plugin ?
    @Test
    public void freestyleFailWhenCoverageDecreases() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisher2 = createPublisherWithJacocoAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisher2.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisher2);
        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Tests a freestyle project with decreased logs while parameter skipPublishingChecks is set.
     *
     * @throws IOException
     *         if build log cannot be read
     */
    @Test
    public void freestyleSkipPublishingChecks() throws IOException {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setSkipPublishingChecks(true);

        Run<?, ?> build = createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .doesNotContain("[Checks API] No suitable checks publisher found.");
    }

    /**
     * Tests a freestyle project with extended logs while parameter skipPublishingChecks is not set.
     *
     * @throws IOException
     *         if build log cannot be read
     */
    @Test
    public void freestylePublishingChecks() throws IOException {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setSkipPublishingChecks(false);

        Run<?, ?> build = createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .contains("[Checks API] No suitable checks publisher found.");
    }

    /**
     * Tests the delta computation of two freestyle projects.
     */
    @Test
    public void freestyleDeltaComputation() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisher2 = createPublisherWithJacocoAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        project.getPublishersList().add(coveragePublisher2);
        Run<?, ?> secondBuild = buildSuccessfully(project);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    /**
     * Tests whether a reference build is correctly set in a second freestyle build.
     */
    @Test
    public void freestyleReferenceBuildPresent() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisher2 = createPublisherWithJacocoAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        project.getPublishersList().add(coveragePublisher2);
        Run<?, ?> secondBuild = buildSuccessfully(project);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isPresent();

        Run<?, ?> referenceBuild = coverageResult.getReferenceBuild().get();
        assertThat(referenceBuild).isEqualTo(firstBuild);
    }

    /**
     * Tests whether a reference build is correctly not set in a single freestyle build.
     */
    @Test
    public void freestyleReferenceBuildEmpty() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isEmpty();
    }

    /**
     * Tests whether the coverage result of two files in a freestyle project are aggregated.
     */
    @Test
    public void freestyleReportAggregation() {
        Run<?, ?> build = createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        int covered = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED
                + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED;
        int total = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL
                + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL;
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(covered, total - covered));
    }

    private Run<?, ?> createFreestyleProjectWithJacocoAdapatersAndAssertBuildResult(final Result expectedBuildResult,
            final String... jacocoFileNames) {
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");
        return createFreestyleProjectAndAssertBuildResult(coveragePublisher, expectedBuildResult, jacocoFileNames);
    }

    private Run<?, ?> createFreestyleProjectWithCoberturaAdapatersAndAssertBuildResult(final Result expectedBuildResult,
            final String... coberturaFileNames) {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        return createFreestyleProjectAndAssertBuildResult(coveragePublisher, expectedBuildResult, coberturaFileNames);
    }

    private CoveragePublisher createPublisherWithAdapter(final CoverageAdapter... adapter) {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Arrays.asList(adapter));
        return coveragePublisher;
    }

    private CoveragePublisher createPublisherWithCoberturaAdapter(final String fileName) {
        return createPublisherWithAdapter(new CoberturaReportAdapter(fileName));
    }

    private CoveragePublisher createPublisherWithJacocoAdapter(final String fileName) {
        return createPublisherWithAdapter(new JacocoReportAdapter(fileName));
    }

    private Run<?, ?> createFreestyleProjectAndAssertBuildResult(final Publisher publisher,
            final Result expectedBuildResult,
            final String... fileNames) {
        FreeStyleProject project;
        if (fileNames.length > 0) {
            project = createFreeStyleProjectWithWorkspaceFiles(fileNames);
        }
        else {
            project = createFreeStyleProject();
        }
        project.getPublishersList().add(publisher);
        Run<?, ?> build = buildWithResult(project, expectedBuildResult);
        assertThat(build.getNumber()).isEqualTo(1);
        return build;
    }
}
