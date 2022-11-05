package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Node;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests if source code copying and rendering and copying works on Docker agents.
 */
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class DockerAndSourceCodeRenderingITest extends AbstractCoverageITest {
    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";
    private static final String REPOSITORY = "https://github.com/jenkinsci/analysis-model.git";
    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer();

    @Test
    void shouldCopyAndRenderSourceCodeAndRenderingInPipelineOnDockerAgent() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        WorkflowJob project = createPipelineWithGitAndJacocoAdapter("node('" + DOCKER_AGENT_NAME + "')");

        copySingleFileToAgentWorkspace(agent, project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = verifyGitRepository(project);

        verifySourceCode(build);
    }

    @Test
    void shouldCopyAndRenderSourceCodeAndRenderingInFreestyleJobOnAgent() throws IOException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE);
        project.setAssignedNode(agent);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new CoverageBuilder().setMetric(Metric.LINE)
                        .setCovered(JACOCO_ANALYSIS_MODEL_COVERED)
                        .setMissed(JACOCO_ANALYSIS_MODEL_MISSED).build());
    }

    private void verifySourceCode(final Run<?, ?> build) {
        ModuleNode root = new ModuleNode("top-level");

        CoverageBuildAction action = new CoverageBuildAction(build, root, new HealthReport(), "-",
                new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), false);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getNode).isEqualTo(root);
        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);
    }

    private Run<?, ?> verifyGitRepository(final WorkflowJob workflowJob) {
        Run<?, ?> build = buildSuccessfully(workflowJob);

        String consoleLog = getConsoleLog(build);

        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("checkout -f " + COMMIT);
        return build;
    }

    private WorkflowJob createPipelineWithGitAndJacocoAdapter(final String node) {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition(node + " {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + COMMIT + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    recordCoverage tools: [[parser: 'JACOCO', pattern: '"
                + JACOCO_ANALYSIS_MODEL_FILE
                + "']], sourceCodeRetention: 'LAST_BUILD' \n"
                + "}", true));

        return job;
    }
}
