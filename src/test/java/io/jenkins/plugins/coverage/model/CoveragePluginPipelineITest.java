package io.jenkins.plugins.coverage.model;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.CoverageScriptedPipelineScriptBuilder;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/*
Pipeline integration tests for coverage api plugin

 */
public class CoveragePluginPipelineITest extends IntegrationTestWithJenkinsPerSuite {

    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";
    //private static final String COBERTURA_FILE_NAME = "../cobertura-coverage.xml";
    private static final String COBERTURA_FILE_NAME = "coverage-with-lots-of-data.xml";
    private static final String JACOCO_BIG_DATA = "jacoco-analysis-model.xml";
    private static final String JACOCO_SMALL_DATA = "jacoco.xml";
    private static final String JACOCO_MINI_DATA = "jacocoModifiedMini.xml";
    private static final String COBERTURA_SMALL_DATA = "cobertura-coverage.xml";
    private static final String COBERTURA_BIG_DATA = "coverage-with-lots-of-data.xml";

    private static final String COMMIT = "6bd346bbcc9779467ce657b2618ab11e38e28c2c";
    private static final String REPOSITORY = "https://github.com/jenkinsci/analysis-model.git";

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Test
    public void noJacocoFile() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();

        WorkflowJob job = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        job.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun run = Objects.requireNonNull(job.scheduleBuild2(0)).waitForStart();

        j.assertBuildStatusSuccess(j.waitForCompletion(run));
        j.assertLogContains("No reports were found", run);

    }

    @Test
    public void oneJacocoFile(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    @Test
    public void twoJacocoFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    @Test
    public void noCoberturaFile() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();
        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);

    }

    @Test
    public void oneCobertura(){
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_BIG_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(602, 958 - 602));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(285, 628 - 285));
    }

    @Test
    public void twoCoberturaFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_BIG_DATA, COBERTURA_SMALL_DATA);

        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(604, 960 - 604));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(285, 628 - 285));
    }

    @Test
    public void oneJacocoOneCobertura() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, COBERTURA_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [cobertura('**/*.xml'),jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6685, 7326 - 6685));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1946, 2503 - 1946));
    }

    @Test
    public void healthReportingHealthy(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 94.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
    }

    @Test
    public void healthReportingUnstable(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 90.0, unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.UNSTABLE);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
    }

   @Test
    public void healthReportingUnhealthySuccess(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "globalThresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 96.0]]"
                + "}", true));

       Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
       HealthReportingAction x = build.getAction(HealthReportingAction.class);

       assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
       assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
    }

    @Test
    public void healthReportingUnhealthyFailure(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_FILE_NAME);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failUnstable: true,"
                + "globalThresholds: [[thresholdTarget: 'Line', unstableThreshold: 96.0]]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }


    @Test
      public void failNoReportsTrue() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles();
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: true"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void failNoReportsFalse() throws IOException {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles();
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failNoReports: false"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("No reports were found");
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    private String getLogFromInputStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Test
    public void qualityGatesGlobalThresholdFailUnhealthy() throws IOException {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 100.0]])]}"
                , true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);

        assertThat(getLogFromInputStream(build.getLogInputStream())).contains("Build failed", "Line");
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void qualityGatesGlobalThresholdSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 42.0]])]}"
                , true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }

    @Test
    public void qualityGatesGlobalThresholdUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        job.setDefinition(new CpsFlowDefinition(
                "node {"
                        + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', "
                        + "thresholds: [[failUnhealthy: false, thresholdTarget: 'Line', unhealthyThreshold: 99.0]])]}"
                , true));

        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        HealthReportingAction x = build.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(0);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(6083, 6368 - 6083));
        assertThat(coverageResult.getBranchCoverage())
                .isEqualTo(new Coverage(1661, 1875 - 1661));
    }








    //FailIfCoverage Decreases true
    @Test
    public void failDecreasingCoverageTrue(){
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));


        Run<?, ?> build1 = buildWithResult(workflowJob, Result.SUCCESS);
        assertThat(build1.getNumber()).isEqualTo(1);


        WorkflowJob workflowJob2 = createPipelineWithWorkspaceFiles(JACOCO_MINI_DATA);
        workflowJob2.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                //+ "sourceFileResolver: sourceFiles('STORE_LAST_BUILD'),"
                + "failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));

        Run<?, ?> build2 = buildWithResult(workflowJob2, Result.SUCCESS);
        assertThat(build2.getNumber()).isEqualTo(1);

    }
    //FailIfCoverage Decreases false
    @Test
    public void failDecreasingCoverageFalse() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", false));


        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
        assertThat(build.getNumber()).isEqualTo(1);


        WorkflowJob workflowJob2 = createPipelineWithWorkspaceFiles(JACOCO_MINI_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                + "failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));

        Run<?, ?> build2 = buildWithResult(workflowJob2, Result.FAILURE);
        assertThat(build2.getNumber()).isEqualTo(1);
    }


        @Test
        public void skipPublishingChecksTrue () throws IOException {
            WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
            workflowJob.setDefinition(new CpsFlowDefinition("node {"
                    + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                    + "skipPublishingChecks: true"
                    + "}", true));

            Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
            assertThat(build.getNumber()).isEqualTo(1);

            CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
            assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));
            j.assertLogContains("No suitable checks publisher found", build);
        }

        @Test
        public void skipPublishingChecksFalse() throws IOException {
            WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
            workflowJob.setDefinition(new CpsFlowDefinition("node {"
                    + "publishCoverage adapters: [jacocoAdapter('**/*.xml')],"
                    + "skipPublishingChecks: false"
                    + "}", true));

            Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);
            assertThat(build.getNumber()).isEqualTo(1);

            CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
            assertThat(coverageResult.getLineCoverage()).isEqualTo(new Coverage(6083, 6368 - 6083));
            j.assertLogNotContains("No suitable checks publisher found", build);
        }



    @Test
    public void deltaComputation(){

    }

    @Test
    public void deltaComputationZeroDelta(){}

    @Test
    public void deltaComputationSingleBuild(){

    }

    @Test
    public void deltaComputationUseOnlyPreviousAndCurrent(){}


    @Test
    public void referenceBuildSingleBuild() {


    }

    @Test
    public void referenceBuildReferenceIsPrevious() {



    }

    @Test
    public void reportAggregation() throws IOException {

    }

    @Test
    public void reportAggregationFalse() throws IOException {

    }






    // onAgentNode
