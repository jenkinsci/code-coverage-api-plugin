package io.jenkins.plugins.coverage.metrics.steps;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.metrics.AbstractCoverageITest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.CoverageParser;

import static edu.hm.hafner.metric.Metric.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests the integration of the Forensics API Plugin while using its Git implementation.
 *
 * @author Florian Orendi
 */
@Testcontainers(disabledWithoutDocker = true)
class GitForensicsITest extends AbstractCoverageITest {
    /**
     * The JaCoCo coverage report, generated for the commit {@link #COMMIT}.
     */
    private static final String JACOCO_FILE = "forensics_integration.xml";
    /**
     * The JaCoCo coverage report, generated for the reference commit {@link #COMMIT_REFERENCE}.
     */
    private static final String JACOCO_REFERENCE_FILE = "forensics_integration_reference.xml";

    private static final String COMMIT = "518eebd7cf42e1bf66cea966328c1b8f22183920";
    private static final String COMMIT_REFERENCE = "fd43cd0eab09d6a96fd21880d228251838a4355a";

    private static final String REPOSITORY = "https://github.com/jenkinsci/forensics-api-plugin.git";

    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer();

    @Test
    void shouldIntegrateForensicsPluginInPipelineOnDockerAgent() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        String node = "node('" + DOCKER_AGENT_NAME + "')";
        WorkflowJob project = createPipeline();
        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);

        project.setDefinition(createPipelineForCommit(node, COMMIT_REFERENCE, JACOCO_REFERENCE_FILE));
        Run<?, ?> referenceBuild = buildSuccessfully(project);
        verifyGitRepositoryForCommit(referenceBuild, COMMIT_REFERENCE);

        project.setDefinition(createPipelineForCommit(node, COMMIT, JACOCO_FILE));
        Run<?, ?> build = buildSuccessfully(project);
        verifyGitRepositoryForCommit(build, COMMIT);

        verifyGitIntegration(build, referenceBuild);
    }

    @Test
    void shouldIntegrateForensicsPluginInFreestyleJobOnAgent() throws IOException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        FreeStyleProject project = createFreestyleJob(CoverageParser.JACOCO);
        project.setAssignedNode(agent);

        configureGit(project, COMMIT_REFERENCE);
        addCoverageRecorder(project, CoverageParser.JACOCO, JACOCO_REFERENCE_FILE);

        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);

        Run<?, ?> referenceBuild = buildSuccessfully(project);

        configureGit(project, COMMIT);
        addCoverageRecorder(project, CoverageParser.JACOCO, JACOCO_FILE);

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
                .contains("remote.origin.url " + REPOSITORY)
                .contains("Checking out Revision " + commit)
                .contains("checkout -f " + commit);
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
        verifyChangeCoverage(action);
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
     * Verifies the calculated change coverage including the change coverage delta.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyChangeCoverage(final CoverageBuildAction action) {
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
        edu.hm.hafner.metric.Node root = action.getResult();
        assertThat(root).isNotNull();

        List<FileNode> changedFiles = root.getAllFileNodes().stream()
                .filter(FileNode::hasChangedLines)
                .collect(Collectors.toList());
        assertThat(changedFiles).hasSize(4);
        assertThat(changedFiles).extracting(FileNode::getName)
                .containsExactlyInAnyOrder("MinerFactory.java", "RepositoryMinerStep.java",
                        "SimpleReferenceRecorder.java", "CommitDecoratorFactory.java");
        assertThat(changedFiles).flatExtracting(FileNode::getChangedLines)
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
        return new CpsFlowDefinition(node + " {"
                + "    checkout([$class: 'GitSCM', "
                + "         branches: [[name: '" + commit + "' ]],\n"
                + "         userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
                + "         extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "             relativeTargetDir: 'checkout']]])\n"
                + "    recordCoverage tools: [[parser: 'JACOCO', pattern: '" + fileName + "']]\n"
                + "}", true);
    }

    /**
     * Adds a {@link CoveragePublisher} to the passed {@link FreeStyleProject project} which uses the passed JaCoCo XML
     * report.
     *
     * @param project
     *         The Jenkins project
     * @param jacocoXML
     *         The content of the generated JaCoCo coverage report as XML
     *
     * @throws IOException
     *         if removing currently existing coverage publishers failed
     */
    private void setCoveragePublisherForFreeStyleProject(final FreeStyleProject project, final String jacocoXML)
            throws IOException {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(jacocoXML);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().removeAll(CoveragePublisher.class);
        project.getPublishersList().add(coveragePublisher);
    }

    /**
     * Adds a {@link GitSCM} to the passed {@link FreeStyleProject project} which represents the passed commit.
     *
     * @param project
     *         The Jenkins project
     * @param commit
     *         The ID of the commit to be represented
     */
    private void configureGit(final FreeStyleProject project, final String commit)
            throws IOException {
        GitSCM scm = new GitSCM(GitSCM.createRepoList(REPOSITORY, null),
                Collections.singletonList(new BranchSpec(commit)), null, null,
                Collections.singletonList(new RelativeTargetDirectory("code-coverage-api")));
        project.setScm(scm);
    }
}
