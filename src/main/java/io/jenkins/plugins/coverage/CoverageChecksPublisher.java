package io.jenkins.plugins.coverage;

import edu.hm.hafner.util.VisibleForTesting;
import hudson.model.Run;
import io.jenkins.plugins.checks.api.*;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.coverage.targets.*;
import io.jenkins.plugins.util.JenkinsFacade;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Publishes coverage as checks to scm platforms.
 *
 * @author Kezhi Xiong
 */
class CoverageChecksPublisher {
    private final CoverageAction action;
    private final JenkinsFacade jenkinsFacade;

    CoverageChecksPublisher(final CoverageAction action) {
        this(action, new JenkinsFacade());
    }

    @VisibleForTesting
    CoverageChecksPublisher(final CoverageAction action, final JenkinsFacade jenkinsFacade) {
        this.jenkinsFacade = jenkinsFacade;
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
                .withSummary(extractSummary(result))
                .withText(extractChecksText(result))
                .build();

        return new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(ChecksConclusion.SUCCESS)
                .withDetailsURL(jenkinsFacade.getAbsoluteUrl(action.getUrlName()))
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
        Map<CoverageElement, Ratio> lastRatios = getLastRatios(result);

        String lineTitle;
        float lineCoverage = result.getCoverage(CoverageElement.LINE).getPercentageFloat();
        if (result.getLinkToBuildThatWasUsedForComparison() != null) {
            lineTitle = extractChecksTitle("Line", "target branch build", lineCoverage,
                    result.getChangeRequestCoverageDiffWithTargetBranch());
        } else if (lastRatios.containsKey(CoverageElement.LINE)) {
            lineTitle = extractChecksTitle("Line", "last successful build", lineCoverage,
                    lineCoverage - lastRatios.get(CoverageElement.LINE).getPercentageFloat());
        } else {
            lineTitle = extractChecksTitle("Line", "", lineCoverage, 0);
        }

        String branchTitle;
        float branchCoverage = result.getCoverage(CoverageElement.CONDITIONAL).getPercentageFloat();
        if (lastRatios.containsKey(CoverageElement.CONDITIONAL)) {
            branchTitle = extractChecksTitle("Branch", "last successful build", branchCoverage,
                    branchCoverage - lastRatios.get(CoverageElement.CONDITIONAL).getPercentageFloat());
        } else {
            branchTitle = extractChecksTitle("Branch", "", branchCoverage, 0);
        }

        return lineTitle + " " + branchTitle;
    }

    private String extractChecksTitle(final String elementName, final String targetBuildName,
                                      final float coverage, final float coverageDiff) {
        StringBuilder title = new StringBuilder()
                .append(elementName)
                .append(" coverage: ")
                .append(String.format("%.2f", coverage))
                .append("%");

        if (StringUtils.isBlank(targetBuildName)) {
            title.append(".");
        } else {
            title.append(" (")
                    .append(String.format("%+.2f%%", coverageDiff))
                    .append(" against ")
                    .append(targetBuildName)
                    .append(").");
        }

        return title.toString();
    }

    private String extractSummary(final CoverageResult result) {
        StringBuilder summary = new StringBuilder();
        if (result.getLinkToBuildThatWasUsedForComparison() != null) {
            summary.append("* ### [target branch build](")
                    .append(jenkinsFacade.getAbsoluteUrl(result.getLinkToBuildThatWasUsedForComparison()))
                    .append(")\n");
        }

        Run<?, ?> lastSuccessfulBuild = result.getOwner().getPreviousSuccessfulBuild();
        if (lastSuccessfulBuild != null) {
            summary.append("* ### [last successful build](")
                    .append(jenkinsFacade.getAbsoluteUrl(lastSuccessfulBuild.getUrl()))
                    .append(")\n");
        }

        return summary.toString();
    }

    private Map<CoverageElement, Ratio> getLastRatios(final CoverageResult result) {
        CoverageResult previousResult = result.getPreviousResult();
        if (previousResult == null) {
            return Collections.emptyMap();
        }

        return previousResult.getResults();
    }
}
