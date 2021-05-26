package io.jenkins.plugins.coverage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import org.jvnet.localizer.Localizable;
import hudson.model.HealthReport;
import hudson.model.Run;

import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksStatus;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.util.JenkinsFacade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CoverageChecksPublisherTest {
    private static final String JENKINS_BASE_URL = "http://127.0.0.1:8080";
    private static final String COVERAGE_URL_NAME = "coverage";
    private static final String BUILD_LINK = "job/pipeline-coding-style/job/PR-3/49/";
    private static final String TARGET_BUILD_LINK = "job/pipeline-coding-style/job/master/3/";
    private static final String LAST_SUCCESSFUL_BUILD_LINK = "http://127.0.0.1:8080/job/pipeline-coding-style/view/change-requests/job/PR-3/110/";
    private static final String HEALTH_REPORT = "Coverage Healthy score is 100%";

    @Test
    public void shouldConstructChecksDetailsWithLineAndMethodCoverage() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(JENKINS_BASE_URL + "/" + BUILD_LINK + COVERAGE_URL_NAME)
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line: 60.00%. Branch: 40.00%.")
                        .withSummary("## " + HEALTH_REPORT + ".")
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
        when(build.getUrl()).thenReturn(BUILD_LINK);
        when(build.getPreviousSuccessfulBuild()).thenReturn(null);

        assertThat(new CoverageChecksPublisher(createActionWithDefaultHealthReport(result), createJenkins())
                .extractChecksDetails())
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
                        .withTitle("Line: 50.00% (+10.00% against target branch). " +
                                "Branch: 50.00% (+20.00% against last successful build).")
                        .withSummary("* ### [Target branch build](" + JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK + ")\n"
                                + "* ### [Last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n"
                                + "## " + HEALTH_REPORT + ".")
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

        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn(HEALTH_REPORT);
        action.setHealthReport(new HealthReport(100, localizable));

        assertThat(new CoverageChecksPublisher(createActionWithDefaultHealthReport(result), createJenkins())
                .extractChecksDetails())
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
                        .withTitle("Line: 50.00% (-10.00% against target branch). " +
                                "Branch: 50.00% (-20.00% against last successful build).")
                        .withSummary("* ### [Target branch build](" + JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK + ")\n"
                                + "* ### [Last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n"
                                + "## " + HEALTH_REPORT + ".")
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

        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn(HEALTH_REPORT);
        action.setHealthReport(new HealthReport(100, localizable));

        assertThat(new CoverageChecksPublisher(createActionWithDefaultHealthReport(result), createJenkins())
                .extractChecksDetails())
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
                        .withTitle("Line: 60.00% (+0.00% against target branch). " +
                                "Branch: 40.00% (+0.00% against last successful build).")
                        .withSummary("* ### [Target branch build](" + JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK + ")\n"
                                + "* ### [Last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n"
                                + "## " + HEALTH_REPORT + ".")
                        .withText("||Conditional|Line|\n" +
                                "|:-:|:-:|:-:|\n" +
                                "|:white_check_mark: **Coverage**|40.00%|60.00%|\n" +
                                "|:chart_with_upwards_trend: **Trend**|+0.00% :arrow_right:|+0.00% :arrow_right:|")
                        .build())
                .build();

        CoverageResult result = createCoverageResult((float)0.6, (float)0.4, (float)0.6, (float)0.4, TARGET_BUILD_LINK, 0);
        CoverageAction action = new CoverageAction(result);

        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn(HEALTH_REPORT);
        action.setHealthReport(new HealthReport(100, localizable));

        assertThat(new CoverageChecksPublisher(createActionWithDefaultHealthReport(result), createJenkins())
                .extractChecksDetails())
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
                        .withTitle("Line: 60.00% (+10.00% against last successful build). " +
                                "Branch: 40.00% (+10.00% against last successful build).")
                        .withSummary("* ### [Last successful build](" + JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK + ")\n"
                                + "## " + HEALTH_REPORT + ".")
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

        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn(HEALTH_REPORT);
        action.setHealthReport(new HealthReport(100, localizable));

        assertThat(new CoverageChecksPublisher(createActionWithDefaultHealthReport(result), createJenkins())
                .extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldPublishFailedCheck() {
        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.FAILURE)
                .withDetailsURL(JENKINS_BASE_URL + "/" + BUILD_LINK + COVERAGE_URL_NAME)
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line: 60.00%. Branch: 40.00%.")
                        .withSummary("## Coverage Healthy score is 10%.\n"
                                + "## Failed because coverage is unhealthy.")
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
        when(build.getUrl()).thenReturn(BUILD_LINK);
        when(build.getPreviousSuccessfulBuild()).thenReturn(null);

        CoverageAction action = new CoverageAction(result);

        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn("Coverage Healthy score is 10%");
        action.setHealthReport(new HealthReport(100, localizable));
        action.setFailMessage("Failed because coverage is unhealthy.");

        assertThat(new CoverageChecksPublisher(action, createJenkins())
                .extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldReportNoLineOrBranchCoverageInChecksTitle() {
        Run build = mock(Run.class);
        CoverageResult result = mock(CoverageResult.class);
        when(result.getOwner()).thenReturn(build);
        when(build.getPreviousSuccessfulBuild()).thenReturn(null);
        when(build.getUrl()).thenReturn(BUILD_LINK);

        Map<CoverageElement, Ratio> ratios = new HashMap<>();
        when(result.getResults()).thenReturn(ratios);
        when(result.getCoverage(CoverageElement.LINE)).thenReturn(null);
        when(result.getCoverage(CoverageElement.CONDITIONAL)).thenReturn(null);
        when(result.getCoverageTrends()).thenReturn(null);

        CoverageAction action = new CoverageAction(result);
        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn(HEALTH_REPORT);
        action.setHealthReport(new HealthReport(100, localizable));

        assertThat(new CoverageChecksPublisher(createActionWithDefaultHealthReport(result), createJenkins())
                .extractChecksDetails().getOutput())
                .isPresent()
                .get()
                .hasFieldOrPropertyWithValue("title", Optional.of("No line or branch coverage has been computed."));
    }

    private CoverageResult createCoverageResult(final float lastLineCoverage, final float lastConditionCoverage,
                                                final float lineCoverage, final float conditionCoverage,
                                                final String targetBuildLink, final float targetBuildDiff) {
        Run build = mock(Run.class);
        Run lastBuild = mock(Run.class);
        CoverageResult lastResult = createCoverageResult(lastLineCoverage, lastConditionCoverage);
        CoverageResult result = createCoverageResult(lineCoverage, conditionCoverage);

        when(result.getPreviousResult()).thenReturn(lastResult);
        when(result.getReferenceBuildUrl()).thenReturn(targetBuildLink);
        when(result.getCoverageDelta(CoverageElement.LINE)).thenReturn(targetBuildDiff);
        when(result.getOwner()).thenReturn(build);
        when(build.getUrl()).thenReturn(BUILD_LINK);
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

    private CoverageAction createActionWithDefaultHealthReport(final CoverageResult result) {
        CoverageAction action = new CoverageAction(result);

        Localizable localizable = mock(Localizable.class);
        when(localizable.toString()).thenReturn(HEALTH_REPORT);
        action.setHealthReport(new HealthReport(100, localizable));

        return action;
    }

    private JenkinsFacade createJenkins() {
        JenkinsFacade jenkinsFacade = mock(JenkinsFacade.class);
        when(jenkinsFacade.getAbsoluteUrl(BUILD_LINK, COVERAGE_URL_NAME)).thenReturn(JENKINS_BASE_URL + "/" + BUILD_LINK + COVERAGE_URL_NAME);
        when(jenkinsFacade.getAbsoluteUrl(TARGET_BUILD_LINK)).thenReturn(JENKINS_BASE_URL + "/" + TARGET_BUILD_LINK);
        when(jenkinsFacade.getAbsoluteUrl(LAST_SUCCESSFUL_BUILD_LINK)).thenReturn(JENKINS_BASE_URL + "/" + LAST_SUCCESSFUL_BUILD_LINK);

        return jenkinsFacade;
    }
}
