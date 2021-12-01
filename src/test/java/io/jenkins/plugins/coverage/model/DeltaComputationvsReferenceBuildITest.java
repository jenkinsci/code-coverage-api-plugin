package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Integration test for delta computation of reference builds.
 */
public class DeltaComputationvsReferenceBuildITest extends IntegrationTestWithJenkinsPerSuite {
    private static final List<String> MESSAGES = Arrays.asList("Message 1", "Message 2");

    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "cobertura-lower-coverage.xml";

    /**
     * Checks if delta can be computed for reference build.
     *
     * @throws IOException
     *         when trying to recover coverage result
     * @throws ClassNotFoundException
     *         when trying to recover coverage result
     */
    @Test
    public void checkIfReferenceBuildForDeltaComputationIsAvailable() throws IOException, ClassNotFoundException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter jacocoReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        //run first build
        Run<?, ?> firstBuild = buildSuccessfully(project);

        //prepare second build
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME_2);

        CoberturaReportAdapter jacocoReportAdapter2 = new CoberturaReportAdapter(
                COBERTURA_FILE_NAME_2);

        List<CoverageAdapter> coverageAdapters = new ArrayList<>();
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter2));

        project.getPublishersList().add(coveragePublisher);

        ReferenceBuild referenceBuild = new ReferenceBuild(firstBuild, MESSAGES);
        referenceBuild.onLoad(firstBuild);

        //run second build
        Run<?, ?> secondBuild = buildWithResult(project, Result.SUCCESS);
        referenceBuild.onAttached(secondBuild);

        CoverageResult resultFirstBuild = CoverageProcessor.recoverCoverageResult(project.getBuild("1"));
        CoverageResult resultSecondBuild = CoverageProcessor.recoverCoverageResult(project.getBuild("2"));

        assertThat(resultSecondBuild.hasDelta(CoverageElement.LINE)).isTrue();
        assertThat(resultFirstBuild.hasDelta(CoverageElement.LINE)).isFalse();

    }
}
