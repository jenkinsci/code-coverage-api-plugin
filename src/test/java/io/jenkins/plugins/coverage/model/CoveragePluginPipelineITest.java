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
}
