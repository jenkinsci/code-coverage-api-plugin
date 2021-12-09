package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import org.junit.Test;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.coverage.CoverageProcessor;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import static io.jenkins.plugins.coverage.model.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the coverage API plugin.
 *
 * @author Thomas Willeit
 *
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_WITH_HIGHER_COVERAGE = "jacoco-analysis-model.xml";
    private static final String JACOCO_LESS_WITH_LESS_COVERAGE = "jacoco-codingstyle.xml";
    private static final String COBERTURA_FILE_ONE = "cobertura.xml";
    private static final String COBERTURA_FILE_TWO = "cobertura2.xml";


    @Test
    public void CoveragePluginPipelineZeroJacocoInputFile() {
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
    public void CoveragePluginPipelineOneJacocoInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('jacoco-analysis-model.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368-6083));
    }


    @Test
    public void CoveragePluginPipelineTwoJacocoInputFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE,
                JACOCO_LESS_WITH_LESS_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6377, 6691-6377));
    }


    @Test
    public void CoveragePluginPipelineZeroCoberturaInputFile() {
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
    public void CoveragePluginPipelineOneCoberturaInputFile() {
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
    public void CoveragePluginPipelineTwoCoberturaInputFile() {
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
    public void CoveragePluginPipelineOneCoberturaInputFileOneJacocoInputFile() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE, COBERTURA_FILE_ONE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('" + COBERTURA_FILE_ONE + "'),jacocoAdapter('" + JACOCO_FILE_WITH_HIGHER_COVERAGE + "')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6085, 6370-6085));
        CoveragePluginITest.JENKINS_PER_SUITE.assertLogContains("A total of 2 reports were found", build);
    }


    @Test
    public void CoveragePluginPipelineFailUnhealthyWithResultFailure() {
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
    public void CoveragePluginPipelineFailUnhealthyWithResultFailureContainsBUG() {
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
    public void CoveragePluginPipelineFailUnhealthyWithResultUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void CoveragePluginPipelineFailUnhealthyWithResultSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void CoveragePluginPipelineFailUnstable() {
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
    public void CoveragePluginPipelineFailNoReports() {
        WorkflowJob job = createPipelineWithWorkspaceFiles();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public void CoveragePluginPipelineGetDelta() {
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
    public void CoveragePluginPipelineFailDecreasingCoverage() {
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
    public  void CoveragePluginPipelineSkipPublishingChecks() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "skipPublishingChecks: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368-6083));

        CoveragePluginITest.JENKINS_PER_SUITE.assertLogNotContains("[Checks API] No suitable checks publisher found.", build);
    }


    @Test
    public  void CoveragePluginPipelinePublishingChecks() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368-6083));

        CoveragePluginITest.JENKINS_PER_SUITE.assertLogContains("[Checks API] No suitable checks publisher found.", build);
    }


    @Test
    public void reportAggregationTrue() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_WITH_HIGHER_COVERAGE, JACOCO_LESS_WITH_LESS_COVERAGE);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);

        CoveragePluginITest.JENKINS_PER_SUITE.assertLogContains("A total of 1 reports were found", build);
    }
}
