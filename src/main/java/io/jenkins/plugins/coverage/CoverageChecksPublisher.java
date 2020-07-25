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
    private static final List<String> COVERAGE_TYPES =
            Arrays.asList("Report", "Group", "Package", "File", "Class", "Method", "Conditional", "Line", "Instruction");

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
                .withSummary(extractComparedBuildsSummary(result) + extractHealthSummary(action))
                .withText(extractChecksText(result))
                .build();

        return new ChecksDetailsBuilder()
                .withName("Code Coverage")
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(StringUtils.isBlank(action.getFailMessage()) ? ChecksConclusion.SUCCESS : ChecksConclusion.FAILURE)
                .withDetailsURL(jenkinsFacade.getAbsoluteUrl(result.getOwner().getUrl(), action.getUrlName()))
                .withOutput(output)
                .build();
    }

    private String extractChecksText(final CoverageResult result) {
        Map<String, Float> ratios = convertRatios(result.getResults());
        Map<String, Float> lastRatios = convertRatios(getLastRatios(result));

        List<String> containedTypes = new ArrayList<>(COVERAGE_TYPES.size());
        StringBuilder coverages = new StringBuilder("|:white_check_mark: **Coverage**|");
        StringBuilder trends = new StringBuilder("|:chart_with_upwards_trend: **Trend**|");
        for (String singleType : COVERAGE_TYPES) {
            if (ratios.containsKey(singleType)) {
                containedTypes.add(singleType);

                float percentage = ratios.get(singleType);
                coverages.append(String.format("%.2f%%|", percentage));

                if (lastRatios.containsKey(singleType)) {
                    float diff = percentage - lastRatios.get(singleType);
                    trends.append(String.format("%+.2f%%", diff));

                    if (Float.compare(diff, 0) > 0) {
                        trends.append(" :arrow_up:|");
                    } else if (Float.compare(diff, 0) < 0) {
                        trends.append(" :arrow_down:|");
                    } else {
                        trends.append(" :arrow_right:|");
                    }
                } else {
                    trends.append("-|");
                }
            }
        }

        return "||" + String.join("|", containedTypes) + "|\n"
                + "|" + String.join("", Collections.nCopies(containedTypes.size() + 1, ":-:|")) + "\n"
                + coverages + "\n"
                + trends;
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
                .append(String.format(": %.2f", coverage))
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

    private String extractComparedBuildsSummary(final CoverageResult result) {
        StringBuilder summary = new StringBuilder();
        if (result.getLinkToBuildThatWasUsedForComparison() != null) {
            summary.append("* ### [Target branch build](")
                    .append(jenkinsFacade.getAbsoluteUrl(result.getLinkToBuildThatWasUsedForComparison()))
                    .append(")\n");
        }

        Run<?, ?> lastSuccessfulBuild = result.getOwner().getPreviousSuccessfulBuild();
        if (lastSuccessfulBuild != null) {
            summary.append("* ### [Last successful build](")
                    .append(jenkinsFacade.getAbsoluteUrl(lastSuccessfulBuild.getUrl()))
                    .append(")\n");
        }

        return summary.toString();
    }

    private String extractHealthSummary(final CoverageAction action) {
        StringBuilder summary = new StringBuilder("## ")
                .append(action.getHealthReport().getLocalizableDescription().toString())
                .append(".");
        if (!StringUtils.isBlank(action.getFailMessage())) {
            summary.append("\n## ")
                    .append(action.getFailMessage());
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

    private Map<String, Float> convertRatios(final Map<CoverageElement, Ratio> ratios) {
        Map<String, Float> converted = new HashMap<String, Float>(ratios.size());
        for (Map.Entry<CoverageElement, Ratio> singleRatio : ratios.entrySet()) {
            converted.put(singleRatio.getKey().getName(), singleRatio.getValue().getPercentageFloat());
        }

        return converted;
    }
}
