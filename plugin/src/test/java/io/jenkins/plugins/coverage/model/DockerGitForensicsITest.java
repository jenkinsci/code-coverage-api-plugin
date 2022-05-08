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
import hudson.model.Run;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static io.jenkins.plugins.coverage.model.CoverageMetric.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests the integration of the Forensics API Plugin while using its Git implementation.
 *
 * @author Florian Orendi
 */
@Testcontainers(disabledWithoutDocker = true)
class DockerGitForensicsITest extends IntegrationTestWithJenkinsPerSuite {

    /**
     * The JaCoCo coverage report, generated for the commit {@link #COMMIT}.
     */
    private static final String JACOCO_FILE = "forensics_integration.xml";
    /**
     * The JaCoCo coverage report, generated for the reference commit {@link #COMMIT_REFERENCE}.
     */
    private static final String JACOCO_REFERENCE_FILE = "forensics_integration_reference.xml";

    private static final String COMMIT = "c18ee99a19717523f564f8d053e98c8512bdf9c5";
    private static final String COMMIT_REFERENCE = "c934ca8b6a73e1243ccd30ee25df79ebf4f644c7";

    private static final String REPOSITORY = "https://github.com/jenkinsci/analysis-model.git";

    @Container
    private static final AgentContainer AGENT_CONTAINER = new AgentContainer();

    @Test
    void shouldIntegrateForensicsPluginInPipelineOnDockerAgent() {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        String node = "node('" + DOCKER_AGENT_NAME + "')";

        WorkflowJob project = createPipeline();
        project.setDefinition(createFlowDefinitionForCommit(node, COMMIT_REFERENCE, JACOCO_REFERENCE_FILE));
        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);
        Run<?, ?> referenceBuild = buildSuccessfully(project);
        verifyGitRepositoryForCommit(referenceBuild, COMMIT_REFERENCE);

        project.setDefinition(createFlowDefinitionForCommit(node, COMMIT, JACOCO_FILE));
        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);
        Run<?, ?> build = buildSuccessfully(project);
        verifyGitRepositoryForCommit(build, COMMIT);

        verifyGitIntegration(build, referenceBuild);
    }

    @Test
    void shouldIntegrateForensicsPluginInFreestyleJobOnAgent() throws IOException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        Node agent = createDockerAgent(AGENT_CONTAINER);
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);

        copySingleFileToAgentWorkspace(agent, project, JACOCO_REFERENCE_FILE, JACOCO_REFERENCE_FILE);
        setGitScmWithCommitSpecForFreeStyleProject(project, COMMIT_REFERENCE);
        addCoveragePublisherForFreeStyleProject(project, JACOCO_REFERENCE_FILE);
        Run<?, ?> referenceBuild = buildSuccessfully(project);

        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE, JACOCO_FILE);
        setGitScmWithCommitSpecForFreeStyleProject(project, COMMIT);
        addCoveragePublisherForFreeStyleProject(project, JACOCO_FILE);
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
                .contains("git config remote.origin.url " + REPOSITORY)
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
            assertThat(coverage.getCovered()).isEqualTo(6107);
            assertThat(coverage.getMissed()).isEqualTo(286);
        });
        assertThat(action.getBranchCoverage()).satisfies(coverage -> {
            assertThat(coverage.getCovered()).isEqualTo(1673);
            assertThat(coverage.getMissed()).isEqualTo(214);
        });
        assertThat(action.getDifference()).containsExactly(
                new SimpleEntry<>(LINE, CoveragePercentage.getCoveragePercentage(-1_460_300, 40_768_161)),
                new SimpleEntry<>(BRANCH, CoveragePercentage.getCoveragePercentage(171_200, 3_545_673))
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
            assertThat(coverage.getCovered()).isEqualTo(19);
            assertThat(coverage.getMissed()).isEqualTo(4);
        });
        assertThat(action.getChangeCoverage(BRANCH)).satisfies(coverage -> {
            assertThat(coverage.getCovered()).isEqualTo(13);
            assertThat(coverage.getMissed()).isEqualTo(1);
        });
        assertThat(action.getChangeCoverageDifference(LINE)).satisfies(coverage -> {
            assertThat(coverage.getNumerator()).isEqualTo(1900);
            assertThat(coverage.getDenominator()).isEqualTo(23);
        });
        assertThat(action.getChangeCoverageDifference(BRANCH)).satisfies(coverage -> {
            assertThat(coverage.getNumerator()).isEqualTo(650);
            assertThat(coverage.getDenominator()).isEqualTo(7);
        });
    }

    /**
     * Verifies the calculated indirect coverage changes.
     *
     * @param action
     *         The created Jenkins action
     */
    private void verifyIndirectCoverageChanges(final CoverageBuildAction action) {
        assertThat(action.getIndirectCoverageChanges(LINE)).satisfies(coverage -> {
            assertThat(coverage.getCovered()).isEqualTo(-1_917_800);
            assertThat(coverage.getMissed()).isEqualTo(147_039);
        });
        assertThat(action.getIndirectCoverageChanges(BRANCH)).satisfies(coverage -> {
            assertThat(coverage.getCovered()).isEqualTo(54_050);
            assertThat(coverage.getMissed()).isEqualTo(13_209);
        });
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
        assertThat(changedFiles).hasSize(1);

        FileCoverageNode changedFile = changedFiles.get(0);
        assertThat(changedFile.getChangedCodeLines()).containsExactly(
                7, 27, 37, 38, 49, 68, 69, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98,
                99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117,
                118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136,
                137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 152, 154, 155
        );
        assertThat(changedFile.getFileAmountWithChangedCoverage()).isEqualTo(1);
        assertThat(changedFile.getFileAmountWithIndirectCoverageChanges()).isEqualTo(1);
        assertThat(changedFile.getLineAmountWithChangedCoverage()).isEqualTo(23);
        assertThat(changedFile.getLineAmountWithIndirectCoverageChanges()).isEqualTo(2);
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
        return new CpsFlowDefinition(node + " {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + commit + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
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
     */
    private void addCoveragePublisherForFreeStyleProject(final FreeStyleProject project, final String jacocoXML) {
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(jacocoXML);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
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
                Collections.singletonList(new RelativeTargetDirectory("checkout")));
        project.setScm(scm);
    }
}