// Agent in Docker
    @Test
    public void agentOnDocker(){


        WorkflowJob workflowJob = createPipeline();
        workflowJob.setDefinition(new CpsFlowDefinition("node ('docker' {"
                + "timestamps {\n"
                + "checkout([$class: 'GitSCM',"
                + "branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c']], \n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                +               "relativeTagertDir: 'checkout']]]), \n"
                +   "publishCoverage adapters: [jacocoAdapters('" + JACOCO_BIG_DATA +"')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}"
                + "}", true));

    }
    @Test
    public void copyTest() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "    checkout([$class: 'GitSCM', "
                + "branches: [[name: '" + COMMIT + "' ]],\n"
                + "userRemoteConfigs: [[url: '" + REPOSITORY + "']],\n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                + "            relativeTargetDir: 'checkout']]])\n"
                + "    publishCoverage adapters: [jacocoAdapter('" + JACOCO_BIG_DATA
                + "')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        String consoleLog = getConsoleLog(build);
        assertThat(consoleLog)
                .contains("Cloning repository " + REPOSITORY)
                .contains("Checking out Revision " + COMMIT)
                .contains("git checkout -f " + COMMIT);
    }




    //TODO declarative pipeline support --> see shouldIRunInDeclarativePipeline im analysis model

//    @Test
//    public void declarativePipeline(){
//
//
//        WorkflowJob workflowJob = createPipeline();
//        workflowJob.setDefinition(new CpsFlowDefinition("pipeline {\n"
//                + "agent 'any' \n"
//                + "stages {n\"
//                + "     stage ('Create a fake warning') {n\"
//                + "         steps{n\"
//                + createShellStep ( "echo \"foo.cc:4:39 error: foo.h: No such file directory\" >warnings.log" )
//                +"               }\n
//                +"       }\n
//                +"}\n
//                +"post {n\"
//                +"      always {n\"
//                +"          recordIssues tool: gcc4(pattern: 'warnings.log')\n"
//                +"    }\n
//                +"}\n
//                +"}", true));
//        AnalysisResult result = scheduleSuccessfulBuild(workflowJob);
//        assertThat(result).hasTotalSize(1);
//    }

    //TODO multiple invocations of step (no tag set)

    //TODO multiple invocations of step (tag set) --> prüfen, wie das tag setzen hier erfolgt


    @Test
    public void withNoTag() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_BIG_DATA + "')]"
                + "   publishCoverage adapters: [jacocoAdapter('" + JACOCO_SMALL_DATA + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(2);
    }

    @Test
    public void withTag() {
        WorkflowJob workflowJob = createPipelineWithWorkspaceFiles(JACOCO_BIG_DATA, JACOCO_SMALL_DATA);
        workflowJob.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters('someTag'): [jacocoAdapter('" + JACOCO_BIG_DATA + "')]"
                + "   publishCoverage adapters('someTag'): [jacocoAdapter('" + JACOCO_SMALL_DATA + "')]"
                + "}", true));

        Run<?, ?> build = buildWithResult(workflowJob, Result.FAILURE);
        assertThat(build.getNumber()).isEqualTo(1);
    }

    @Test
    public void multipleInvocationsTagSet(){


        WorkflowJob workflowJob = createPipeline();
        workflowJob.setDefinition(new CpsFlowDefinition("node ('docker' {"
                + "timestamps {\n"
                + "checkout([$class: 'GitSCM',"
                + "branches: [[name: '6bd346bbcc9779467ce657b2618ab11e38e28c2c']], \n"
                + "userRemoteConfigs: [[url: '" + "https://github.com/jenkinsci/analysis-model.git"+"']], \n"
                + "extensions: [[$class: 'RelativeTargetDirectory', \n"
                +               "relativeTagertDir: 'checkout']]]), \n"
                +   "publishCoverage tag:'someTag' adapters: [jacocoAdapters('" + JACOCO_BIG_DATA +"')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                +   "publishCoverage tag:'someTag' adapters: [jacocoAdapters('" + JACOCO_SMALL_DATA +"')], sourceFileResolver: sourceFiles('STORE_ALL_BUILD')\n"
                + "}"
                + "}", true));

    }




    @Test
    public void testAdapters() throws Exception {
        CoberturaReportAdapter blub = new CoberturaReportAdapter("cobertura-coverage.xml");
        blub.setMergeToOneReport(true);
        JacocoReportAdapter blob = new JacocoReportAdapter("cobertura-coverage.xml");
        blob.setMergeToOneReport(true);
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
                .addAdapter(blub)
                .addAdapter(blob);


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));
        workspace.child("jacoco.xml").copyFrom(getClass()
                .getResourceAsStream("jacoco.xml"));

        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);



        j.assertBuildStatusSuccess(j.waitForCompletion(r));

        CoverageBuildAction coverageResult = r.getAction(CoverageBuildAction.class);
        HealthReportingAction x = r.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(100);
        //assertThat(coverageResult.getLineCoverage())
         //       .isEqualTo(new Coverage(5319, 5581 - 5319));
        String xy = coverageResult.getDelta(CoverageMetric.FILE);
        //int y = coverageResult.getReferenceBuild().get()
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("A total of 2 reports were found", r);

    }


    @Test
    public void coveragePluginReportFails() throws Exception {
        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();
        // .addAdapter(new CoberturaReportAdapter("cobertura-coverage.xml"))
        //  .addAdapter(new JacocoReportAdapter("jacoco.xml"));


        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        /*bjects.requireNonNull(workspace)
                .child("cobertura-coverage.xml")
                .copyFrom(getClass().getResourceAsStream("cobertura-coverage.xml"));
        workspace.child("jacoco.xml").copyFrom(getClass()
                .getResourceAsStream("jacoco.xml"));
*/
        project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);

    }





    @Test
    public void coveragePluginPipelineTest() throws Exception {
        //CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder()
               /* .addAdapter(new CoberturaReportAdapter(COBERTURA_FILE_NAME) {{
                    setThresholds(Collections.singletonList(new Threshold() {{
                        new Threshold("cobertura") {{
                            setUnhealthyThreshold(90);
                            setUnstableThreshold(93);
                            setFailUnhealthy(true);
                    }}))
                }}))
                .setFailUnhealthy(true)
                .setFailNoReports(false)
                .setApplyThresholdRecursively(true)
                .addGlobalThreshold(new Threshold("cobertura") {{
                    setUnhealthyThreshold(90);
                    setUnstableThreshold(93);
                    setFailUnhealthy(true);
                }})));
                */
        //  .addAdapter(new JacocoReportAdapter("jacoco.xml"));

        CoverageScriptedPipelineScriptBuilder builder = CoverageScriptedPipelineScriptBuilder.builder();
        WorkflowJob project = j.createProject(WorkflowJob.class, "coverage-pipeline-test");
        project.setDefinition(new CpsFlowDefinition(""));
        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        Objects.requireNonNull(workspace)
                .child(COBERTURA_FILE_NAME)
                .copyFrom(getClass().getResourceAsStream(COBERTURA_FILE_NAME));

        //project.setDefinition(new CpsFlowDefinition(builder.build(), true));
        WorkflowRun r = Objects.requireNonNull(project.scheduleBuild2(0)).waitForStart();

        Assert.assertNotNull(r);

        CoverageBuildAction coverageResult = r.getAction(CoverageBuildAction.class);
        HealthReportingAction x = r.getAction(HealthReportingAction.class);

        assertThat(x.getBuildHealth().getScore()).isEqualTo(23);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(1204, 1916 - 1204));
        j.assertBuildStatusSuccess(j.waitForCompletion(r));
        j.assertLogContains("No reports were found", r);

    }


}
