package io.jenkins.plugins.coverage.metrics;

import org.junit.jupiter.api.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;

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

        assertThatJson(callJsonRemoteApi(build.getUrl() + "coverage/api/json").getJSONObject())
                .node("projectStatistics").isEqualTo("{\n"
                        + "BRANCH: \"88.28%\",\n"
                        + "CLASS: \"98.81%\",\n"
                        + "COMPLEXITY: \"2558\",\n"
                        + "COMPLEXITY_DENSITY: \"+44.12%\",\n"
                        + "FILE: \"99.67%\",\n"
                        + "INSTRUCTION: \"96.11%\",\n"
                        + "LINE: \"95.39%\",\n"
                        + "LOC: \"5798\",\n"
                        + "METHOD: \"97.29%\",\n"
                        + "MODULE: \"100.00%\",\n"
                        + "PACKAGE: \"100.00%\"\n"
                        + "  }\n");
    }
}
