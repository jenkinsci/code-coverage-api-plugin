package io.jenkins.plugins.coverage.model;

import java.util.List;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;

import io.jenkins.plugins.coverage.metrics.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.CoverageTool;
import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Provides some helper methods to create different job types that will record code coverage results.
 *
 * @author Ullrich Hafner
 */
public abstract class AbstractCoverageITest extends IntegrationTestWithJenkinsPerSuite {
    static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    static final String JACOCO_CODING_STYLE_FILE = "jacoco-codingstyle.xml";

    protected FreeStyleProject createFreestyleJob(final CoverageParser parser, final String... fileNames) {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(fileNames);

        CoverageRecorder recorder = new CoverageRecorder();
        var tool = new CoverageTool();
        tool.setParser(parser);
        tool.setPattern("**/*xml");
        recorder.setTools(List.of(tool));
        project.getPublishersList().add(recorder);

        return project;
    }

    protected WorkflowJob createPipeline(final CoverageParser parser, final String... fileNames) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(fileNames);

        setPipelineScript(job,
                "recordCoverage tools: [[parser: '" + parser.name() + "', pattern: '**/*xml']]");

        return job;
    }

    protected void setPipelineScript(final WorkflowJob job, final String recorderSnippet) {
        job.setDefinition(new CpsFlowDefinition(
                "node {\n"
                        + recorderSnippet + "\n"
                        + " }\n", true));
    }

    protected WorkflowJob createDeclarativePipeline(final CoverageParser parser, final String... fileNames) {
        WorkflowJob job = createPipelineWithWorkspaceFiles(fileNames);

        job.setDefinition(new CpsFlowDefinition("pipeline {\n"
                + "    agent any\n"
                + "    stages {\n"
                + "        stage('Test') {\n"
                + "            steps {\n"
                + "                    recordCoverage(\n"
                + "                        tools: [[parser: '" + parser.name() + "', pattern: '**/*xml']]"
                + "            )}\n"
                + "        }\n"
                + "    }\n"
                + "}", true));
        return job;
    }
}
