package io.jenkins.plugins.coverage.model;

import hudson.model.Result;
import hudson.model.Run;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Pipeline integration tests with docker for the CoveragePlugin.
 *
 * @author Johannes Walter
 */
public class CoveragePluginPipelineDockerITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_BIG_DATA = "jacoco-analysis-model.xml";
    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";
    private static final String REPOSITORY = "https://github.com/jenkinsci/analysis-model.git";
    private static final TestUtil TEST_UTIL = new TestUtil();

    /**
     * Tests pipeline execution with an agent in docker.
     * @throws IOException from getLogFromInputStream {@link InputStream}
     * @throws InterruptedException by the java docker rule
     */
    @Test
    public void agentInDocker() throws IOException, InterruptedException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();
        DumbSlave agent = TEST_UTIL.createDockerContainerAgent();
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, JACOCO_BIG_DATA, JACOCO_BIG_DATA);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        String consoleLog = getConsoleLog(build);
        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("git checkout -f " + COMMIT);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    /**
     * Creates a pipeline on an agent.
     * @return the configured {@link WorkflowJob}
     */
    private WorkflowJob createPipelineOnAgent() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node('docker') {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + COMMIT + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_BIG_DATA + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}", true));

        return job;
    }

    /**
     * Tests the source code rendering and copying on an agent.
     */
    @Test
    public void sourceCodeRenderingAndCopyingAgent() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();
        DumbSlave agent = TEST_UTIL.createDockerContainerAgent();
        WorkflowJob project = createPipelineOnAgent();

        copySingleFileToAgentWorkspace(agent, project, JACOCO_BIG_DATA, JACOCO_BIG_DATA);

        Run<?, ?> build = buildSuccessfully(project);

        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);

        String consoleLog = getConsoleLog(build);

        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("git checkout -f " + COMMIT);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);

        CoverageViewModel model = action.getTarget();

        CoverageViewModel.CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "Package", "File", "Class", "Method", "Line", "Instruction", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                21, 306, 344, 1801, 6083, 26_283, 1661
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 1, 5, 48, 285, 1036, 214
        );
    }
}
