package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Run;

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

    private static final String FILE_NAME = "jacoco-analysis-model.xml";

    /** Example integration test for a freestyle build with code coverage. */
    @Test
    public void coveragePluginHelloWorld() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        // 4. Jacoco XML File in den Workspace legen (Stub für einen Build)
        // 5. Jenkins Build starten
        Run<?, ?> build = buildSuccessfully(project);
        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
    }
}
