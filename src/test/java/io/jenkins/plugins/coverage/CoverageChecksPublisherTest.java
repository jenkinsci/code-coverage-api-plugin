package io.jenkins.plugins.coverage;

import hudson.model.Run;
import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksStatus;
import io.jenkins.plugins.coverage.targets.*;
import io.jenkins.plugins.util.JenkinsFacade;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoverageChecksPublisherTest {
    private static final String JENKINS_BASE_URL = "http://127.0.0.1:8080";
    private static final String COVERAGE_URL_NAME = "coverage";
    private static final String BUILD_LINK = "job/pipeline-coding-style/job/PR-3/49/";
    private static final String TARGET_BUILD_LINK = "job/pipeline-coding-style/job/master/3/";
    private static final String LAST_SUCCESSFUL_BUILD_LINK = "http://127.0.0.1:8080/job/pipeline-coding-style/view/change-requests/job/PR-3/110/";

    @Test
    public void shouldConstructChecksDetailsWithLineAndMethodCoverage() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(JENKINS_BASE_URL + "/" + BUILD_LINK + COVERAGE_URL_NAME)
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage: 60.00%. Branch coverage: 40.00%.")
                        .withSummary("")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 40%\n"
                                + "## Line\n* :white_check_mark: Coverage: 60%\n")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|40.00%|60.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|-|-|")
                        .build())
                .build();

        Run build = mock(Run.class);
        CoverageResult result = createCoverageResult((float)0.6, (float)0.4);
        when(result.getPreviousResult()).thenReturn(null);
        when(result.getOwner()).thenReturn(build);
        when(build.getPreviousSuccessfulBuild()).thenReturn(null);

        CoverageAction action = new CoverageAction(result);
        assertThat(new CoverageChecksPublisher(action, createJenkins()).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldConstructChecksDetailsWithIncreasedLineCoverageAndConditionalCoverage() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(JENKINS_BASE_URL + "/" + BUILD_LINK + COVERAGE_URL_NAME)
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage: 50.00% (+10.00% against target branch build). " +
                                "Branch coverage: 50.00% (+20.00% against last successful build).")
                        .withSummary("* ### [target branch build](" + JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK + ")\n"
                                + "* ### [last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 50%\n* :arrow_up: Trend: 20%\n"
                                + "## Line\n* :white_check_mark: Coverage: 50%\n* :arrow_up: Trend: 10%\n")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|50.00%|50.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|+20.00% :arrow_up:|+10.00% :arrow_up:|")
                        .build())
                .build();

        CoverageResult result = createCoverageResult((float)0.4, (float)0.3, (float)0.5, (float)0.5, TARGET_BUILD_LINK, +10);
        CoverageAction action = new CoverageAction(result);
        assertThat(new CoverageChecksPublisher(action, createJenkins()).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldConstructChecksDetailsWithDecreasedLineCoverageAndConditionalCoverage() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(JENKINS_BASE_URL + "/job/pipeline-coding-style/job/PR-3/49/coverage")
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage: 50.00% (-10.00% against target branch build). " +
                                "Branch coverage: 50.00% (-20.00% against last successful build).")
                        .withSummary("* ### [target branch build](" + JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK + ")\n"
                                + "* ### [last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 50%\n* :arrow_down: Trend: 20%\n"
                                + "## Line\n* :white_check_mark: Coverage: 50%\n* :arrow_down: Trend: 10%\n")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|50.00%|50.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|-20.00% :arrow_down:|-10.00% :arrow_down:|")
                        .build())
                .build();

        CoverageResult result = createCoverageResult((float)0.6, (float)0.7, (float)0.5, (float)0.5, TARGET_BUILD_LINK, -10);
        CoverageAction action = new CoverageAction(result);
        assertThat(new CoverageChecksPublisher(action, createJenkins()).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldConstructChecksDetailsWithUnchangedLineAndConditionalCoverage() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(JENKINS_BASE_URL + "/job/pipeline-coding-style/job/PR-3/49/coverage")
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage: 60.00% (+0.00% against target branch build). " +
                                "Branch coverage: 40.00% (+0.00% against last successful build).")
                        .withSummary("* ### [target branch build](" + JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK + ")\n"
                                + "* ### [last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|40.00%|60.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|+0.00% :arrow_right:|+0.00% :arrow_right:|")
                        .build())
                .build();

        CoverageResult result = createCoverageResult((float)0.6, (float)0.4, (float)0.6, (float)0.4, TARGET_BUILD_LINK, 0);
        CoverageAction action = new CoverageAction(result);
        assertThat(new CoverageChecksPublisher(action, createJenkins()).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldUseLastSuccessfulBuildForLineCoverageIfNoTargetBranchIsComparedWith() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(JENKINS_BASE_URL + "/job/pipeline-coding-style/job/PR-3/49/coverage")
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage: 60.00% (+10.00% against last successful build). " +
                                "Branch coverage: 40.00% (+10.00% against last successful build).")
                        .withSummary("* ### [last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 40%\n* :arrow_up: Trend: 10%\n"
                                + "## Line\n* :white_check_mark: Coverage: 60%\n* :arrow_up: Trend: 10%\n")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|40.00%|60.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|+10.00% :arrow_up:|+10.00% :arrow_up:|")
                        .build())
                .build();

        CoverageResult result = createCoverageResult((float)0.5, (float)0.3, (float)0.6, (float)0.4, null, 0);
        CoverageAction action = new CoverageAction(result);
        assertThat(new CoverageChecksPublisher(action, createJenkins()).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    private CoverageResult createCoverageResult(final float lastLineCoverage, final float lastConditionCoverage,
                                                final float lineCoverage, final float conditionCoverage,
                                                final String targetBuildLink, final float targetBuildDiff) {
        Run build = mock(Run.class);
        Run lastBuild = mock(Run.class);
        CoverageResult lastResult = createCoverageResult(lastLineCoverage, lastConditionCoverage);
        CoverageResult result = createCoverageResult(lineCoverage, conditionCoverage);
        when(result.getPreviousResult()).thenReturn(lastResult);
        when(result.getLinkToBuildThatWasUsedForComparison()).thenReturn(targetBuildLink);
        when(result.getChangeRequestCoverageDiffWithTargetBranch()).thenReturn(targetBuildDiff);
        when(result.getOwner()).thenReturn(build);
        when(build.getPreviousSuccessfulBuild()).thenReturn(lastBuild);
        when(lastBuild.getUrl()).thenReturn(LAST_SUCCESSFUL_BUILD_LINK);

        return result;
    }

    private CoverageResult createCoverageResult(final float lineCoverage, final float conditionCoverage) {
        CoverageResult result = mock(CoverageResult.class);

        Map<CoverageElement, Ratio> ratios = new HashMap<>(3);
        ratios.put(CoverageElement.LINE, Ratio.create(lineCoverage, 1));
        ratios.put(CoverageElement.CONDITIONAL, Ratio.create(conditionCoverage, 1));

        when(result.getResults()).thenReturn(ratios);
        when(result.getCoverage(CoverageElement.LINE)).thenReturn(Ratio.create(lineCoverage, 1));
        when(result.getCoverage(CoverageElement.CONDITIONAL)).thenReturn(Ratio.create(conditionCoverage, 1));
        when(result.getCoverageTrends()).thenReturn(null);

        return result;
    }

    private JenkinsFacade createJenkins() {
        JenkinsFacade jenkinsFacade = mock(JenkinsFacade.class);
        when(jenkinsFacade.getAbsoluteUrl(COVERAGE_URL_NAME)).thenReturn(JENKINS_BASE_URL + "/" + BUILD_LINK + COVERAGE_URL_NAME);
        when(jenkinsFacade.getAbsoluteUrl(TARGET_BUILD_LINK)).thenReturn(JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK);
        when(jenkinsFacade.getAbsoluteUrl(LAST_SUCCESSFUL_BUILD_LINK)).thenReturn(JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK);

        return jenkinsFacade;
    }
}
