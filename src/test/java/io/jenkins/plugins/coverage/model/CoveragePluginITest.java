package io.jenkins.plugins.coverage.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    // TODO: other possibility than duplicating files because of different ressource folder ?
    private static final String FILE_NAME_JACOCO_ANALYSIS_MODEL = "jacoco-analysis-model.xml";
    private static final String FILE_NAME_JACOCO_CODING_STYLE = "jacoco-codingstyle.xml";
    private static final String FILE_NAME_JACOCO_CODING_STYLE_DECREASED = "jacoco-codingstyle-2.xml";
    private static final String FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA = "coverage-with-lots-of-data.xml";
    private static final String FILE_NAME_COBERTURA_COVERAGE = "cobertura-coverage.xml";

    private static final int TOTAL_LINES_JACOCO_ANALYSIS_MODEL = 6368;
    private static final int COVERED_LINES_JACOCO_ANALYSIS_MODEL = 6083;
    private static final int TOTAL_LINES_JACOCO_CODING_STYLE = 323;
    private static final int COVERED_LINES_JACOCO_CODING_STYLE = 294;
    private static final int COVERED_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA = 602;
    private static final int TOTAL_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA = 958;

    private static final int COVERED_LINES_COBERTURA_COVERAGE = 2;
    private static final int TOTAL_LINES_COBERTURA_COVERAGE = 2;

    /** Example integration test for a freestyle build with code coverage. */
    @Test
    public void coveragePluginFreestyleHelloWorld() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_ANALYSIS_MODEL);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME_JACOCO_ANALYSIS_MODEL);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifySimpleCoverageNode(project,
                COVERED_LINES_JACOCO_ANALYSIS_MODEL,
                TOTAL_LINES_JACOCO_ANALYSIS_MODEL - COVERED_LINES_JACOCO_ANALYSIS_MODEL);
    }

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
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_ANALYSIS_MODEL);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME_JACOCO_ANALYSIS_MODEL);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

//        assertLineCoverageResults(Arrays.asList(TOTAL_LINES_JACOCO_ANALYSIS_MODEL),
//                Arrays.asList(COVERED_LINES_JACOCO_ANALYSIS_MODEL), coverageResult);
    }

    /** Test with two Jacoco files */
    @Test
    public void freestyleJacocoWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_ANALYSIS_MODEL, FILE_NAME_JACOCO_CODING_STYLE);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(Arrays.asList(TOTAL_LINES_JACOCO_ANALYSIS_MODEL, TOTAL_LINES_JACOCO_CODING_STYLE),
                Arrays.asList(COVERED_LINES_JACOCO_ANALYSIS_MODEL, COVERED_LINES_JACOCO_CODING_STYLE), coverageResult);
    }

    /** Test with two Jacoco files and two adapters */
    @Test
    public void freestyleJacocoWithTwoFilesAndTwoAdapters() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_ANALYSIS_MODEL, FILE_NAME_JACOCO_CODING_STYLE);

        JacocoReportAdapter jacocoReportAdapterOne = new JacocoReportAdapter(FILE_NAME_JACOCO_ANALYSIS_MODEL);
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(FILE_NAME_JACOCO_CODING_STYLE);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapterOne, jacocoReportAdapterTwo));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(Arrays.asList(TOTAL_LINES_JACOCO_ANALYSIS_MODEL, TOTAL_LINES_JACOCO_CODING_STYLE),
                Arrays.asList(COVERED_LINES_JACOCO_ANALYSIS_MODEL, COVERED_LINES_JACOCO_CODING_STYLE), coverageResult);
    }

    private void assertLineCoverageResults(List<Integer> totalLines, List<Integer> coveredLines,
            CoverageBuildAction coverageResult) {
        int totalCoveredLines = coveredLines.stream().mapToInt(x -> x).sum();
        int totalMissedLines =
                totalLines.stream().mapToInt(x -> x).sum() - coveredLines.stream().mapToInt(x -> x).sum();
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(
                        totalCoveredLines,
                        totalMissedLines
                ));
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
        assertThat(coverageResult).isEqualTo(null);
    }

    @Test
    public void freestyleCoberturaWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(TOTAL_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA),
                Arrays.asList(COVERED_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA),
                coverageResult);
    }

    @Test
    public void freestyleCoberturaWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA, FILE_NAME_COBERTURA_COVERAGE);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(TOTAL_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA, TOTAL_LINES_COBERTURA_COVERAGE),
                Arrays.asList(COVERED_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA, COVERED_LINES_COBERTURA_COVERAGE),
                coverageResult);
    }

    @Test
    public void freestyleWithJacocoAdapterAndCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Collections.emptyList(),
                Collections.emptyList(),
                coverageResult);
    }

    @Test
    public void freestyleWithCoberturaAndJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_CODING_STYLE, FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME_JACOCO_CODING_STYLE);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(
                FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        coveragePublisher.setAdapters(Arrays.asList(coberturaReportAdapter, jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(TOTAL_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA, TOTAL_LINES_JACOCO_CODING_STYLE),
                Arrays.asList(COVERED_LINES_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA, COVERED_LINES_JACOCO_CODING_STYLE),
                coverageResult);
    }

    @Test
    public void zeroReportsFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

//        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        // TODO: Niko: complete tests
//        assertThat(coverageResult)
//        assertThatThrownBy(() -> coverageResult.getHealthReport());
    }

    @Test
    public void zeroReportsOkay() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_COBERTURA_COVERAGE_WITH_LOTS_OF_DATA);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

//        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        // TODO: Niko: complete tests
//        assertThat(coverageResult)
//        assertThatThrownBy(() -> coverageResult.getHealthReport());
    }

    @Test
    public void freestyleQualityGatesSuccessful() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_ANALYSIS_MODEL);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void freestyleQualityGatesFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_ANALYSIS_MODEL);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void healthReports() {
        // TODO: Niko
    }

    // TODO: Michi: Build is successful. Wrong checks ?
    @Test
    public void failWhenCoverageDecreases() {
        // build 1
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO_CODING_STYLE, FILE_NAME_JACOCO_CODING_STYLE_DECREASED);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME_JACOCO_CODING_STYLE);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(FILE_NAME_JACOCO_CODING_STYLE_DECREASED);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        coveragePublisherTwo.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> build_two = buildWithResult(project, Result.FAILURE);
        assertThat(build_two.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void skipChecksWhenPublishing() {
        // TODO: Niko
    }

    // TODO: @All: Check Google DOC for more assigned tests !

    /** Example integration test for a pipeline with code coverage. */
    @Test
    public void coveragePluginPipelineHelloWorld() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(FILE_NAME_JACOCO_ANALYSIS_MODEL);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifySimpleCoverageNode(job, 6083, 6368 - 6083);

    }

    private void verifySimpleCoverageNode(final ParameterizedJob<?, ?> project, int assertCoveredLines,
            int assertMissedLines) {
        // 4. Jacoco XML File in den Workspace legen (Stub für einen Build)
        // 5. Jenkins Build starten
        Run<?, ?> build = buildSuccessfully(project);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(assertCoveredLines, assertMissedLines));
    }

}
