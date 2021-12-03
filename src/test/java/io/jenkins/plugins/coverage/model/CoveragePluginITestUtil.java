package io.jenkins.plugins.coverage.model;

import java.util.Collections;
import java.util.List;

import org.junit.AssumptionViolatedException;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class CoveragePluginITestUtil {

    public static final String JACOCO_ANALYSIS_MODEL_FILE_NAME = "jacoco-analysis-model.xml";
    public static final String JACOCO_CODING_STYLE_FILE_NAME = "jacoco-codingstyle.xml";
    public static final String JACOCO_CODING_STYLE_DECREASED_FILE_NAME = "jacoco-codingstyle-decreased-line-coverage.xml";
    public static final String COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME = "coverage-with-lots-of-data.xml";
    public static final String COBERTURA_COVERAGE_FILE_NAME = "cobertura-coverage.xml";

    public static final int JACOCO_ANALYSIS_MODEL_LINES_TOTAL = 6368;
    public static final int JACOCO_ANALYSIS_MODEL_LINES_COVERED = 6083;
    public static final int JACOCO_CODING_STYLE_LINES_TOTAL = 323;
    public static final int JACOCO_CODING_STYLE_LINES_COVERED = 294;
    public static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED = 602;
    public static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL = 958;

    public static final int COBERTURA_COVERAGE_LINES_COVERED = 2;
    public static final int COBERTURA_COVERAGE_LINES_TOTAL = 2;

    public static void assertLineCoverageResultsOfBuild(List<Integer> totalLines, List<Integer> coveredLines,
            Run<?, ?> build) {
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

    public static void verifySimpleCoverageNode(Run<?, ?> build, int assertCoveredLines,
            int assertMissedLines) {
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(assertCoveredLines, assertMissedLines));
    }



}
