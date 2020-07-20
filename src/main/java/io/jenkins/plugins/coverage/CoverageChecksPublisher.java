package io.jenkins.plugins.coverage;

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

    private ChecksDetails extractChecksDetails() {
        CoverageResult result = action.getResult();
        ChecksOutputBuilder outputBuilder = new ChecksOutputBuilder()
                .withTitle("Code Coverage")
                .withSummary("## Generated " + result.getResults().size() + " types of coverage.")
                .withText(extractChecksText(result)); // TODO: improve summary

        return new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(action.getOwner().getAbsoluteUrl() + "coverage/")
                .withOutput(outputBuilder.build())
                .build();
    }

    private String extractChecksText(CoverageResult result) {
        Map<CoverageElement, Ratio> ratios = result.getResults();
        Map<String, Float> lastRatios = getLastRatios(result);

        StringBuilder text = new StringBuilder();
        for (Map.Entry<CoverageElement, Ratio> singleRatio : ratios.entrySet()) {
            text.append("## ")
                    .append(singleRatio.getKey().getName())
                    .append(": \n* Coverage: ")
                    .append(singleRatio.getValue())
                    .append(" :white_check_mark:");

            if (!lastRatios.isEmpty()) {
                text.append("\n* Trend: ");

                float delta = singleRatio.getValue().getPercentageFloat() - lastRatios.get(singleRatio.getKey().getName());
                int compare = Float.compare(delta, (float) 0);
                if (compare > 0) {
                    text.append(Math.abs(delta))
                            .append("%")
                            .append(" :arrow_up:");
                } else if (compare < 0) {
                    text.append(Math.abs(delta))
                            .append("%")
                            .append(" :arrow_down:");
                } else {
                    text.append(Math.abs(delta))
                            .append("%")
                            .append(" :arrow_right:");
                }

            }

            text.append("\n");
        }

        return text.toString();
    }

    private Map<String, Float> getLastRatios(CoverageResult result) {
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
