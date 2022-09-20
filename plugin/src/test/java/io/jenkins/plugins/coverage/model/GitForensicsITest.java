package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static io.jenkins.plugins.coverage.model.CoverageMetric.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests the integration of the Forensics API Plugin while using its Git implementation.
 *
 * @author Florian Orendi
 */
@Testcontainers(disabledWithoutDocker = true)
class GitForensicsITest extends IntegrationTestWithJenkinsPerSuite {

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

        project.setDefinition(createFlowDefinitionForCommit(node, COMMIT_REFERENCE, JACOCO_REFERENCE_FILE));
        Run<?, ?> referenceBuild = buildSuccessfully(project);
        verifyGitRepositoryForCommit(referenceBuild, COMMIT_REFERENCE);

        project.setDefinition(createFlowDefinitionForCommit(node, COMMIT, JACOCO_FILE));
        Run<?, ?> build = buildSuccessfully(project);
        verifyGitRepositoryForCommit(build, COMMIT);

        verifyGitIntegration(build, referenceBuild);
    }

    // See https://github.com/jenkinsci/code-coverage-api-plugin/issues/467
    @Test
    void tryToExpose467() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        String node = "node('" + DOCKER_AGENT_NAME + "')";
        WorkflowJob project = createPipeline();
        // jacoco file of commit 723aac8 -> the reference build
        String jacocoReferenceFile = "jacoco-duplicate-key-reference.xml";
        copySingleFileToAgentWorkspace(agent, project, jacocoReferenceFile, jacocoReferenceFile);
        // jacoco file of commit 2796bb8 -> the current build
        String jacocoFile = "jacoco-duplicate-key.xml";
        copySingleFileToAgentWorkspace(agent, project, jacocoFile, jacocoFile);

        // the commit id of the reference build - the master branch parent of the current commit
        String commitReference = "723aac821a5e967dd3894d215b267a38de086b90";
        project.setDefinition(createFlowDefinitionForCommit(node, commitReference, jacocoReferenceFile, "https://github.com/janfaracik/design-library-plugin.git"));
        Run<?, ?> referenceBuild = buildSuccessfully(project);
        System.out.println(getConsoleLog(referenceBuild));

        // the commit id of the second parent commit within the PR
        String commit = "3823eadf5aa0196f8f2d8d9a2abe29b6ced721bb";
        project.setDefinition(createFlowDefinitionForCommit(node, commit, jacocoFile, "https://github.com/janfaracik/design-library-plugin.git"));
        // the git delta in the issue is calculated between the parents, not between the merge commit and the master parent, what would be the expected behavior.
        // (this behavior is a guess adapted from a similar situation: https://github.com/jenkinsci/design-library-plugin/commit/708f674be9170c7a732400fb26bb7dc882185676) -> also see the Jenkins log of the build of this commit
        // -> the GitDeltaCalculator takes the latest commit for the current build to calculate the delta, not the merge commit itself which would be required here
        // -> reason: GitDeltaCalculator uses the function getLatestCommit() and ignores the targetCommit, if set, which represents the merge commit
        // -> TODO: possible solution in Git-Forensics-Plugin GitDeltaCalculator -> check for target commit, fallback with latest commit for current build
        Run<?, ?> build = buildWithResult(project, Result.FAILURE);
        assertThat(getConsoleLog(build))
                .contains("java.lang.IllegalStateException: Duplicate key io/jenkins/plugins/designlibrary/Conventions.java (attempted merging values [File] Conventions.java and [File] Layouts.java)");
    }

    @Test
    void shouldIntegrateForensicsPluginInFreestyleJobOnAgent() throws IOException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);

        setGitScmWithCommitSpecForFreeStyleProject(project, COMMIT_REFERENCE);
        setCoveragePublisherForFreeStyleProject(project, JACOCO_REFERENCE_FILE);
        Run<?, ?> referenceBuild = buildSuccessfully(project);

        setGitScmWithCommitSpecForFreeStyleProject(project, COMMIT);
        setCoveragePublisherForFreeStyleProject(project, JACOCO_FILE);
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
                .contains("git " + REPOSITORY)
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
        assertThat(action.getLineCoverage()).satisfies(coverage -> {
            assertThat(coverage).hasCovered(546);
            assertThat(coverage).hasMissed(461);
        });
        assertThat(action.getBranchCoverage()).satisfies(coverage -> {
            assertThat(coverage).hasCovered(136);
            assertThat(coverage).hasMissed(94);
        });
        assertThat(action.getDifference()).contains(
                new SimpleEntry<>(LINE, CoveragePercentage.valueOf(65_160, 103_721)),
                new SimpleEntry<>(BRANCH, CoveragePercentage.valueOf(0, 1))
        );
    }

    /**
     * Verifies the calculated change coverage including the change coverage delta.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyChangeCoverage(final CoverageBuildAction action) {
        assertThat(action.getChangeCoverage(LINE)).satisfies(coverage -> {
            assertThat(coverage).hasCovered(1);
            assertThat(coverage).hasMissed(1);
        });
        assertThat(action.getChangeCoverage(BRANCH)).satisfies(coverage -> {
            assertThat(coverage).hasCovered(0);
            assertThat(coverage).hasMissed(0);
        });
        assertThat(action.getChangeCoverageDifference(LINE)).satisfies(coverage -> {
            assertThat(coverage).hasNumerator(-4250);
            assertThat(coverage).hasDenominator(1007);
        });
        assertThat(action.hasChangeCoverageDifference(BRANCH)).isFalse();
    }

    /**
     * Verifies the calculated indirect coverage changes.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyIndirectCoverageChanges(final CoverageBuildAction action) {
        assertThat(action.getIndirectCoverageChanges(LINE)).satisfies(coverage -> {
            assertThat(coverage).hasCovered(4);
            assertThat(coverage).hasMissed(0);
        });
        assertThat(action.hasIndirectCoverageChanges(BRANCH)).isFalse();
    }

    /**
     * Verifies the calculated code delta.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyCodeDelta(final CoverageBuildAction action) {
        CoverageNode root = action.getResult();
        assertThat(root).isNotNull();

        List<FileCoverageNode> changedFiles = root.getAllFileCoverageNodes().stream()
                .filter(fileNode -> !fileNode.getChangedCodeLines().isEmpty())
                .collect(Collectors.toList());
        assertThat(changedFiles).hasSize(4);
        assertThat(changedFiles).extracting(FileCoverageNode::getName)
                .containsExactly("MinerFactory.java", "RepositoryMinerStep.java",
                        "SimpleReferenceRecorder.java", "CommitDecoratorFactory.java");
        assertThat(changedFiles).flatExtracting(FileCoverageNode::getChangedCodeLines)
                .containsExactlyInAnyOrder(15, 17, 63, 68, 80, 90, 130);

        assertThat(root).hasFileAmountWithChangedCoverage(2);
        assertThat(root).hasFileAmountWithIndirectCoverageChanges(1);
        assertThat(root).hasLineAmountWithChangedCoverage(2);
        assertThat(root).hasLineAmountWithIndirectCoverageChanges(4);
    }

    /**
     * Creates a {@link FlowDefinition} for a Jenkins pipeline which processes a JaCoCo coverage report.
     *
     * @param node
     *         The node
     * @param commit
     *         The processed commit
     * @param jacocoXML
     *         The content of the processed JaCoCo report
     *
     * @return the created definition
     */
    private FlowDefinition createFlowDefinitionForCommit(
            final String node, final String commit, final String jacocoXML) {
        return createFlowDefinitionForCommit(node, commit, jacocoXML, REPOSITORY);
    }

    /**
     * Creates a {@link FlowDefinition} for a Jenkins pipeline which processes a JaCoCo coverage report.
     *
     * @param node
     *         The node
     * @param commit
     *         The processed commit
     * @param jacocoXML
     *         The content of the processed JaCoCo report
     * @param repository
     *         the remote Git repository
     *
     * @return the created definition
     */
    private FlowDefinition createFlowDefinitionForCommit(
            final String node, final String commit, final String jacocoXML, final String repository) {
        return new CpsFlowDefinition(node + " {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + commit + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + repository + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + jacocoXML
                + "')], sourceFileResolver: sourceFiles('NEVER_STORE')\n"
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
    private void setGitScmWithCommitSpecForFreeStyleProject(final FreeStyleProject project, final String commit)
            throws IOException {
        GitSCM scm = new GitSCM(GitSCM.createRepoList(REPOSITORY, null),
                Collections.singletonList(new BranchSpec(commit)), null, null,
                Collections.singletonList(new RelativeTargetDirectory("code-coverage-api")));
        project.setScm(scm);
    }
}
