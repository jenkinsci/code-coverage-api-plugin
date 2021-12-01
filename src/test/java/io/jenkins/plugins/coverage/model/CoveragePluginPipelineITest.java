package io.jenkins.plugins.coverage.model;

import hudson.FilePath;
import hudson.model.HealthReportingAction;
import io.jenkins.plugins.coverage.CoverageScriptedPipelineScriptBuilder;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class CoveragePluginPipelineITest {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    //private static final String COBERTURA_FILE_NAME = "../cobertura-coverage.xml";
    private static final String COBERTURA_FILE_NAME = "coverage-with-lots-of-data.xml";

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testAdapters() throws Exception {
        CoberturaReportAdapter blub = new CoberturaReportAdapter("cobertura-coverage.xml");
        blub.setMergeToOneReport(true);
        JacocoReportAdapter blob = new JacocoReportAdapter("cobertura-coverage.xml");
        blob.setMergeToOneReport(true);
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(blub)
                .addAdapter(blob);


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

        CoverageBuildAction coverageResult = r.getAction(CoverageBuildAction.class);
        HealthReportingAction x = r.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
        //assertThat(coverageResult.getLineCoverage())
         //       .isEqualTo(new Coverage(5319, 5581 - 5319));
        String xy = coverageResult.getDelta(CoverageMetric.FILE);
        //int y = coverageResult.getReferenceBuild().get()
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("A total of 2 reports were found", r);

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
    public void testAdapters1() throws Exception {
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
