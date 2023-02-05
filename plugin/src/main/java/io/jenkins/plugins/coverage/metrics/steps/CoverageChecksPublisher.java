package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.VisibleForTesting;

import hudson.Functions;
import hudson.model.TaskListener;

import io.jenkins.plugins.checks.api.ChecksAnnotation;
import io.jenkins.plugins.checks.api.ChecksAnnotation.ChecksAnnotationBuilder;
import io.jenkins.plugins.checks.api.ChecksAnnotation.ChecksAnnotationLevel;
import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksDetails.ChecksDetailsBuilder;
import io.jenkins.plugins.checks.api.ChecksOutput.ChecksOutputBuilder;
import io.jenkins.plugins.checks.api.ChecksPublisherFactory;
import io.jenkins.plugins.checks.api.ChecksStatus;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.QualityGateStatus;

/**
 * Publishes coverage as Checks to SCM platforms.
 *
 * @author Florian Orendi
 */
class CoverageChecksPublisher {

    private final ElementFormatter formatter;

    private final CoverageBuildAction action;
    private final JenkinsFacade jenkinsFacade;
    private final String checksName;

    CoverageChecksPublisher(final CoverageBuildAction action, final String checksName) {
        this(action, checksName, new JenkinsFacade());
    }

    @VisibleForTesting
    CoverageChecksPublisher(final CoverageBuildAction action,
            final String checksName, final JenkinsFacade jenkinsFacade) {
        this.jenkinsFacade = jenkinsFacade;
        this.action = action;
        this.checksName = checksName;
        this.formatter = new ElementFormatter();
    }

    void publishChecks(final TaskListener listener) {
        var publisher = ChecksPublisherFactory.fromRun(action.getOwner(), listener);
        publisher.publish(extractChecksDetails());
    }

    @VisibleForTesting
    ChecksDetails extractChecksDetails() {
        var output = new ChecksOutputBuilder()
                .withTitle(getChecksTitle())
                .withSummary(getSummary())
                .withAnnotations(getAnnotations())
                .build();

        return new ChecksDetailsBuilder()
                .withName(checksName)
                .withStatus(ChecksStatus.COMPLETED)
                .withConclusion(getCheckConclusion(action.getQualityGateResult().getOverallStatus()))
                .withDetailsURL(getCoverageReportBaseUrl())
                .withOutput(output)
                .build();
    }

    private String getChecksTitle() {
        return String.format("Change Coverage: %s (%s)",
                action.formatValueForMetric(Baseline.MODIFIED_LINES, Metric.LINE),
                formatValueOfMetric(action.getResult(), Metric.LOC));
    }

    private String getSummary() {
        var root = action.getResult();
        return getOverallCoverageSummary(root) + "\n\n"
                + getHealthReportSummary() + "\n\n"
                + getProjectMetricsSummary(root);
    }

    private List<ChecksAnnotation> getAnnotations() {
        var annotations = new ArrayList<ChecksAnnotation>();
        for (var fileNode : action.getResult().filterChanges().getAllFileNodes()) {
            for (var aggregatedLines : getAggregatedMissingLines(fileNode)) {
                ChecksAnnotationBuilder builder = new ChecksAnnotationBuilder()
                        .withPath(fileNode.getPath())
                        .withTitle(Messages.Checks_Annotation_Title())
                        .withAnnotationLevel(ChecksAnnotationLevel.WARNING)
                        .withMessage(getAnnotationMessage(aggregatedLines))
                        .withStartLine(aggregatedLines.startLine)
                        .withEndLine(aggregatedLines.endLine);

                annotations.add(builder.build());
            }
        }

        return annotations;
    }

    private String getAnnotationMessage(final AggregatedMissingLines aggregatedMissingLines) {
        if (aggregatedMissingLines.startLine == aggregatedMissingLines.endLine) {
            return Messages.Checks_Annotation_Message_SingleLine(aggregatedMissingLines.startLine);
        }
        return Messages.Checks_Annotation_Message_MultiLine(aggregatedMissingLines.startLine,
                aggregatedMissingLines.endLine);
    }

    private List<AggregatedMissingLines> getAggregatedMissingLines(final FileNode fileNode) {
        var aggregatedMissingLines = new ArrayList<AggregatedMissingLines>();

        if (fileNode.hasCoveredLinesInChangeSet()) {
            var linesWithCoverage = fileNode.getCoveredLines();
            // there has to be at least one line when it is a file node with changes
            var previousLine = linesWithCoverage.first();
            var aggregatedLines = new AggregatedMissingLines(previousLine);
            linesWithCoverage.remove(previousLine);

            for (final var line : linesWithCoverage) {
                if (line == previousLine + 1) {
                    aggregatedLines.increaseEndLine();
                }
                else {
                    aggregatedMissingLines.add(aggregatedLines);
                    aggregatedLines = new AggregatedMissingLines(line);
                }
                previousLine = line;
            }

            aggregatedMissingLines.add(aggregatedLines);
        }

        return aggregatedMissingLines;
    }

