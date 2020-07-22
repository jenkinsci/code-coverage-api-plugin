package io.jenkins.plugins.coverage;

import edu.hm.hafner.util.VisibleForTesting;
import hudson.model.Run;
import io.jenkins.plugins.checks.api.*;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.coverage.targets.*;

import java.util.*;

/**
 * Publishes coverage as checks to scm platforms.
 *
 * @author Kezhi Xiong
 */
class CoverageChecksPublisher {
    private final CoverageAction action;

    CoverageChecksPublisher(final CoverageAction action) {
        this.action = action;
    }

    void publishChecks() {
        ChecksPublisher publisher = ChecksPublisherFactory.fromRun(action.getOwner());
        publisher.publish(extractChecksDetails());
    }

    @VisibleForTesting
    ChecksDetails extractChecksDetails() {
        CoverageResult result = action.getResult();
        ChecksOutput output = new ChecksOutputBuilder()
                .withTitle(extractChecksTitle(result))
                .withSummary("")
                .withText(extractChecksText(result))
                .build();

        return new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(action.getAbsoluteUrl())
                .withOutput(output)
                .build();
    }

    private String extractChecksText(final CoverageResult result) {
        Map<CoverageElement, Ratio> ratios = result.getResults();
        Map<String, Float> lastRatios = getLastRatios(result);

        StringBuilder text = new StringBuilder();
        for (Map.Entry<CoverageElement, Ratio> singleRatio : ratios.entrySet()) {
            text.append("## ")
                    .append(singleRatio.getKey().getName())
                    .append("\n* :white_check_mark: Coverage: ")
                    .append(singleRatio.getValue().getPercentage())
                    .append("%");

            if (!lastRatios.isEmpty()) {
                text.append("\n* ");

                int delta = (int)(singleRatio.getValue().getPercentageFloat() - lastRatios.get(singleRatio.getKey().getName()));
                if (delta > 0) {
                    text.append(":arrow_up: ");
                } else if (delta < 0) {
                    text.append(":arrow_down: ");
                } else {
                    text.append(":arrow_right: ");
                }

                text.append("Trend: ")
                        .append(Math.abs(delta))
                        .append("%");
            }

            text.append("\n");
        }

        return text.toString();
    }

    private String extractChecksTitle(final CoverageResult result) {
        int lineCoverage = result.getCoverage(CoverageElement.LINE).getPercentage();
        int lastLineCoverage = getLastRatios(result).getOrDefault("Line", (float)-1.0).intValue();

        StringBuilder title = new StringBuilder()
                .append("Line coverage of ")
                .append(result.getCoverage(CoverageElement.LINE).getPercentage())
                .append("%");

        if (lastLineCoverage == -1) {
            return title.append(".")
                    .toString();
        }

        if (lineCoverage < lastLineCoverage) {
            title.append(" is less than ");
        } else if (lineCoverage > lastLineCoverage) {
            title.append(" is greater than ");
        } else {
            title.append(" is the same as ");
        }

        return title.append(String.format("the last successful build (%d%%).", lastLineCoverage))
                .toString();
    }

    private Map<String, Float> getLastRatios(final CoverageResult result) {
        List<CoverageTrend> trends = result.getCoverageTrends();
        if (trends == null) {
            return Collections.emptyMap();
        }

        Map<String, Float> ratioByType = new HashMap<>(trends.size());

        Run<?, ?> previousSuccess = result.getOwner().getPreviousSuccessfulBuild();
        if (previousSuccess != null) {
            trends.stream()
                    .filter(t -> t.getBuildName().equals("#" + previousSuccess.getId()))
                    .findFirst()
                    .ifPresent(
                            trend -> trend.getElements()
                                    .forEach(element -> ratioByType.put(element.getName(), element.getRatio()))
            );
        }

        return ratioByType;
    }
}
