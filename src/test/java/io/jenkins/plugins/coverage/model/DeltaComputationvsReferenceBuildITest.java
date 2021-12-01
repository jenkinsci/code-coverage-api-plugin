package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import edu.hm.hafner.analysis.Report;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerTest;

import static io.jenkins.plugins.coverage.model.Assertions.*;

public class DeltaComputationvsReferenceBuildITest extends IntegrationTestWithJenkinsPerSuite {
    private static final List<String> MESSAGES = Arrays.asList("Message 1", "Message 2");

    private static final String COBERTURA_FILE_NAME = "cobertura-higher-coverage.xml";
    private static final String COBERTURA_FILE_NAME_2 = "cobertura-lower-coverage.xml";
@Rule
public JenkinsRule j = new JenkinsRule();



    @Test
    public void checkIfReferenceBuildForDeltaComputationIsAvailable() throws IOException, ClassNotFoundException {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);
        // 3b. Job konfigurieren// 3a. Job erzeugen

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
        //coverageResult.getReferenceBuild();

        // coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
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
