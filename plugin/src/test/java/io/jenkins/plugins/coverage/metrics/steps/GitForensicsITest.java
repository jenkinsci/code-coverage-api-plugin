package io.jenkins.plugins.coverage.metrics.steps;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.FileNode;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageITest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.Parser;
import io.jenkins.plugins.prism.SourceCodeRetention;

import static edu.hm.hafner.coverage.Metric.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests the integration of the Forensics API Plugin while using its Git implementation.
 *
 * @author Florian Orendi
 */
@Testcontainers(disabledWithoutDocker = true)
class GitForensicsITest extends AbstractCoverageITest {
    /** The JaCoCo coverage report, generated for the commit {@link #COMMIT}. */
    private static final String JACOCO_FILE = "forensics_integration.xml";
    /** The JaCoCo coverage report, generated for the reference commit {@link #COMMIT_REFERENCE}. */
    private static final String JACOCO_REFERENCE_FILE = "forensics_integration_reference.xml";

    private static final String COMMIT = "518eebd";
    private static final String COMMIT_REFERENCE = "fd43cd0";

    private static final String REPOSITORY = "https://github.com/jenkinsci/forensics-api-plugin.git";

    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer();

    @ParameterizedTest(name = "Source code retention {0} should store {1} files")
    @CsvSource({
            "EVERY_BUILD, 37",
            "MODIFIED, 2"
    })
    @DisplayName("Should compute delta report and store selected source files")
    void shouldComputeDeltaInPipelineOnDockerAgent(final SourceCodeRetention sourceCodeRetention,
            final int expectedNumberOfFilesToBeStored) {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        String node = "node('" + DOCKER_AGENT_NAME + "')";
        WorkflowJob project = createPipeline();
        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);

        project.setDefinition(createPipelineForCommit(node, COMMIT_REFERENCE, JACOCO_REFERENCE_FILE));
        Run<?, ?> referenceBuild = buildSuccessfully(project);
        verifyGitRepositoryForCommit(referenceBuild, COMMIT_REFERENCE);

        project.setDefinition(createPipelineForCommit(node, COMMIT, JACOCO_FILE, sourceCodeRetention));
        Run<?, ?> build = buildSuccessfully(project);
        verifyGitRepositoryForCommit(build, COMMIT);

        System.out.println(getConsoleLog(build));
        verifyGitIntegration(build, referenceBuild);

