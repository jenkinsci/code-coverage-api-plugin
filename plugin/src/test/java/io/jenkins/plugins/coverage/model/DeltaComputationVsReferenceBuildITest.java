package io.jenkins.plugins.coverage.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Node;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static edu.hm.hafner.metric.Metric.*;
import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Integration test for delta computation of reference builds.
 */
class DeltaComputationVsReferenceBuildITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_FILE = "jacoco-codingstyle.xml";

    /**
     * Checks if the delta coverage can be computed regarding a reference build within a freestyle project.
     */
    @Test
    void freestyleProjectTryCreatingReferenceBuildWithDeltaComputation() {
        FreeStyleProject project = createFreeStyleProjectWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE,
                JACOCO_CODINGSTYLE_FILE);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.singletonList(new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE)));
        project.getPublishersList().add(coveragePublisher);

        //run first build
        Run<?, ?> firstBuild = buildSuccessfully(project);

        // update adapter
        coveragePublisher.setAdapters(Collections.singletonList(new JacocoReportAdapter(JACOCO_CODINGSTYLE_FILE)));

        //run second build
        Run<?, ?> secondBuild = buildSuccessfully(project);

        verifyDeltaComputation(firstBuild, secondBuild);
    }

    /**
     * Checks if the delta coverage can be computed regarding a reference build within a pipeline project.
     */
    @Test
    void pipelineCreatingReferenceBuildWithDeltaComputation() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE, JACOCO_CODINGSTYLE_FILE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_ANALYSIS_MODEL_FILE
                + "')]"
                + "}", true));

        Run<?, ?> firstBuild = buildSuccessfully(job);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('" + JACOCO_CODINGSTYLE_FILE + "')]\n"
                + "discoverReferenceBuild(referenceJob: '" + job.getName() + "')"
                + "}", true));

        Run<?, ?> secondBuild = buildSuccessfully(job);

        verifyDeltaComputation(firstBuild, secondBuild);
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

        assertThat(coverageBuildAction.getDelta()).contains(
                new SimpleEntry<>(LINE, Fraction.getFraction(-2_315_425, 514_216)),
                new SimpleEntry<>(BRANCH, Fraction.getFraction(11_699, 2175)),
                new SimpleEntry<>(INSTRUCTION, Fraction.getFraction(-235_580, 81_957)),
                new SimpleEntry<>(METHOD, Fraction.getFraction(-217_450, 94_299))
        );

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
