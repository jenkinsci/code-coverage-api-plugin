package io.jenkins.plugins.coverage;

import edu.hm.hafner.util.VisibleForTesting;
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
        Map<CoverageElement, Ratio> lastRatios = getLastRatios(result);

        StringBuilder text = new StringBuilder();
        for (Map.Entry<CoverageElement, Ratio> singleRatio : ratios.entrySet()) {
            text.append("## ")
                    .append(singleRatio.getKey().getName())
                    .append("\n* :white_check_mark: Coverage: ")
                    .append(singleRatio.getValue().getPercentage())
                    .append("%");

            if (!lastRatios.isEmpty()) {
                text.append("\n* ");

                int delta = (int)(singleRatio.getValue().getPercentageFloat() - lastRatios.get(singleRatio.getKey()).getPercentage());
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
        int branchCoverage = result.getCoverage(CoverageElement.CONDITIONAL).getPercentage();

        Map<CoverageElement, Ratio> lastRatios = getLastRatios(result);
        int lastLineCoverage = lastRatios.getOrDefault(CoverageElement.LINE, Ratio.create(-1, 100)).getPercentage();
        int lastBranchCoverage = lastRatios.getOrDefault(CoverageElement.CONDITIONAL, Ratio.create(-1, 100)).getPercentage();

        return extractChecksTitle("Line", lineCoverage, lastLineCoverage)
                + " "
                + extractChecksTitle("Branch", branchCoverage, lastBranchCoverage);
    }

    private String extractChecksTitle(final String name, final int coverage, final int lastCoverage) {
        StringBuilder title = new StringBuilder()
                .append(name)
                .append(" coverage: ")
                .append(coverage)
                .append("%");

        if (lastCoverage == -1) {
            return title.append(".")
                    .toString();
        }

        if (coverage < lastCoverage) {
            title.append(" (decreased ")
                    .append(lastCoverage - coverage)
                    .append("%).");
        } else if (coverage > lastCoverage) {
            title.append(" (increased ")
                    .append(coverage - lastCoverage)
                    .append("%).");
        } else {
            title.append(" (unchanged).");
        }

        return title.toString();
    }

    private Map<CoverageElement, Ratio> getLastRatios(final CoverageResult result) {
        CoverageResult previousResult = result.getPreviousResult();
        if (previousResult == null) {
            return Collections.emptyMap();
        }

        return previousResult.getResults();
    }
}
