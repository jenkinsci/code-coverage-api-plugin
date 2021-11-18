package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String FILE_NAME_JACOCO = "jacoco-analysis-model.xml";
    private static final String FILE_NAME_COBERTURA = "cobertura-coverage.xml";

    @Test
    public void  freestyleJacocoWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();
        // copyFilesToWorkspace(project, FILE_NAME_JACOCO);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
//        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("");
        coveragePublisher.setAdapters(Collections.emptyList());
        project.getPublishersList().add(coveragePublisher);

        verifySimpleCoverageNode(project, 0, 0);
    }

    @Test
    public void freestyleJacocoWithOneFile() {
        // TODO
    }

    @Test
    public void freestyleJacocoWithTwoFiles() {
        // TODO
    }

    @Test
    public void  freestyleCoberturaWithEmptyFiles() {
        // TODO
    }

    @Test
    public void freestyleCoberturaWithOneFile() {
        // TODO
    }

    @Test
    public void freestyleCoberturaWithTwoFiles() {
        // TODO
    }



    /** Example integration test for a pipeline with code coverage. */
    @Test
    public void coveragePluginPipelineHelloWorld() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(FILE_NAME_JACOCO);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifySimpleCoverageNode(job, 6083, 6368 - 6083);

    }

    /** Example integration test for a freestyle build with code coverage. */
    @Test
    public void coveragePluginFreestyleHelloWorld() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME_JACOCO);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME_JACOCO);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifySimpleCoverageNode(project, 6083, 6368 - 6083);
    }

    private void verifySimpleCoverageNode(final ParameterizedJob<?, ?> project, int assertCoveredLines, int assertMissedLines) {
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
