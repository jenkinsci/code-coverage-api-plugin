package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;
import java.util.TreeMap;

import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

/**
 * Tests if source code Rendering and copying works with Docker and if freestyle-projects run successfully with Docker.
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class DockerAndSourceCodeRenderingITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_ANALYSIS_MODEL_FILE = "jacoco-analysis-model.xml";
    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";
    private static final String REPOSITORY = "https://github.com/jenkinsci/analysis-model.git";

    /** Docker container for java-maven builds. Contains also git to check out from an SCM. */
    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    /**
     * Integration test for a pipeline with sourcecode that runs on an agent.
     */
    @Test
    public void copySourceCodeAndCodeRenderingPipelineOnAgent() throws IOException, InterruptedException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        WorkflowJob project = createPipelineWithSCMandJacocoAdapter("node('docker')");

        copySingleFileToAgentWorkspace(agent, project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_ANALYSIS_MODEL_FILE);

        Run<?, ?> build = verifyGitRepository(project);

        verifySourceCode(build);
    }

    /**
     * Integration test for a pipeline with sourcecode.
     */
    @Test
    public void copySourceCodeAndCodeRenderingPipeline() {
        WorkflowJob job = createPipelineWithSCMandJacocoAdapter("node");
        Run<?, ?> build = verifyGitRepository(job);

        verifySourceCode(build);
    }

    /**
     * Verifies sourcecode is present in project.
     *
     * @param build
     *         job of the project
     */
    private void verifySourceCode(final Run<?, ?> build) {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "top-level");

        CoverageBuildAction action = new CoverageBuildAction(build, root, new HealthReport(), "-",
                new TreeMap<>(), false);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getNode).isEqualTo(root);
        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isEqualTo(build);
    }

    /**
     * Verifies clone of repository by checking console log.
     *
     * @param workflowJob
     *         job of the project
     *
     * @return build of {@link WorkflowJob}
     */
    private Run<?, ?> verifyGitRepository(final WorkflowJob workflowJob) {
        Run<?, ?> build = buildSuccessfully(workflowJob);

        String consoleLog = getConsoleLog(build);

        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("checkout -f " + COMMIT);
        return build;
    }

    /**
     * Creates a docker container agent.
     *
     * @param dockerContainer
     *         the docker container of the agent
     *
     * @return A docker container agent.
     */
    @SuppressWarnings({"PMD.AvoidCatchingThrowable", "IllegalCatch"})
    protected DumbSlave createDockerContainerAgent(final DockerContainer dockerContainer) {
        try {
            SystemCredentialsProvider.getInstance().getDomainCredentialsMap().put(Domain.global(),
                    Collections.singletonList(
                            new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "dummyCredentialId",
                                    null, "test", "test")
                    )
            );
            DumbSlave agent = new DumbSlave("docker", "/home/test",
                    new SSHLauncher(dockerContainer.ipBound(22), dockerContainer.port(22), "dummyCredentialId"));
            agent.setNodeProperties(Collections.singletonList(new EnvironmentVariablesNodeProperty(
                    new Entry("JAVA_HOME", "/usr/lib/jvm/java-8-openjdk-amd64/jre"))));
            getJenkins().jenkins.addNode(agent);
            getJenkins().waitOnline(agent);

            return agent;
        }
        catch (Throwable e) {
            throw new AssumptionViolatedException("Failed to create docker container", e);
        }
    }

    private WorkflowJob createPipelineWithSCMandJacocoAdapter(final String node) {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition(node + " {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + COMMIT + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_ANALYSIS_MODEL_FILE
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}", true));

        return job;
    }

    /**
     * Tests if freestyle project is running successfully in docker.
     *
     * @throws IOException
     *         due to javaDockerRule.get()
     * @throws InterruptedException
     *         to setAssignedNode() to project
     */
    @Test
    public void freestyleProjectCoverageOnAgentNode() throws IOException, InterruptedException {
        assumeThat(isWindows()).as("Running on Windows").isFalse();

        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);
        copySingleFileToAgentWorkspace(agent, project, JACOCO_ANALYSIS_MODEL_FILE, JACOCO_ANALYSIS_MODEL_FILE);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
    }
}
