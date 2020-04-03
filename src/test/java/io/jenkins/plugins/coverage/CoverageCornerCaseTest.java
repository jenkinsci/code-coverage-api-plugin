package io.jenkins.plugins.coverage;

import hudson.FilePath;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import io.jenkins.plugins.coverage.adapter.util.XMLUtils;
import java.io.File;
import org.w3c.dom.Document;

public class CoverageCornerCaseTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testIfNoReportFound() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-corner-test");
        project.setDefinition(new CpsFlowDefinition(builder.build(), true));

        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();
        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);
    }

    @Test
    public void testIfFoundEmptyReport() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(new JacocoReportAdapter("jacoco.xml"));

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-corner-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Path emptyFile = Files.createTempFile("test", "found-empty-report");

        Objects.requireNonNull(workspace)
                .child("jacoco.xml")
                .copyFrom(emptyFile.toUri().toURL());
        FileUtils.deleteQuietly(emptyFile.toFile());

        workspace.child("cobertura-coverage.xml");
        FileUtils.deleteQuietly(emptyFile.toFile());

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));

        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();


        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);
    }


    @Test
    public void testPreventXXE() throws Exception {
        /*
          Test for SECURITY-1699: if external entities are executed an exception will be thrown
          as an invalid external entity (unknown protocol foobar) is defined in the supplied XML
          test file
       */
        Document d;
        File file = new File(getClass().getResource("sec1699.xml").toURI());
        d = XMLUtils.getInstance().readXMLtoDocument(file);
    }

}