    private String getCoverageReportBaseUrl() {
        return jenkinsFacade.getAbsoluteUrl(action.getOwner().getUrl(), action.getUrlName());
    }

    private String getOverallCoverageSummary(final Node root) {
        String sectionHeader = getSectionHeader(2, Messages.Checks_Summary());

        var changedFilesCoverageRoot = root.filterByChangedFilesCoverage();
        var changeCoverageRoot = root.filterChanges();
        var indirectlyChangedCoverage = root.filterByIndirectlyChangedCoverage();

        var projectCoverageHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.PROJECT_DELTA.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.PROJECT_DELTA.getUrl())));
        var changedFilesCoverageHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.MODIFIED_FILES_DELTA.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.MODIFIED_FILES_DELTA.getUrl())));
        var changeCoverageHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.MODIFIED_LINES_DELTA.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.MODIFIED_LINES_DELTA.getUrl())));
        var indirectCoverageChangesHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.INDIRECT.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.INDIRECT.getUrl())));

        var projectCoverageLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, action::formatValueForMetric, Baseline.PROJECT));
        var projectCoverageBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, action::formatValueForMetric, Baseline.PROJECT));
        var projectCoverageComplexity = getBulletListItem(2, formatValueOfMetric(root, Metric.COMPLEXITY_DENSITY));
        var projectCoverageLoc = getBulletListItem(2, formatValueOfMetric(root, Metric.LOC));

        var changedFilesCoverageLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, action::formatValueForMetric, Baseline.MODIFIED_FILES));
        var changedFilesCoverageBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, action::formatValueForMetric, Baseline.MODIFIED_FILES));
        var changedFilesCoverageComplexity = getBulletListItem(2,
                formatValueOfMetric(changedFilesCoverageRoot, Metric.COMPLEXITY_DENSITY));
        var changedFilesCoverageLoc = getBulletListItem(2,
                formatValueOfMetric(changedFilesCoverageRoot, Metric.LOC));

        var changeCoverageLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, action::formatValueForMetric, Baseline.MODIFIED_LINES));
        var changeCoverageBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, action::formatValueForMetric, Baseline.MODIFIED_LINES));
        var changeCoverageLoc = getBulletListItem(2, formatValueOfMetric(changeCoverageRoot, Metric.LOC));

        var indirectCoverageChangesLine = getBulletListItem(2,
                action.formatValueForMetric(Baseline.INDIRECT, Metric.LINE));
        var indirectCoverageChangesBranch = getBulletListItem(2,
                action.formatValueForMetric(Baseline.INDIRECT, Metric.BRANCH));
        var indirectCoverageChangesLoc = getBulletListItem(2,
                formatValueOfMetric(indirectlyChangedCoverage, Metric.LOC));

        return sectionHeader
                + projectCoverageHeader
                + projectCoverageLine
                + projectCoverageBranch
                + projectCoverageComplexity
                + projectCoverageLoc
                + changedFilesCoverageHeader
                + changedFilesCoverageLine
                + changedFilesCoverageBranch
                + changedFilesCoverageComplexity
                + changedFilesCoverageLoc
                + changeCoverageHeader
                + changeCoverageLine
                + changeCoverageBranch
                + changeCoverageLoc
                + indirectCoverageChangesHeader
                + indirectCoverageChangesLine
                + indirectCoverageChangesBranch
                + indirectCoverageChangesLoc;
    }

    /**
     * Checks overview regarding the quality gate status.
     *
     * @return the markdown string representing the status summary
     */
    // TODO: expand with summary of status of each defined quality gate
    private String getHealthReportSummary() {
        return getSectionHeader(2, Messages.Checks_QualityGates(action.getQualityGateResult().toString()));
    }

    private String getProjectMetricsSummary(final Node result) {
        String sectionHeader = getSectionHeader(2, Messages.Checks_ProjectOverview());

        List<String> coverageDisplayNames = formatter.getSortedCoverageDisplayNames();
        String header = formatRow(coverageDisplayNames);
        String headerSeparator = formatRow(
                getTableSeparators(ColumnAlignment.CENTER, coverageDisplayNames.size()));

        String projectCoverageName = String.format("|%s **%s**", Icon.WHITE_CHECK_MARK.markdown,
                formatter.getDisplayName(Baseline.PROJECT));
        List<String> projectCoverage = formatter.getFormattedValues(formatter.getSortedCoverageValues(result),
                Functions.getCurrentLocale());
        String projectCoverageRow = projectCoverageName + formatRow(projectCoverage);

        String projectCoverageDeltaName = String.format("|%s **%s**", Icon.CHART_UPWARDS_TREND.markdown,
                formatter.getDisplayName(Baseline.PROJECT_DELTA));
        Collection<String> projectCoverageDelta = formatCoverageDelta(Metric.getCoverageMetrics(),
                action.getAllDeltas(Baseline.PROJECT_DELTA));
        String projectCoverageDeltaRow =
                projectCoverageDeltaName + formatRow(projectCoverageDelta);

        return sectionHeader
                + header
                + headerSeparator
                + projectCoverageRow
                + projectCoverageDeltaRow;
    }

    private String formatCoverageForMetric(final Metric metric,
            final BiFunction<Baseline, Metric, String> coverageFormat,
            final Baseline baseline) {
        return String.format("%s (%s)",
                coverageFormat.apply(baseline, metric), action.formatDelta(baseline, metric));
    }

    private String formatValueOfMetric(final Node root, final Metric metric) {
        var value = root.getValue(metric);
        return value.map(action::formatValueWithDetails)
                .orElseGet(() -> formatter.getDisplayName(metric) + ": " + Messages.Coverage_Not_Available());
    }

    private String formatText(final TextFormat format, final String text) {
        switch (format) {
            case BOLD:
                return "**" + text + "**";
            case CURSIVE:
                return "_" + text + "_";
            default:
                return text;
        }
    }

    /**
     * Formats the passed delta computation to a collection of its display representations, which is sorted by the
     * metric ordinal. Also, a collection of required metrics is passed. This is used to fill not existent metrics which
     * are required for the representation. Coverage deltas might not be existent if the reference does not contain a
     * reference value of the metric.
     *
     * @param requiredMetrics
     *         The metrics which should be displayed
     * @param deltas
     *         The delta calculation mapped by their metric
     */
    private Collection<String> formatCoverageDelta(final Collection<Metric> requiredMetrics,
            final NavigableMap<Metric, Fraction> deltas) {
        var coverageDelta = new TreeMap<Metric, String>();
        for (Metric metric : requiredMetrics) {
            if (deltas.containsKey(metric)) {
                var coverage = deltas.get(metric);
                coverageDelta.putIfAbsent(metric,
                        formatter.formatDelta(coverage, metric, Functions.getCurrentLocale())
                                + getTrendIcon(coverage.doubleValue()));
            }
            else {
                coverageDelta.putIfAbsent(metric,
                        formatter.formatPercentage(Coverage.nullObject(metric), Functions.getCurrentLocale()));
            }
        }
        return coverageDelta.values();
    }

    private String getTrendIcon(final double trend) {
        if (trend > 0) {
            return " " + Icon.ARROW_UP.markdown;
        }
        else if (trend < 0) {
            return " " + Icon.ARROW_DOWN.markdown;
        }
        else {
            return " " + Icon.ARROW_RIGHT.markdown;
        }
    }

    private List<String> getTableSeparators(final ColumnAlignment alignment, final int count) {
        switch (alignment) {
            case LEFT:
                return Collections.nCopies(count, ":---");
            case RIGHT:
                return Collections.nCopies(count, "---:");
            case CENTER:
            default:
                return Collections.nCopies(count, ":---:");
        }
    }

    private String getBulletListItem(final int level, final String text) {
        int whitespaces = (level - 1) * 2;
        return String.join("", Collections.nCopies(whitespaces, " ")) + "* " + text + "\n";
    }

    private String getUrlText(final String text, final String url) {
        return String.format("[%s](%s)", text, url);
    }

    private String formatRow(final Collection<String> columns) {
        StringBuilder row = new StringBuilder();
        for (Object column : columns) {
            row.append(String.format("|%s", column));
        }
        if (columns.size() > 0) {
            row.append('|');
        }
        row.append('\n');
        return row.toString();
    }

    private String getSectionHeader(final int level, final String text) {
        return String.join("", Collections.nCopies(level, "#")) + " " + text + "\n\n";
    }

    private ChecksConclusion getCheckConclusion(final QualityGateStatus status) {
        switch (status) {
            case INACTIVE:
            case PASSED:
                return ChecksConclusion.SUCCESS;
            case FAILED:
            case WARNING:
                return ChecksConclusion.FAILURE;
            default:
                throw new IllegalArgumentException("Unsupported quality gate status: " + status);
        }
    }

    private enum ColumnAlignment {
        CENTER,
        LEFT,
        RIGHT
    }

    private enum Icon {
        WHITE_CHECK_MARK(":white_check_mark:"),
        CHART_UPWARDS_TREND(":chart_with_upwards_trend:"),
        ARROW_UP(":arrow_up:"),
        ARROW_RIGHT(":arrow_right:"),
        ARROW_DOWN(":arrow_down:");

        private final String markdown;

        Icon(final String markdown) {
            this.markdown = markdown;
        }
    }

    private enum TextFormat {
        BOLD,
        CURSIVE
    }

    private static class AggregatedMissingLines {
        private final int startLine;
        private int endLine;

        private AggregatedMissingLines(final int startLine) {
            this.startLine = startLine;
            this.endLine = startLine;
        }

        private void increaseEndLine() {
            endLine++;
        }
    }
}
