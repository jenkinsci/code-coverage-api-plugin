package io.jenkins.plugins.coverage.model;

import java.util.Collections;
import java.util.List;

import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.test.acceptance.docker.DockerContainer;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import static org.assertj.core.api.Assertions.*;

/**
 * Test util for integration tests.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class CoveragePluginITestUtil {

    /**
     * Jacoco analysis model example file.
     */
    public static final String JACOCO_ANALYSIS_MODEL_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Jacoco coding style example file.
     */
    public static final String JACOCO_CODING_STYLE_FILE_NAME = "jacoco-codingstyle.xml";

    /**
     * Jacoco coding style example file with decreased line coverage.
     */
    public static final String JACOCO_CODING_STYLE_DECREASED_LINE_COVERAGE_FILE_NAME = "jacoco-codingstyle-decreased-line-coverage.xml";

    /**
     * Cobertura example file with lots of data.
     */
    public static final String COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME = "coverage-with-lots-of-data.xml";

    /**
     * Cobertura example file.
     */
    public static final String COBERTURA_COVERAGE_FILE_NAME = "cobertura-coverage.xml";

    /**
     * The number of total lines for file {@link #JACOCO_ANALYSIS_MODEL_FILE_NAME}.
     */
    public static final int JACOCO_ANALYSIS_MODEL_LINES_TOTAL = 6368;

    /**
     * The number of covered lines for file {@link #JACOCO_ANALYSIS_MODEL_FILE_NAME}.
     */
    public static final int JACOCO_ANALYSIS_MODEL_LINES_COVERED = 6083;

    /**
     * The number of total lines for file {@link #JACOCO_CODING_STYLE_FILE_NAME}.
     */
    public static final int JACOCO_CODING_STYLE_LINES_TOTAL = 323;

    /**
     * The number of covered lines for file {@link #JACOCO_CODING_STYLE_FILE_NAME}.
     */
    public static final int JACOCO_CODING_STYLE_LINES_COVERED = 294;

    /**
     * The number of covered lines for file {@link #COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED = 602;

    /**
     * The number of total lines for file {@link #COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL = 958;

    /**
     * The number of covered lines for file {@link #COBERTURA_COVERAGE_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_LINES_COVERED = 2;

    /**
     * The number of total lines for file {@link #COBERTURA_COVERAGE_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_LINES_TOTAL = 2;

    /**
     * Private default constructor avoiding public default constructor.
     */
    private CoveragePluginITestUtil() {

    }

    /**
     * Takes a build and asserts its line coverage.
     *
     * @param totalLines
     *         the numbers of total lines of the files processed within the build. The order is important and should
     *         match to {@code coveredLines}
     * @param coveredLines
     *         the numbers of covered lines of the files processed within the build. The order is important and should
     *         match to {@code totalLines}
     * @param build
     *         the processed build
     */
    public static void assertLineCoverageResultsOfBuild(final List<Integer> totalLines,
            final List<Integer> coveredLines,
            final Run<?, ?> build) {
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        int totalCoveredLines = coveredLines.stream().mapToInt(x -> x).sum();
        int totalMissedLines =
                totalLines.stream().mapToInt(x -> x).sum() - coveredLines.stream().mapToInt(x -> x).sum();
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(
                        totalCoveredLines,
                        totalMissedLines
                ));
    }

    /**
     * Connects a docker container and provides its docker agent.
     *
     * @param dockerContainer
     *         a docker container to be connected at
     * @param jenkinsRule
     *         the jenkins rule to which the docker container should be applied at
     *
     * @return a started and connected docker agent
     * @throws Exception
     *         thrown by {@link DumbSlave}
     */
    public static DumbSlave createDockerContainerAgent(final DockerContainer dockerContainer, final JenkinsRule jenkinsRule)
            throws Exception {
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
        jenkinsRule.jenkins.addNode(agent);
        jenkinsRule.waitOnline(agent);
        return agent;
    }
}
