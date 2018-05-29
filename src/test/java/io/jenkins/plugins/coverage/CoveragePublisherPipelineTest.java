package io.jenkins.plugins.coverage;

import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Result;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import io.jenkins.plugins.coverage.threshold.Threshold;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class CoveragePublisherPipelineTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testAutoDetect() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();

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
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatus(Result.FAILURE, j.waitForCompletion(r));
        j.assertLogContains("No reports were found in this path", r);
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

    public static class CoverageScriptedPipelineScriptBuilder {

        private String autoDetectPath = "*.xml";

        private List<CoverageReportAdapter> adapters = new LinkedList<>();

        private List<Threshold> globalThresholds = new LinkedList<>();

        private boolean failUnhealthy;
        private boolean failUnstable;
        private boolean failNoReports;

        private CoverageScriptedPipelineScriptBuilder() {
        }


        public static CoverageScriptedPipelineScriptBuilder builder() {
            return new CoverageScriptedPipelineScriptBuilder();
        }

        public String getAutoDetectPath() {
            return autoDetectPath;
        }

        public CoverageScriptedPipelineScriptBuilder setAutoDetectPath(String autoDetectPath) {
            this.autoDetectPath = autoDetectPath;
            return this;
        }

        public CoverageScriptedPipelineScriptBuilder addAdapter(CoverageReportAdapter adapter) {
            adapters.add(adapter);
            return this;
        }

        public CoverageScriptedPipelineScriptBuilder addGlobalThreshold(Threshold threshold) {
            globalThresholds.add(threshold);
            return this;
        }


        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append("node {")
                    .append("publishCoverage(");

            sb.append("autoDetectPath:'").append(autoDetectPath).append("'");

            sb.append(",failUnhealthy:").append(failUnhealthy);
            sb.append(",failUnstable:").append(failUnstable);
            sb.append(",failNoReports:").append(failNoReports);

            if (adapters.size() > 0) {
                sb.append(",adapters:[");
                for (int i = 0; i < adapters.size(); i++) {
                    if (i > 0) sb.append(",");

                    CoverageReportAdapter adapter = adapters.get(i);
                    sb.append(generateSnippetForAdapter(adapter));
                }
                sb.append(']');
            }

            sb.append(",globalThresholds: ").append(generateSnippetForThresholds(globalThresholds));

            sb.append(")").append("}");

            return sb.toString();
        }

        private String generateSnippetForAdapter(CoverageReportAdapter adapter) {
            Descriptor<CoverageReportAdapter> d = adapter.getDescriptor();
            Class c = d.getClass();

            if (c.isAnnotationPresent(Symbol.class)) {
                Symbol s = (Symbol) c.getAnnotation(Symbol.class);
                String[] symbolValues = s.value();

                if (symbolValues.length > 0) {
                    String symbol = symbolValues[0];

                    return symbol + "(path:'" + adapter.getPath()
                            + "', thresholds: " + generateSnippetForThresholds(adapter.getThresholds()) + ")";
                }
            }

            //TODO condition when adapter do not have @Symbol annotation.
            return "";
        }

        private String generateSnippetForThresholds(List<Threshold> thresholds) {
            StringBuilder sb = new StringBuilder();

            sb.append("[");
            for (int i = 0; i < thresholds.size(); i++) {
                if (i > 0) sb.append(",");

                Threshold threshold = thresholds.get(i);
                sb.append("[thresholdTarget: '").append(threshold.getThresholdTarget())
                        .append("', unhealthyThreshold: ").append(threshold.getUnhealthyThreshold())
                        .append(", unstableThreshold: ").append(threshold.getUnstableThreshold())
                        .append(", failUnhealthy: ").append(threshold.isFailUnhealthy())
                        .append("]");
            }
            sb.append("]");

            return sb.toString();
        }


        public CoverageScriptedPipelineScriptBuilder setFailUnhealthy(boolean failUnhealthy) {
            this.failUnhealthy = failUnhealthy;
            return this;
        }

        public CoverageScriptedPipelineScriptBuilder setFailUnstable(boolean failUnstable) {
            this.failUnstable = failUnstable;
            return this;
        }

        public CoverageScriptedPipelineScriptBuilder setFailNoReports(boolean failNoReports) {
            this.failNoReports = failNoReports;
            return this;
        }

        @Override
        public String toString() {
            return build();
        }
    }

}
