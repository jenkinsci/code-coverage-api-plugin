package io.jenkins.plugins.coverage.metrics;

import org.junit.jupiter.api.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageRecorder}.
 *
 * @author Ullrich Hafner
 */
class CoverageRecorderTest extends IntegrationTestWithJenkinsPerSuite {

    @Test
    void shouldIgnoreEmptyListOfFiles() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition(
                "node {\n"
                        + "    recordCoverage tools: [[parser: 'JACOCO']]\n"
                        + " }\n", true));

        Run<?, ?> run = buildWithResult(job, Result.SUCCESS);

        assertThat(getConsoleLog(run))
                .contains("[JaCoCo] Using default pattern '**/jacoco.xml' since user defined pattern is not set",
                        "[-ERROR-] No files found for pattern '**/jacoco.xml'. Configuration error?")
                .containsPattern("Searching for all files in '.*' that match the pattern '\\*\\*/jacoco.xml'")
                .doesNotContain("Expanding pattern");
    }

    @Test
    void shouldParseFileWithJaCoCo() {
        WorkflowJob job = createPipeline();
        copyFilesToWorkspace(job, "jacoco.xml");
        job.setDefinition(new CpsFlowDefinition(
                "node {\n"
                        + "    recordCoverage tools: [[parser: 'JACOCO']]\n"
                        + " }\n", true));

        Run<?, ?> run = buildWithResult(job, Result.SUCCESS);

        assertThat(getConsoleLog(run))
                .contains("[JaCoCo] Using default pattern '**/jacoco.xml' since user defined pattern is not set",
                        "[JaCoCo] -> found 1 file",
                        "[JaCoCo] MODULE: 100.00% (1/1)",
                        "[JaCoCo] PACKAGE: 100.00% (1/1)",
                        "[JaCoCo] FILE: 70.00% (7/10)",
                        "[JaCoCo] CLASS: 83.33% (15/18)",
                        "[JaCoCo] METHOD: 95.10% (97/102)",
                        "[JaCoCo] INSTRUCTION: 93.33% (1260/1350)",
                        "[JaCoCo] LINE: 91.02% (294/323)",
                        "[JaCoCo] BRANCH: 93.97% (109/116)",
                        "[JaCoCo] COMPLEXITY: 160")
                .containsPattern("Searching for all files in '.*' that match the pattern '\\*\\*/jacoco.xml'")
                .containsPattern("Successfully parsed file '.*/jacoco.xml'")
                .doesNotContain("Expanding pattern");
    }
}
