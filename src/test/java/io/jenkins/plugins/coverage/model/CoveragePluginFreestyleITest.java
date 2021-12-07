package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;
import hudson.tasks.Publisher;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage plugin using freestyle jobs.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class CoveragePluginFreestyleITest extends IntegrationTestWithJenkinsPerSuite {

    // TODO: @Michael bitte verifizieren
    /**
     * Docker rule describing a JavaGitContainer.
     */
    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    /**
     * Tests a freestyle job with no files present, using no adapters.
     */
    @Test
    public void freestyleWithEmptyAdapters() {
        FreeStyleProject project = createFreeStyleProject();
        // TODO: This is not necessary to duplicate in all tests, try to integrate into the method that creates the job
        Run<?, ?> build = createBuildWithJacocoAdapaters(project, Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);

    }

    /**
     * Tests a freestyle job with no files present, using a jacoco adapter.
     */
    @Test
    public void freestyleJacocoWithNoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        Run<?, ?> build = createBuildWithJacocoAdapaters(project, Result.SUCCESS, "*.xml");
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /**
     * Tests a freestyle job with one jacoco file present.
     */
    @Test
    public void freestyleJacocoWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        Run<?, ?> build = createBuildWithJacocoAdapaters(project, Result.SUCCESS, "*.xml");

        assertThat(build.getNumber()).isEqualTo(1);
        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED), build);
    }

    /**
     * Tests a freestyle job with two jacoco files present.
     */
    @Test
    public void freestyleJacocoWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        Run<?, ?> build = createBuildWithJacocoAdapaters(project, Result.SUCCESS, "*.xml");

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /**
     * Tests a freestyle job with two jacoco file present using two jacoco adapters.
     */
    @Test
    public void freestyleJacocoWithTwoFilesAndTwoAdapters() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        Run<?, ?> build = createBuildWithJacocoAdapaters(project, Result.SUCCESS,
                CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED), build);
    }

    /**
     * Tests a freestyle job with no files present, using a cobertura adapter.
     */
    @Test
    public void freestyleCoberturaWithNoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        Run<?, ?> build = createBuildWithCoberturaAdapaters(project, Result.SUCCESS,
                "*.xml");

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isNull();
    }

    /**
     * Tests a freestyle job with one cobertura file present.
     */
    @Test
    public void freestyleCoberturaWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);
        Run<?, ?> build = createBuildWithCoberturaAdapaters(project, Result.SUCCESS,
                "*.xml");

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.singletonList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL),
                Collections.singletonList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED),
                build);
    }

    /**
     * Tests a freestyle job with two cobertura files present.
     */
    @Test
    public void freestyleCoberturaWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        Run<?, ?> build = createBuildWithCoberturaAdapaters(project, Result.SUCCESS,
                "*.xml");

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL,
                        CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED,
                        CoveragePluginITestUtil.COBERTURA_COVERAGE_LINES_COVERED),
                build);
    }

    /**
     * Tests a freestyle job with a cobertura file present using a jacoco adapter resulting in coverages with value 0.
     */
    @Test
    public void freestyleWithJacocoAdapterAndCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Collections.emptyList(),
                Collections.emptyList(),
                build);
    }

    /**
     * Tests a freestyle job with a cobertura file as well as a jacoco file present.
     */
    @Test
    public void freestyleWithCoberturaAndJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(
                CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(coberturaReportAdapter, jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getNumber()).isEqualTo(1);
        CoveragePluginITestUtil.assertLineCoverageResultsOfBuild(
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(CoveragePluginITestUtil.COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED,
                        CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED),
                build);
    }

    /**
     * Tests a freestyle job failing while set up parameter failNoReports and containing no reports.
     */
    @Test
    public void freestyleZeroReportsFail() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Tests a freestyle job succeeding while parameter failNoReports is not set and containing no reports.
     */
    @Test
    public void freestyleZeroReportsOkay() {
        FreeStyleProject project = createFreeStyleProject();

        // TODO: DRY! Try to use one method with parameters.
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setFailNoReports(false);
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.SUCCESS);
    }

    /**
     * Tests a freestyle job succeeding while containing a quality gate.
     */
    @Test
    public void freestyleQualityGatesSuccessful() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.SUCCESS);
    }

    // TODO: Michi - Bitte dokumentieren
    @Test
    public void freestyleQualityGatesSuccessfulUnhealthy() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(0);
    }

    /**
     * Tests a freestyle job resulting unstable while containing a quality gate.
     */
    @Test
    public void freestyleQualityGatesUnstable() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnstableThreshold(99f);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
    }

    /**
     * Tests a freestyle job failing while containing a quality gate.
     */
    @Test
    public void freestyleQualityGatesFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Tests the health report of a freestyle job.
     */
    @Test
    public void freestyleHealthReport() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(100);
    }

    /**
     * Tests a freestyle job failing while parameter failBuildIfCoverageDecreasedInChangeRequest is set and coverage decreases.
     */
    // TODO: Bug in coverage-plugin ? second build is not failing as expected although negative delta is computed.
    @Test
    public void freestyleFailWhenCoverageDecreases() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisher2 = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisher2.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisher2);
        buildWithResult(project, Result.FAILURE);
    }

    /**
     * Tests a freestyle job with decreased logs while parameter skipPublishingChecks is set.
     * @throws IOException if build log cannot be read
     */
    @Test
    public void freestyleSkipPublishingChecks() throws IOException {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setSkipPublishingChecks(true);

        Run<?, ?> build = createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .doesNotContain("[Checks API] No suitable checks publisher found.");
    }

    /**
     * Tests a freestyle job with extended logs while parameter skipPublishingChecks is not set.
     * @throws IOException if build log cannot be read
     */
    @Test
    public void freestylePublishingChecks() throws IOException {
        CoveragePublisher coveragePublisher = createPublisherWithCoberturaAdapter("*.xml");
        coveragePublisher.setSkipPublishingChecks(false);

        Run<?, ?> build = createFreestyleProjectAndAssertBuildResult(coveragePublisher, Result.SUCCESS, CoveragePluginITestUtil.COBERTURA_COVERAGE_FILE_NAME);

        assertThat(build.getLog(1000))
                .contains("[Checks API] No suitable checks publisher found.");
    }

    /**
     * Tests the delta computation of two freestyle jobs.
     */
    @Test
    public void freestyleDeltaComputation() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisher2 = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
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
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisher2 = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
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
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_DECREASED_FILE_NAME);

        // build 1
        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter(CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isEmpty();
    }

    /**
     * Tests whether the coverage result of two files in a freestyle job are aggregated.
     */
    @Test
    public void freestyleReportAggregation() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_FILE_NAME,
                CoveragePluginITestUtil.JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePublisher coveragePublisher = createPublisherWithJacocoAdapter("*.xml");
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        int covered = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_COVERED
                + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_COVERED;
        int total = CoveragePluginITestUtil.JACOCO_ANALYSIS_MODEL_LINES_TOTAL
                + CoveragePluginITestUtil.JACOCO_CODING_STYLE_LINES_TOTAL;
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(covered, total - covered));
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
        catch (Throwable e) {
            throw new AssumptionViolatedException("Failed to create docker container", e);
        }
    }

    private Run<?, ?> createBuildWithJacocoAdapaters(final FreeStyleProject project, final Result expectedBuildResult,
            final String... jacocoFileNames) {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> jacocoReportAdapters = new ArrayList<>();
        for (String fileName : jacocoFileNames) {
            jacocoReportAdapters.add(new JacocoReportAdapter(fileName));
        }
        coveragePublisher.setAdapters(jacocoReportAdapters);
        project.getPublishersList().add(coveragePublisher);
        return buildWithResult(project, expectedBuildResult);
    }

    private Run<?, ?> createBuildWithCoberturaAdapaters(final FreeStyleProject project, final Result expectedBuildResult,
            final String... coberturaFileNames) {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coberturaAdapters = new ArrayList<>();
        for (String fileName : coberturaFileNames) {
            coberturaAdapters.add(new CoberturaReportAdapter(fileName));
        }
        coveragePublisher.setAdapters(coberturaAdapters);
        project.getPublishersList().add(coveragePublisher);
        return buildWithResult(project, expectedBuildResult);
    }


    private CoveragePublisher createPublisherWithCoberturaAdapter(final String fileName) {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(fileName);

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        return coveragePublisher;
    }

    private CoveragePublisher createPublisherWithJacocoAdapter(final String fileName) {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(fileName);

        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        return coveragePublisher;
    }

    private Run<?, ?> createFreestyleProjectAndAssertBuildResult(final Publisher publisher, final Result expectedBuildResult,
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
