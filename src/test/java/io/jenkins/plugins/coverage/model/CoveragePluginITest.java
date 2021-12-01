package io.jenkins.plugins.coverage.model;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoverageScriptedPipelineScriptBuilder;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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


public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    //private static final String COBERTURA_FILE_NAME = "../cobertura-coverage.xml";
    private static final String COBERTURA_FILE_NAME = "coverage-with-lots-of-data.xml";

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test(expected = NullPointerException.class)
    public void coveragePluginNoInputFile() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
    }

    @Test
    public void coveragePluginOneJacoco() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);

        // 3b. Job konfigurieren
        CoveragePublisher coveragePublisher = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        // 4. Jacoco XML File in den Workspace legen (Stub für einen Build)
        // 5. Jenkins Build starten
        Run<?, ?> build = buildSuccessfully(project);
        // 6. Mit Assertions Ergebnisse prüfend
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));

    }

    @Test
    public void coveragePluginOneCobertura() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();

        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
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
    public void coveragePluginOneCoberturaOneJacoco() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coverageAdapterList = new ArrayList<>();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        coverageAdapterList.add(coberturaReportAdapter);
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coverageAdapterList.add(jacocoReportAdapter);
        coveragePublisher.setAdapters(coverageAdapterList);

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6685, 7326 - 6685));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1946, 2503 - 1946));
    }

    @Test
    public void coveragePluginTwoJacoco() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coverageAdapterList = new ArrayList<>();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);
        coverageAdapterList.add(jacocoReportAdapter);
        JacocoReportAdapter jacocoReportAdapter2 = new JacocoReportAdapter(JACOCO_FILE_NAME);
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
    public void coveragePluginTwoCobertura() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        List<CoverageAdapter> coverageAdapterList = new ArrayList<>();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
        coverageAdapterList.add(coberturaReportAdapter);
        CoberturaReportAdapter coberturaReportAdapter2 = new CoberturaReportAdapter(COBERTURA_FILE_NAME);
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
    public void coveragePluginReportFails() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();
               // .addAdapter(new CoberturaReportAdapter("cobertura-coverage.xml"))
              //  .addAdapter(new JacocoReportAdapter("jacoco.xml"));


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        /*bjects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));
        workspace.child("jacoco.xml").copyFrom(getClass()
                .getResourceAsStream("jacoco.xml"));
*/
        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);

    }



    @Test
    public void coveragePluginPipelineNoJacocoFile() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();
        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        CoverageBuildAction coverageResult = r.getAction(CoverageBuildAction.class);


        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);

    }

    @Test
    public void coveragePluginPipelineTest() throws Exception {
        //CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
               /* .addAdapter(new CoberturaReportAdapter(COBERTURA_FILE_NAME) {{
                    setThresholds(Collections.singletonList(new Threshold() {{
                        new Threshold("cobertura") {{
                            setUnhealthyThreshold(90);
                            setUnstableThreshold(93);
                            setFailUnhealthy(true);
                    }}))
                }}))
                .setFailUnhealthy(true)
                .setFailNoReports(false)
                .setApplyThresholdRecursively(true)
                .addGlobalThreshold(new Threshold("cobertura") {{
                    setUnhealthyThreshold(90);
                    setUnstableThreshold(93);
                    setFailUnhealthy(true);
                }})));
                */
        //  .addAdapter(new JacocoReportAdapter("jacoco.xml"));

        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();
        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        project.setDefinition(new CpsFlowDefinition(""));
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child(COBERTURA_FILE_NAME)
                .copyFrom(getClass().getResourceAsStream(COBERTURA_FILE_NAME));

        //project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        CoverageBuildAction coverageResult = r.getAction(CoverageBuildAction.class);
        HealthReportingAction x = r.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(23);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(1204, 1916 - 1204));
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);

    }

    @Test
    public void testAdapters() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new CoberturaReportAdapter("cobertura-coverage.xml"))
                .addAdapter(new JacocoReportAdapter("jacoco.xml"));


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));
        workspace.child("jacoco.xml").copyFrom(getClass()
                .getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("A total of 2 reports were found", r);

    }
}
