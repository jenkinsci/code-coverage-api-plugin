package io.jenkins.plugins.coverage;

import hudson.FilePath;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
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

public class CoveragePublisherPipelineTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testAutoDetect() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();

        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        workspace.child("cobertura-coverage.xml").copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = project.scheduleBuild2(0).waitForStart();
        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("Auto Detect was ended: Found 1 report", r);
    }

    public void testAdapters() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder().
                addAdapter(new CoberturaReportAdapter("cobertura-coverage.xml"))
                .addAdapter(new JacocoReportAdapter("jacoco.xml"));


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        workspace.child("cobertura-coverage.xml").copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));
        workspace.child("jacoco.xml").copyFrom(getClass().getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = project.scheduleBuild2(0).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("A total of 2 reports were found", r);

    }

    public static class CoverageScriptedPipelineScriptBuilder {

        private String autoDetectPath = "*.xml";

        private List<CoverageReportAdapter> adapters = new LinkedList<>();

        private List<Threshold> globalThresholds = new LinkedList<>();

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

            if (adapters.size() > 0) {
                sb.append(",adapters:[");
                for (int i = 0; i < adapters.size(); i++) {
                    if (i > 0) sb.append(",");

                    CoverageReportAdapter adapter = adapters.get(i);
                    sb.append(generateSnippetForAdapter(adapter));
                }
                sb.append(']');
            }

            if (globalThresholds.size() > 0) {
                sb.append(",globalThresholds: [");
                for (int i = 0; i < globalThresholds.size(); i++) {
                    if (i > 0) sb.append(",");

                    Threshold threshold = globalThresholds.get(i);
                    sb.append(generateSnippetForThreshold(threshold));
                }
                sb.append("]");
            }

            sb.append(")")
                    .append("}");
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

                    return symbol + "(path:'" + adapter.getPath() + "')";
                }
            }

            //TODO condition when adapter do not have @Symbol annotation.
            return "";
        }

        private String generateSnippetForThreshold(Threshold threshold) {

            return "[healthyThresh:" + threshold.getHealthyThresh()
                    + ", threshTarget: '" + threshold.getThreshTarget().getName()
                    + "', unhealthyThresh: " + threshold.getUnhealthyThresh()
                    + ", unstableThresh: " + threshold.getUnstableThresh() + "]";
        }

        @Override
        public String toString() {
            return build();
        }
    }

}
