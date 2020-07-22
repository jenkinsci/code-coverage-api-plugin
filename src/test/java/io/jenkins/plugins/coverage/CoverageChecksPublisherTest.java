package io.jenkins.plugins.coverage;

import hudson.model.Run;
import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksStatus;
import io.jenkins.plugins.coverage.targets.*;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoverageChecksPublisherTest {
    @Test
    public void shouldConstructChecksDetailsWithLineAndMethodCoverage() throws Exception {
        CoverageResult result = createCoverageResult((float)0.6, (float)0.4);
        CoverageAction action = mock(CoverageAction.class);
        when(action.getResult()).thenReturn(result);
        when(action.getAbsoluteUrl()).thenReturn("http://127.0.0.1:8080/job/pipeline-coding-style/job/PR-3/49/coverage");

        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL("http://127.0.0.1:8080/job/pipeline-coding-style/job/PR-3/49/coverage")
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage of 60%.")
                        .withSummary("")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 40%\n"
                                + "## Line\n* :white_check_mark: Coverage: 60%\n")
                        .build())
                .build();

        assertThat(new CoverageChecksPublisher(action).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldConstructChecksDetailsWithDecreasedLineCoverageAndIncreasedConditionalCoverage() {
        Run build = mock(Run.class);
        Run lastBuild = mock(Run.class);
        when(build.getPreviousSuccessfulBuild()).thenReturn(lastBuild);
        when(lastBuild.getId()).thenReturn("1");

        CoverageResult result = createCoverageResultWithTrend((float)0.4, (float)0.6, (float)0.5, (float)0.5, "#1");
        when(result.getOwner()).thenReturn(build);

        CoverageAction action = mock(CoverageAction.class);
        when(action.getResult()).thenReturn(result);
        when(action.getAbsoluteUrl()).thenReturn("http://127.0.0.1:8080/job/pipeline-coding-style/job/PR-3/49/coverage");

        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL("http://127.0.0.1:8080/job/pipeline-coding-style/job/PR-3/49/coverage")
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage of 40% is less than the last successful build (50%).")
                        .withSummary("")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 60%\n* :arrow_up: Trend: 10%\n"
                                + "## Line\n* :white_check_mark: Coverage: 40%\n* :arrow_down: Trend: 10%\n")
                        .build())
                .build();

        assertThat(new CoverageChecksPublisher(action).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    @Test
    public void shouldConstructChecksDetailsWithUnchangedLineAndConditionalCoverage() {
        Run build = mock(Run.class);
        Run lastBuild = mock(Run.class);
        when(build.getPreviousSuccessfulBuild()).thenReturn(lastBuild);
        when(lastBuild.getId()).thenReturn("1");

        CoverageResult result = createCoverageResultWithTrend((float)0.6, (float)0.4, (float)0.6, (float)0.4, "#1");
        when(result.getOwner()).thenReturn(build);

        CoverageAction action = mock(CoverageAction.class);
        when(action.getResult()).thenReturn(result);
        when(action.getAbsoluteUrl()).thenReturn("http://127.0.0.1:8080/job/pipeline-coding-style/job/PR-3/49/coverage");

        ChecksDetails expectedDetails = new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL("http://127.0.0.1:8080/job/pipeline-coding-style/job/PR-3/49/coverage")
                .withOutput(new ChecksOutputBuilder()
                        .withTitle("Line coverage of 60% is the same as the last successful build (60%).")
                        .withSummary("")
                        .withText("## Conditional\n* :white_check_mark: Coverage: 40%\n* :arrow_right: Trend: 0%\n"
                                + "## Line\n* :white_check_mark: Coverage: 60%\n* :arrow_right: Trend: 0%\n")
                        .build())
                .build();

        assertThat(new CoverageChecksPublisher(action).extractChecksDetails())
                .usingRecursiveComparison()
                .isEqualTo(expectedDetails);
    }

    private CoverageResult createCoverageResult(final float lineCoverage, final float conditionCoverage) {
        CoverageResult result = mock(CoverageResult.class);

        Map<CoverageElement, Ratio> ratios = new HashMap<>(3);
        ratios.put(CoverageElement.LINE, Ratio.create(lineCoverage, 1));
        ratios.put(CoverageElement.CONDITIONAL, Ratio.create(conditionCoverage, 1));

        when(result.getResults()).thenReturn(ratios);
        when(result.getCoverage(CoverageElement.LINE)).thenReturn(Ratio.create(lineCoverage, 1));
        when(result.getCoverageTrends()).thenReturn(null);

        return result;
    }

    private CoverageResult createCoverageResultWithTrend(final float lineCoverage, final float conditionalCoverage,
                                                         final float lastLineCoverage, final float lastConditionalCoverage,
                                                         final String lastBuildName) {
        CoverageResult result = createCoverageResult(lineCoverage, conditionalCoverage);
        CoverageTrend trend = new CoverageTrend(lastBuildName, Arrays.asList(
                new CoverageTreeElement(CoverageElement.LINE, Ratio.create(lastLineCoverage, 1)),
                new CoverageTreeElement(CoverageElement.CONDITIONAL, Ratio.create(lastConditionalCoverage, 1))));
        when(result.getCoverageTrends()).thenReturn(Collections.singletonList(trend));

        return result;
    }
}
