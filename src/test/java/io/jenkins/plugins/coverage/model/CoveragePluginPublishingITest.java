package io.jenkins.plugins.coverage.model;

import java.util.Collections;

import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Run;

import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksStatus;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

public class CoveragePluginPublishingITest extends IntegrationTestWithJenkinsPerSuite {
    private static final String JACOCO_FILE_NAME = "jacoco-analysis-model.xml";

    @Test
    public void skipPublishingOfChecks() {
        FreeStyleProject project = createFreeStyleProject();
        FreeStyleProject project2 = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_FILE_NAME);
        copyFilesToWorkspace(project2, JACOCO_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoveragePublisher coveragePublisher2 = new CoveragePublisher();

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_FILE_NAME);


        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line: 60.00%. Branch: 40.00%.")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 40%\n"
                                + "## Line\n* :white_check_mark: Coverage: 60%\n")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|40.00%|60.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|-|-|")
                        .build())
                .build();



    coveragePublisher.setSkipPublishingChecks(true);
        coveragePublisher2.setSkipPublishingChecks(false);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        coveragePublisher2.setAdapters(Collections.singletonList(jacocoReportAdapter));


        project.getPublishersList().add(coveragePublisher);
        project2.getPublishersList().add(coveragePublisher2);

        Run<?, ?> build = buildSuccessfully(project);
        Run<?, ?> build2 = buildSuccessfully(project2);

        assertThat(build.getNumber()).isEqualTo(1);


        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        CoverageBuildAction coverageResult2 = build2.getAction(CoverageBuildAction.class);

        if (coverageResult == coverageResult2) {
            System.out.println("HI");
        }

    }

}
