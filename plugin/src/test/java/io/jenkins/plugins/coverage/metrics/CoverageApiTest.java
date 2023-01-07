package io.jenkins.plugins.coverage.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import net.sf.json.JSONObject;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateCriticality;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/**
 * Tests the class {@link CoverageApi}.
 *
 * @author Ullrich Hafner
 */
class CoverageApiTest extends AbstractCoverageITest {
    @Test
    void shouldProvideRemoteApi() {
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        var remoteApiResult = callRemoteApi(build);
        assertThatJson(remoteApiResult)
                .node("projectStatistics").isEqualTo("{\n"
                        + "  \"branch\": \"88.28%\",\n"
                        + "  \"complexity\": \"2558\",\n"
                        + "  \"complexity-density\": \"+44.12%\",\n"
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
        var qualityGates = List.of(new QualityGate(100, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, r -> r.setQualityGates(qualityGates), JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);

        var remoteApiResult = callRemoteApi(build);
        assertThatJson(remoteApiResult)
                .node("projectStatistics").isEqualTo("{\n"
                        + "  \"branch\": \"88.28%\",\n"
                        + "  \"complexity\": \"2558\",\n"
                        + "  \"complexity-density\": \"+44.12%\",\n"
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

    private JSONObject callRemoteApi(final Run<?, ?> build) {
        return callJsonRemoteApi(build.getUrl() + "coverage/api/json").getJSONObject();
    }
}
