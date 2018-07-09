package io.jenkins.plugins.coverage;

import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.model.Result;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.LLVMCovReportAdapter;
import io.jenkins.plugins.coverage.detector.AntPathReportDetector;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import io.jenkins.plugins.coverage.threshold.Threshold;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Objects;

public class CoveragePublisherPipelineTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testAutoDetect() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new AntPathReportDetector("**/*.xml"));

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();
        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("Auto Detect was ended: Found 1 report", r);
    }

    @Test
    public void testAdapters() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
//                .addAdapter(new CoberturaReportAdapter("cobertura-coverage.xml"))
//                .addAdapter(new JacocoReportAdapter("jacoco.xml"));
        .addAdapter(new LLVMCovReportAdapter("llvm-cov.json"));


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("llvm-cov.json")
                .copyFrom(getClass().getResourceAsStream("llvm-cov.json"));
//        Objects.requireNonNull(workspace)
//                .child("cobertura-coverage.xml")
//                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));
//        workspace.child("jacoco.xml").copyFrom(getClass()
//                .getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("A total of 1 reports were found", r);

    }

    @Test
    public void testFailUnhealthy() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setFailUnhealthy(true);

        Threshold lineThreshold = new Threshold(CoverageMetric.LINE);
        lineThreshold.setUnhealthyThreshold(20);

        builder.addGlobalThreshold(lineThreshold);


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("jacoco.xml")
                .copyFrom(getClass().getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatus(Result.FAILURE, j.waitForCompletion(r));
        j.assertLogContains("Publish Coverage Failed (Unhealthy):", r);
    }

    @Test
    public void testFailUnstable() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setFailUnstable(true);

        Threshold lineThreshold = new Threshold(CoverageMetric.LINE);
        lineThreshold.setUnstableThreshold(20);

        builder.addGlobalThreshold(lineThreshold);


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("jacoco.xml")
                .copyFrom(getClass().getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatus(Result.FAILURE, j.waitForCompletion(r));
        j.assertLogContains("Publish Coverage Failed (Unstable):", r);
    }

    @Test
    public void testFailNoReports() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .setFailNoReports(true);

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatus(Result.FAILURE, j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);
        j.assertLogContains("Publish Coverage Failed : No Reports were found", r);
    }

    @Test
    public void testGlobalThresholdFailUnhealthy() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"));

        Threshold lineThreshold = new Threshold(CoverageMetric.LINE);
        lineThreshold.setUnhealthyThreshold(20);
        lineThreshold.setFailUnhealthy(true);

        builder.addGlobalThreshold(lineThreshold);


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("jacoco.xml")
                .copyFrom(getClass().getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatus(Result.FAILURE, j.waitForCompletion(r));
        j.assertLogContains("Publish Coverage Failed (Unhealthy):", r);
    }

    @Test
    public void testAdapterThresholdFailUnhealthy() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();

        JacocoReportAdapter adapter = new JacocoReportAdapter("jacoco.xml");

        Threshold lineThreshold = new Threshold(CoverageMetric.LINE);
        lineThreshold.setUnhealthyThreshold(20);
        lineThreshold.setFailUnhealthy(true);

        adapter.setThresholds(Lists.newArrayList(lineThreshold));

        builder.addAdapter(adapter);

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("jacoco.xml")
                .copyFrom(getClass().getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatus(Result.FAILURE, j.waitForCompletion(r));
        j.assertLogContains("Publish Coverage Failed (Unhealthy):", r);
    }

}
