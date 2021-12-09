package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Collections;

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
import org.jenkinsci.test.acceptance.docker.fixtures.JavaGitContainer;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the coverage API plugin.
 *
 *  todo: DRY, constants for values, javadoc
 *
 * @author Thomas Willeit
 *
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    /** Docker container for java-maven builds. Contains also git to check out from an SCM. */
    @Rule
    public DockerRule<JavaGitContainer> javaDockerRule = new DockerRule<>(JavaGitContainer.class);

    private static final String JACOCO_FILE_WITH_HIGHER_COVERAGE = "jacoco-analysis-model.xml";
    private static final String JACOCO_LESS_WITH_LESS_COVERAGE = "jacoco-codingstyle.xml";
    private static final String COBERTURA_FILE_ONE = "cobertura.xml";
    private static final String COBERTURA_FILE_TWO = "cobertura2.xml";


    @Test
    public void coveragePluginPipelineZeroJacocoInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isEqualTo(null);
    }


    @Test
    public void coveragePluginPipelineOneJacocoInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('jacoco-analysis-model.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));
    }


    @Test
    public void coveragePluginPipelineTwoJacocoInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE,
                JACOCO_LESS_WITH_LESS_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6377, 6691 - 6377));
    }


    @Test
    public void coveragePluginPipelineZeroCoberturaInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles();

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('cobertura.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult).isEqualTo(null);
    }


    @Test
    public void coveragePluginPipelineOneCoberturaInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_ONE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('cobertura.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(2, 0));
    }

    @Test
    public void coveragePluginPipelineTwoCoberturaInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_ONE, COBERTURA_FILE_TWO);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(4, 0));
    }


    @Test
    public void coveragePluginPipelineOneCoberturaInputFileOneJacocoInputFile() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE, COBERTURA_FILE_ONE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('" + COBERTURA_FILE_ONE + "'),jacocoAdapter('" + JACOCO_FILE_WITH_HIGHER_COVERAGE + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6085, 6370 - 6085));
        CoveragePluginITest.JENKINS_PER_SUITE.assertLogContains("A total of 2 reports were found", build);
    }


    @Test
    public void coveragePluginPipelineFailUnhealthyWithResultFailure() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 96.0, unstableThreshold: 50.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(0);
    }


    @Test
    public void coveragePluginPipelineFailUnhealthyWithResultFailureContainsBUG() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 96.0, unstableThreshold: 90.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(96);

        //HealthReportingAction result = build.getAction(HealthReportingAction.class);
        //assertThat(result.getBuildHealth()).isEqualTo(null); //healthReport is null why???
    }


    @Test
    public void coveragePluginPipelineFailUnhealthyWithResultUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void coveragePluginPipelineFailUnhealthyWithResultSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void coveragePluginPipelineFailUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failUnstable: true,"
                + "globalThresholds: [[thresholdTarget: 'Line', unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void coveragePluginPipelineFailNoReports() {
        WorkflowJob job = createPipelineWithWorkspaceFiles();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void coveragePluginPipelineGetDelta() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_LESS_WITH_LESS_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_LESS_WITH_LESS_COVERAGE
                + "')]"
                + "}", true));

        Run<?, ?> firstBuild = buildSuccessfully(job);
        assertThat(firstBuild.getNumber()).isEqualTo(1);

        copyFilesToWorkspace(job, JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('" + JACOCO_FILE_WITH_HIGHER_COVERAGE + "')]\n"
                + "discoverReferenceBuild(referenceJob: '" + job.getName() + "')"
                + "}", true));

        Run<?, ?> secondBuild = buildSuccessfully(job);

        CoverageBuildAction coverageResult = secondBuild.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("+0.045");
    }


    @Test
    public void coveragePluginPipelineFailDecreasingCoverage() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> firstBuild = buildSuccessfully(job);
        assertThat(firstBuild.getNumber()).isEqualTo(1);

        cleanWorkspace(job);
        copyFilesToWorkspace(job, JACOCO_LESS_WITH_LESS_COVERAGE);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "   failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));


        Run<?, ?> secondBuild = buildWithResult(job,Result.FAILURE);
        assertThat(secondBuild.getNumber()).isEqualTo(2);
    }


    @Test
    public void coveragePluginPipelineSkipPublishingChecks() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "skipPublishingChecks: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));

        CoveragePluginITest.JENKINS_PER_SUITE.assertLogNotContains("[Checks API] No suitable checks publisher found.", build);
    }


    @Test
    public void coveragePluginPipelinePublishingChecks() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));

        CoveragePluginITest.JENKINS_PER_SUITE.assertLogContains("[Checks API] No suitable checks publisher found.", build);
    }


    @Test
    public void coveragePluginPipelineReportAggregationTrue() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE, JACOCO_LESS_WITH_LESS_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

        CoveragePluginITest.JENKINS_PER_SUITE.assertLogContains("A total of 1 reports were found", build);
    }


    @Test
    /**
     * Test ignored ... falsche Imports?
     * java.lang.IllegalArgumentException: Unknown server host key algorithm 'ssh-ed25519'
     */
    public void  coveragePluginFreestyleProjectDockerTest() throws IOException, InterruptedException {
        DumbSlave agent = createDockerContainerAgent(javaDockerRule.get());
        FreeStyleProject project = createFreeStyleProject();
        project.setAssignedNode(agent);

        copySingleFileToAgentWorkspace(agent, project, JACOCO_FILE_WITH_HIGHER_COVERAGE, JACOCO_FILE_WITH_HIGHER_COVERAGE);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ? > build = buildSuccessfully(project);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));
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
                    new EnvironmentVariablesNodeProperty.Entry("JAVA_HOME", "/usr/lib/jvm/java-8-openjdk-amd64/jre"))));
            getJenkins().jenkins.addNode(agent);
            getJenkins().waitOnline(agent);

            return agent;
        }
        catch (Throwable e) {
            throw new AssumptionViolatedException("Failed to create docker container", e);
        }
    }
}
