package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Node;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;

import static edu.hm.hafner.metric.Metric.*;
import static io.jenkins.plugins.coverage.model.Assertions.*;
import static org.assertj.core.data.Percentage.*;

/**
 * Integration test for delta computation of reference builds.
 */
class DeltaComputationVsReferenceBuildITest extends AbstractCoverageITest {
    @Test
    void shouldComputeDeltaInFreestyleJob() {
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO,
                JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODING_STYLE_FILE);

        Run<?, ?> firstBuild = buildSuccessfully(project);
        verifyFirstBuild(firstBuild);

        // update parser pattern to pick only the coding style results
        project.getPublishersList().get(CoverageRecorder.class).getTools().get(0).setPattern(JACOCO_CODING_STYLE_FILE);

        Run<?, ?> secondBuild = buildSuccessfully(project);
        verifySecondBuild(secondBuild);

        verifyDeltaComputation(firstBuild, secondBuild);
    }

    @Test
    void shouldComputeDeltaInPipeline() {
        WorkflowJob job = createPipeline(CoverageParser.JACOCO, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODING_STYLE_FILE);

        Run<?, ?> firstBuild = buildSuccessfully(job);
        verifyFirstBuild(firstBuild);

        // update parser pattern to pick only the codingstyle results
        setPipelineScript(job,
                "recordCoverage tools: [[parser: 'JACOCO', pattern: '" + JACOCO_CODING_STYLE_FILE + "']]");

        Run<?, ?> secondBuild = buildSuccessfully(job);
        verifySecondBuild(secondBuild);

        verifyDeltaComputation(firstBuild, secondBuild);
    }

    private static void verifyFirstBuild(final Run<?, ?> firstBuild) {
        var action = firstBuild.getAction(CoverageBuildAction.class);

        var lineCoverage = action.getLineCoverage();
        assertThat(lineCoverage.getCovered()).isEqualTo(5882); // 294 + 5531 ?
        assertThat(lineCoverage.getCoveredPercentage().doubleValue()).isCloseTo(0.95, withPercentage(1.0));

        var branchCoverage = action.getBranchCoverage();
        assertThat(branchCoverage.getCovered()).isEqualTo(1544 + 109);
        assertThat(branchCoverage.getCoveredPercentage().doubleValue()).isCloseTo(0.88, withPercentage(1.0));
    }

    private static void verifySecondBuild(final Run<?, ?> secondBuild) {
        var action = secondBuild.getAction(CoverageBuildAction.class);

        var lineCoverage = action.getLineCoverage();
        assertThat(lineCoverage).extracting(Coverage::getCovered).isEqualTo(294);
        assertThat(lineCoverage.getCoveredPercentage().doubleValue()).isCloseTo(0.91, withPercentage(1.0));

        var branchCoverage = action.getBranchCoverage();
        assertThat(branchCoverage.getCovered()).isEqualTo(109);
        assertThat(branchCoverage.getCoveredPercentage().doubleValue()).isCloseTo(0.94, withPercentage(1.0));
    }

    /**
     * Verifies the coverageComputation of the first and second build of the job.
     *
     * @param firstBuild
     *         of the project which is used as a reference
     * @param secondBuild
     *         of the project
     */
    private void verifyDeltaComputation(final Run<?, ?> firstBuild, final Run<?, ?> secondBuild) {
        assertThat(secondBuild.getAction(CoverageBuildAction.class)).isNotNull();

        CoverageBuildAction coverageBuildAction = secondBuild.getAction(CoverageBuildAction.class);

        assertThat(coverageBuildAction).isNotNull();
        assertThat(coverageBuildAction.getReferenceBuild())
                .isPresent()
                .satisfies(reference -> assertThat(reference.get()).isEqualTo(firstBuild));

        var delta = coverageBuildAction.getDelta();
        assertThat(delta.get(LINE).doubleValue()).isCloseTo(-0.0415, withPercentage(1.0));
        assertThat(delta.get(BRANCH).doubleValue()).isCloseTo(0.0533, withPercentage(1.0));
        // TODO: compute delta for other metrics

        verifyChangeCoverage(coverageBuildAction);
    }

    /**
     * Verifies the calculated change coverage including the change coverage delta and the code delta. This makes sure
     * these metrics are set properly even if there are no code changes.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyChangeCoverage(final CoverageBuildAction action) {
        verifyCodeDelta(action);
        assertThat(action.hasChangeCoverage()).isFalse();
        assertThat(action.hasChangeCoverageDifference(LINE)).isFalse();
        assertThat(action.hasChangeCoverageDifference(BRANCH)).isFalse();
    }

    /**
     * Verifies the calculated code delta.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyCodeDelta(final CoverageBuildAction action) {
        Node root = action.getResult();
        assertThat(root).isNotNull();
        assertThat(root.getAllFileNodes()).flatExtracting(FileNode::getChangedLines).isEmpty();
    }
}
