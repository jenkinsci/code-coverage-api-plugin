package io.jenkins.plugins.coverage.model;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import static io.jenkins.plugins.coverage.model.Assertions.*;

import io.jenkins.plugins.checks.api.ChecksAction;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;




/**
 * Integration test for the coverage API plugin.
 *
 * @author Thomas Willeit
 *
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    private static final String JACOCO_LESS_FILE_NAME = "jacoco-codingstyle.xml";
    private static final String COBERTURA_FILE_NAME = "cobertura.xml";
    private static final String COBERTURA_FILE_NAME_2 = "cobertura2.xml";


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void CoveragePluginPipeline_0_JacocoInputFile(){
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
    public void CoveragePluginPipeline_1_JacocoInputFile(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('jacoco-analysis-model.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368-6083));
    }

    @Test
    public void CoveragePluginPipeline_2_JacocoInputFile(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME, JACOCO_LESS_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(12166, 12736-12166));
    }



    @Test
    public void CoveragePluginPipeline_0_CoberturaInputFile(){
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
    public void CoveragePluginPipeline_1_CoberturaInputFile(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_NAME);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('cobertura.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(2, 0));
    }

    @Test
    public void CoveragePluginPipeline_2_CoberturaInputFile(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_FILE_NAME, COBERTURA_FILE_NAME_2);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(4, 0));
    }


    @Test
    public void CoveragePluginPipeline_1_CoberturaInputFile_1JacocoInputFile() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME, COBERTURA_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml'),jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6085, 6370-6085));
        j.assertLogContains("A total of 2 reports were found", build);
    }

    @Test
    public void CoveragePluginPipelineFailUnhealthy_Result_Failure(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 96.0, unstableThreshold: 90.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);

        //HealthReportingAction result = build.getAction(HealthReportingAction.class);
        //assertThat(result.getBuildHealth()).isEqualTo(null); //healthReport is null why???
        //TODO: assertThat(coverageResult.getHealthReport().getScore()).isEqualTo(96);  null-pointer-exception... why???
    }

    @Test
    public void CoveragePluginPipelineFailUnhealthy_Result_Unstable(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    public void CoveragePluginPipelineFailUnhealthy_Result_Success(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    public void CoveragePluginPipelineFailUnstable(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failUnstable: true,"
                + "globalThresholds: [[thresholdTarget: 'Line', unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    //fail if 0 reports (vs. ok???)

    public void CoveragePluginPipelineFailNoReports(){
        WorkflowJob job = createPipelineWithWorkspaceFiles();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
    public  void CoveragePluginPipelineQualityGates(){
        //TODO: Wie testet man die Quality Gates???
    }


    @Test
    public void CoveragePluginPipelineFailDecreasingCoverage(){
        WorkflowJob job1 = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job1.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));


        Run<?, ?> build1 = buildWithResult(job1, Result.SUCCESS);
        assertThat(build1.getNumber()).isEqualTo(1);


        WorkflowJob job2 = createPipelineWithWorkspaceFiles(JACOCO_LESS_FILE_NAME);
        job2.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                //+ "sourceFileResolver: sourceFiles('STORE_LAST_BUILD'),"
                + "failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));

        Run<?, ?> build2 = buildWithResult(job2, Result.FAILURE);
        assertThat(build2.getNumber()).isEqualTo(1);

    }

    @Test
    public  void CoveragePluginPipelineSkipPublishingChecks(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "skipPublishingChecks: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368-6083));

        //TODO: Wie assertet man publish checks???
    }

    //TODO: Source Rendering and copying, Delta Computation vs. reference Build, Report aggregation, Agent in Docker

}
