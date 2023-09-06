package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;

import net.sf.json.JSONObject;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageITest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.restapi.CoverageApi;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.Parser;
import io.jenkins.plugins.util.QualityGate.QualityGateCriticality;

import static io.jenkins.plugins.coverage.metrics.AbstractCoverageTest.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/**
 * Tests the class {@link CoverageApi}.
 *
 * @author Ullrich Hafner
 */
class CoverageApiITest extends AbstractCoverageITest {
    @Test
    void shouldProvideRemoteApi() {
        FreeStyleProject project = createFreestyleJob(Parser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        var remoteApiResult = callRemoteApi(build);
        assertThatJson(remoteApiResult)
                .node("projectStatistics").isEqualTo("{\n"
                        + "  \"branch\": \"88.28%\",\n"
                        + "  \"complexity\": \"2558\",\n"
                        + "  \"complexity-density\": \"+44.12%\",\n"
                        + "  \"complexity-maximum\": \"21\",\n"
                        + "  \"file\": \"99.67%\",\n"
                        + "  \"instruction\": \"96.11%\",\n"
                        + "  \"line\": \"95.39%\",\n"
                        + "  \"loc\": \"5798\",\n"
                        + "  \"method\": \"97.29%\",\n"
                        + "  \"module\": \"100.00%\",\n"
                        + "  \"package\": \"100.00%\"}");
        assertThatJson(remoteApiResult)
                .node("modifiedFilesStatistics").isEqualTo("{}");
        assertThatJson(remoteApiResult)
                .node("modifiedLinesStatistics").isEqualTo("{}");
    }

    @Test
    void shouldShowQualityGatesInRemoteApi() {
        var qualityGates = List.of(new CoverageQualityGate(100, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        FreeStyleProject project = createFreestyleJob(Parser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        var remoteApiResult = callRemoteApi(build);
        assertThatJson(remoteApiResult)
                .node("qualityGates.overallResult").isEqualTo("UNSTABLE");
        assertThatJson(remoteApiResult)
                .node("qualityGates.resultItems").isEqualTo("[{\n"
                        + "  \"qualityGate\": \"Overall project - Line Coverage\",\n"
                        + "  \"result\": \"UNSTABLE\",\n"
                        + "  \"threshold\": 100.0,\n"
                        + "  \"value\": \"95.39%\"\n"
                        + "}]\n");
    }

    @Test
    void shouldShowDeltaInRemoteApi() {
        FreeStyleProject project = createFreestyleJob(Parser.JACOCO,
                JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODING_STYLE_FILE);

        buildSuccessfully(project);
        // update parser pattern to pick only the coding style results
        project.getPublishersList().get(CoverageRecorder.class).getTools().get(0).setPattern(JACOCO_CODING_STYLE_FILE);
        Run<?, ?> secondBuild = buildSuccessfully(project);

        var remoteApiResult = callRemoteApi(secondBuild);
        assertThatJson(remoteApiResult)
                .node("projectDelta").isEqualTo("{\n"
                        + "  \"branch\": \"+5.33%\",\n"
                        + "  \"complexity\": \"-2558\",\n"
                        + "  \"complexity-density\": \"+5.13%\",\n"
                        + "  \"complexity-maximum\": \"-15\",\n"
                        + "  \"file\": \"-28.74%\",\n"
                        + "  \"instruction\": \"-2.63%\",\n"
                        + "  \"line\": \"-4.14%\",\n"
                        + "  \"loc\": \"-5798\",\n"
                        + "  \"method\": \"-2.06%\",\n"
                        + "  \"module\": \"+0.00%\",\n"
                        + "  \"package\": \"+0.00%\"\n"
                        + "}");
        assertThatJson(remoteApiResult).node("referenceBuild").asString()
                .matches("<a href=\".*jenkins/job/test0/1/\".*>test0 #1</a>");
    }

    private JSONObject callRemoteApi(final Run<?, ?> build) {
        return callJsonRemoteApi(build.getUrl() + "coverage/api/json").getJSONObject();
    }
}
