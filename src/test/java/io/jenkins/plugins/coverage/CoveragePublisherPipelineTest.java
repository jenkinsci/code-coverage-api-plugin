package io.jenkins.plugins.coverage;

import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.model.Result;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.detector.AntPathReportDetector;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import io.jenkins.plugins.coverage.threshold.Threshold;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
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

    @Test
    public void testFailUnhealthy() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setFailUnhealthy(true)
                .setApplyThresholdRecursively(true);

        Threshold lineThreshold = new Threshold("Line");
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
        j.assertLogContains("Build failed because following metrics did not meet health target", r);
    }

    @Test
    public void testFailUnhealthyGracefully() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setFailUnhealthy(true)
                .setApplyThresholdRecursively(true);

        Threshold lineThreshold = new Threshold("Line");
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
        j.assertLogContains("Build failed because following metrics did not meet health target", r);
        Assert.assertNotNull(r.getAction(CoverageAction.class));
        Assert.assertNotNull(r.getAction(CoverageAction.class).getResult());
    }

    @Test
    public void testFailUnstable() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setFailUnstable(true)
                .setApplyThresholdRecursively(true);

        Threshold lineThreshold = new Threshold("Line");
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
        j.assertLogContains("Build failed because following metrics did not meet stability target", r);
    }

    @Test
    public void testFailUnstableGracefully() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setFailUnstable(true)
                .setApplyThresholdRecursively(true);

        Threshold lineThreshold = new Threshold("Line");
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
        j.assertLogContains("Build failed because following metrics did not meet stability target", r);
        Assert.assertNotNull(r.getAction(CoverageAction.class));
        Assert.assertNotNull(r.getAction(CoverageAction.class).getResult());
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
                .addAdapter(new JacocoReportAdapter("jacoco.xml"))
                .setApplyThresholdRecursively(true);

        Threshold lineThreshold = new Threshold("Line");
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
        j.assertLogContains("Build failed because following metrics did not meet health target", r);
    }

    @Test
    public void testAdapterThresholdFailUnhealthy() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .setApplyThresholdRecursively(true);

        JacocoReportAdapter adapter = new JacocoReportAdapter("jacoco.xml");

        Threshold lineThreshold = new Threshold("Line");
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
        j.assertLogContains("Build failed because following metrics did not meet health target", r);
    }

    @Test
    public void testAbsolutePathSourceFile() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .setEnableSourceFileResolver(true);

        CoberturaReportAdapter adapter = new CoberturaReportAdapter("cobertura-coverage.xml");
        builder.addAdapter(adapter);

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));

        workspace.child("cc.js")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));

        String absoluteSourceFilePath = workspace.child("cc.js").getRemote();

        String sourceFileContent = workspace
                .child("cobertura-coverage.xml")
                .readToString()
                .replace("filename=\"cc.js\"", "filename=\"" + absoluteSourceFilePath + "\"");

        workspace.child("cobertura-coverage.xml")
                .write(sourceFileContent, "utf-8");

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);
        j.assertBuildStatus(Result.SUCCESS, j.waitForCompletion(r));

        File sourceFile = new File(r.getRootDir(), DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + absoluteSourceFilePath.replaceAll("[^a-zA-Z0-9-_.]", "_"));

        System.out.println("Listing file below source files directory");
        for (File file : Objects.requireNonNull(sourceFile.getParentFile().listFiles())) {
            System.out.println(file.getAbsolutePath());
        }

        Assert.assertTrue(String.format("Source file path %s .%nDestination file path %s", absoluteSourceFilePath, sourceFile), sourceFile.exists());
    }

    @Test
    public void testRelativePathSourceFile() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .setEnableSourceFileResolver(true);

        CoberturaReportAdapter adapter = new CoberturaReportAdapter("cobertura-coverage.xml");
        builder.addAdapter(adapter);

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));

        workspace.child("cc.js")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));

        String relativeSourcePath = "cc.js";

        String sourceFileContent = workspace
                .child("cobertura-coverage.xml")
                .readToString()
                .replaceAll("cc.js", relativeSourcePath);

        workspace.child("cobertura-coverage.xml")
                .write(sourceFileContent, "utf-8");

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);
        j.assertBuildStatus(Result.SUCCESS, j.waitForCompletion(r));

        File sourceFile = new File(r.getRootDir(), DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + relativeSourcePath.replaceAll("[^a-zA-Z0-9-_.]", "_"));

        Assert.assertTrue(sourceFile.exists());
    }

}