        assertThat(getConsoleLog(build)).contains(
                "[Coverage] -> 18 files contain changes",
                "[Coverage] Painting " + expectedNumberOfFilesToBeStored + " source files on agent");
    }

    @Test
    void shouldComputeDeltaInFreestyleJobOnDockerAgent() throws IOException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        FreeStyleProject project = createFreestyleJob(Parser.JACOCO);
        project.setAssignedNode(agent);

        configureGit(project, COMMIT_REFERENCE);
        addCoverageRecorder(project, Parser.JACOCO, JACOCO_REFERENCE_FILE);

        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);

        Run<?, ?> referenceBuild = buildSuccessfully(project);

        configureGit(project, COMMIT);
        addCoverageRecorder(project, Parser.JACOCO, JACOCO_FILE);

        Run<?, ?> build = buildSuccessfully(project);

        verifyGitIntegration(build, referenceBuild);
    }

    /**
     * Verifies the Git repository for the commit with the passed ID.
     *
     * @param build
     *         The current build
     * @param commit
     *         The commit ID
     */
    private void verifyGitRepositoryForCommit(final Run<?, ?> build, final String commit) {
        String consoleLog = getConsoleLog(build);
        assertThat(consoleLog)
                .contains("Recording commits of 'git " + REPOSITORY)
                .contains("Checking out Revision " + commit);
    }

    /**
     * Verifies the Git integration.
     *
     * @param build
     *         The current build
     * @param referenceBuild
     *         The reference build
     */
    private void verifyGitIntegration(final Run<?, ?> build, final Run<?, ?> referenceBuild) {
        CoverageBuildAction action = build.getAction(CoverageBuildAction.class);
        assertThat(action).isNotNull();
        assertThat(action.getReferenceBuild())
                .isPresent()
                .satisfies(reference ->
                        assertThat(reference.get().getExternalizableId()).isEqualTo(
                                referenceBuild.getExternalizableId()));
        verifyCodeDelta(action);
        verifyCoverage(action);
    }

    /**
     * Verifies the calculated coverage for the most important metrics line and branch coverage.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyCoverage(final CoverageBuildAction action) {
        verifyOverallCoverage(action);
        verifyModifiedLinesCoverage(action);
        verifyIndirectCoverageChanges(action);
    }

    /**
     * Verifies the calculated overall coverage including the coverage delta.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyOverallCoverage(final CoverageBuildAction action) {
        var builder = new CoverageBuilder();
        assertThat(action.getAllValues(Baseline.PROJECT)).contains(
                builder.setMetric(LINE).setCovered(529).setMissed(408).build(),
                builder.setMetric(BRANCH).setCovered(136).setMissed(94).build());
    }

    /**
     * Verifies the calculated modified lines coverage including the modified lines coverage delta.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyModifiedLinesCoverage(final CoverageBuildAction action) {
        var builder = new CoverageBuilder();
        assertThat(action.getAllValues(Baseline.MODIFIED_LINES)).contains(
                builder.setMetric(LINE).setCovered(1).setMissed(1).build());
    }

    /**
     * Verifies the calculated indirect coverage changes.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyIndirectCoverageChanges(final CoverageBuildAction action) {
        assertThat(action.getAllValues(Baseline.INDIRECT))
                .filteredOn(coverage -> coverage.getMetric().equals(LINE))
                .first()
                .isInstanceOfSatisfying(Coverage.class, coverage -> {
                    assertThat(coverage.getCovered()).isEqualTo(4);
                    assertThat(coverage.getMissed()).isEqualTo(0);
                });
        assertThat(action.getAllValues(Baseline.INDIRECT))
                .filteredOn(coverage -> coverage.getMetric().equals(BRANCH))
                .isEmpty();
    }

    private void verifyCodeDelta(final CoverageBuildAction action) {
        edu.hm.hafner.coverage.Node root = action.getResult();
        assertThat(root).isNotNull();

        List<FileNode> modifiedFiles = root.getAllFileNodes().stream()
                .filter(FileNode::hasModifiedLines)
                .collect(Collectors.toList());
        assertThat(modifiedFiles).hasSize(4);
        assertThat(modifiedFiles).extracting(FileNode::getName)
                .containsExactlyInAnyOrder("MinerFactory.java", "RepositoryMinerStep.java",
                        "SimpleReferenceRecorder.java", "CommitDecoratorFactory.java");
        assertThat(modifiedFiles).flatExtracting(FileNode::getModifiedLines)
                .containsExactlyInAnyOrder(15, 17, 63, 68, 80, 90, 130);
    }

    /**
     * Creates a {@link FlowDefinition} for a Jenkins pipeline which processes a JaCoCo coverage report.
     *
     * @param node
     *         The node
     * @param commit
     *         The processed commit
     * @param fileName
     *         The content of the processed JaCoCo report
     *
     * @return the created definition
     */
    private FlowDefinition createPipelineForCommit(final String node, final String commit, final String fileName) {
        return createPipelineForCommit(node, commit, fileName, SourceCodeRetention.EVERY_BUILD);
    }

    /**
     * Creates a {@link FlowDefinition} for a Jenkins pipeline which processes a JaCoCo coverage report.
     *
     * @param node
     *         The node
     * @param commit
     *         The processed commit
     * @param fileName
     *         The content of the processed JaCoCo report
     * @param sourceCodeRetentionStrategy
     *         the source code retention strategy
     *
     * @return the created definition
     */
    private FlowDefinition createPipelineForCommit(final String node, final String commit, final String fileName,
            final SourceCodeRetention sourceCodeRetentionStrategy) {
        return new CpsFlowDefinition(node + " {"
                + "    checkout([$class: 'GitSCM', "
                + "         branches: [[name: '" + commit + "' ]],\n"
                + "         userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
                + "         extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "             relativeTargetDir: 'checkout']]])\n"
                + "    recordCoverage tools: [[parser: 'JACOCO', pattern: '" + fileName + "']], "
                + "         sourceCodeRetention: '" + sourceCodeRetentionStrategy.name() + "'\n"
                + "}", true);
    }

    private void configureGit(final FreeStyleProject project, final String commit) throws IOException {
        GitSCM scm = new GitSCM(GitSCM.createRepoList(REPOSITORY, null),
                Collections.singletonList(new BranchSpec(commit)), null, null,
                Collections.singletonList(new RelativeTargetDirectory("code-coverage-api")));
        project.setScm(scm);
    }
}
