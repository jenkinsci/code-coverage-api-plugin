package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Provides some helper methods to create different job types that will record code coverage results.
 *
 * @author Ullrich Hafner
 */
public abstract class AbstractCoverageITest extends IntegrationTestWithJenkinsPerSuite {
    static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    static final int JACOCO_ANALYSIS_MODEL_COVERED = 5531;
    static final int JACOCO_ANALYSIS_MODEL_MISSED = 267;
    static final int JACOCO_ANALYSIS_MODEL_TOTAL
            = JACOCO_ANALYSIS_MODEL_COVERED + JACOCO_ANALYSIS_MODEL_MISSED;

    static final String JACOCO_CODING_STYLE_FILE = "jacoco-codingstyle.xml";
    static final int JACOCO_CODING_STYLE_COVERED = 294;
    static final int JACOCO_CODING_STYLE_MISSED = 29;
    static final int JACOCO_CODING_STYLE_TOTAL
            = JACOCO_CODING_STYLE_COVERED + JACOCO_CODING_STYLE_MISSED;

    protected FreeStyleProject createFreestyleJob(final CoverageParser parser, final String... fileNames) {
        return createFreestyleJob(parser, i -> { }, fileNames);
    }

    protected FreeStyleProject createFreestyleJob(final CoverageParser parser,
            final Consumer<CoverageRecorder> configuration, final String... fileNames) {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(fileNames);

        addCoverageRecorder(project, parser, "**/*xml", configuration);

        return project;
    }

    void addCoverageRecorder(final FreeStyleProject project, final CoverageParser parser, final String pattern) {
        addCoverageRecorder(project, parser, pattern, i -> { });
    }

    void addCoverageRecorder(final FreeStyleProject project,
            final CoverageParser parser, final String pattern, final Consumer<CoverageRecorder> configuration) {
        CoverageRecorder recorder = new CoverageRecorder();

        var tool = new CoverageTool();
        tool.setParser(parser);
        tool.setPattern(pattern);
        recorder.setTools(List.of(tool));

        configuration.accept(recorder);

        try {
            project.getPublishersList().remove(CoverageRecorder.class);
        }
        catch (IOException exception) {
            // ignore and continue
        }
        project.getPublishersList().add(recorder);
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
