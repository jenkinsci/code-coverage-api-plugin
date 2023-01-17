package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;

import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.Parser;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

/**
 * Provides some helper methods to create different job types that will record code coverage results.
 *
 * @author Ullrich Hafner
 */
public abstract class AbstractCoverageITest extends IntegrationTestWithJenkinsPerSuite {
    protected FreeStyleProject createFreestyleJob(final Parser parser, final String... fileNames) {
        return createFreestyleJob(parser, i -> { }, fileNames);
    }

    protected FreeStyleProject createFreestyleJob(final Parser parser,
            final Consumer<CoverageRecorder> configuration, final String... fileNames) {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(fileNames);

        addCoverageRecorder(project, parser, "**/*xml", configuration);

        return project;
    }

    protected void addCoverageRecorder(final FreeStyleProject project,
            final Parser parser, final String pattern) {
        addCoverageRecorder(project, parser, pattern, i -> { });
    }

    void addCoverageRecorder(final FreeStyleProject project,
            final Parser parser, final String pattern, final Consumer<CoverageRecorder> configuration) {
        CoverageRecorder recorder = new CoverageRecorder();

        var tool = new io.jenkins.plugins.coverage.metrics.steps.CoverageTool();
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

    protected WorkflowJob createPipeline(final Parser parser, final String... fileNames) {
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

    protected WorkflowJob createDeclarativePipeline(final Parser parser, final String... fileNames) {
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
