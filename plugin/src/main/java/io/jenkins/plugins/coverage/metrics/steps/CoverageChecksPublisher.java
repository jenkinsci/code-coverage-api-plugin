package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

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
import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder.ChecksAnnotationScope;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.QualityGateStatus;

/**
 * Publishes coverage as Checks to SCM platforms.
 *
 * @author Florian Orendi
 */
class CoverageChecksPublisher {
    private static final ElementFormatter FORMATTER = new ElementFormatter();

    private final CoverageBuildAction action;
    private final JenkinsFacade jenkinsFacade;
    private final String checksName;
    private final ChecksAnnotationScope annotationScope;

    CoverageChecksPublisher(final CoverageBuildAction action, final String checksName,
            final ChecksAnnotationScope annotationScope) {
        this(action, checksName, annotationScope, new JenkinsFacade());
    }

    @VisibleForTesting
    CoverageChecksPublisher(final CoverageBuildAction action,
            final String checksName, final ChecksAnnotationScope annotationScope, final JenkinsFacade jenkinsFacade) {
        this.jenkinsFacade = jenkinsFacade;
        this.action = action;
        this.checksName = checksName;
        this.annotationScope = annotationScope;
    }

    /**
     * Publishes the coverage report as Checks to SCM platforms.
     *
     * @param listener
     *         The task listener
     */
    void publishCoverageReport(final TaskListener listener) {
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
        return String.format("%s: %s",
                FORMATTER.getDisplayName(Baseline.MODIFIED_LINES),
                action.formatValue(Baseline.MODIFIED_LINES, Metric.LINE));
    }

    private String getSummary() {
        var root = action.getResult();
        return getOverallCoverageSummary(root) + "\n\n"
                + getQualityGatesSummary() + "\n\n"
                + getProjectMetricsSummary(root);
    }

    private List<ChecksAnnotation> getAnnotations() {
        if (annotationScope == ChecksAnnotationScope.SKIP) {
            return List.of();
        }
        // TODO: check if it makes sense to add annotations for all lines
        var annotations = new ArrayList<ChecksAnnotation>();
        for (var fileNode : action.getResult().filterByModifiedLines().getAllFileNodes()) {
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
            var linesWithCoverage = fileNode.getLinesWithCoverage();
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

        var modifiedFilesCoverageRoot = root.filterByModifiedFiles();
        var modifiedLinesCoverageRoot = root.filterByModifiedLines();
        var indirectlyChangedCoverage = root.filterByIndirectChanges();

        var projectCoverageHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.PROJECT_DELTA.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.PROJECT_DELTA.getUrl())));
        var modifiedFilesCoverageHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.MODIFIED_FILES_DELTA.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.MODIFIED_FILES_DELTA.getUrl())));
        var modifiedLinesCoverageHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.MODIFIED_LINES_DELTA.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.MODIFIED_LINES_DELTA.getUrl())));
        var indirectCoverageChangesHeader = getBulletListItem(1,
                formatText(TextFormat.BOLD, getUrlText(Baseline.INDIRECT.getTitle(),
                        getCoverageReportBaseUrl() + Baseline.INDIRECT.getUrl())));

        var projectCoverageLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, Baseline.PROJECT));
        var projectCoverageBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, Baseline.PROJECT));
        var projectCoverageComplexity = getBulletListItem(2, formatRootValueOfMetric(root, Metric.COMPLEXITY_DENSITY));
        var projectCoverageLoc = getBulletListItem(2, formatRootValueOfMetric(root, Metric.LOC));

        var modifiedFilesCoverageLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, Baseline.MODIFIED_FILES));
        var modifiedFilesCoverageBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, Baseline.MODIFIED_FILES));
        var modifiedFilesCoverageComplexity = getBulletListItem(2,
                formatRootValueOfMetric(modifiedFilesCoverageRoot, Metric.COMPLEXITY_DENSITY));
        var modifiedFilesCoverageLoc = getBulletListItem(2,
                formatRootValueOfMetric(modifiedFilesCoverageRoot, Metric.LOC));

        var modifiedLinesCoverageLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, Baseline.MODIFIED_LINES));
        var modifiedLinesCoverageBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, Baseline.MODIFIED_LINES));
        var modifiedLinesCoverageLoc = getBulletListItem(2,
                formatRootValueOfMetric(modifiedLinesCoverageRoot, Metric.LOC));

        var indirectCoverageChangesLine = getBulletListItem(2,
                formatCoverageForMetric(Metric.LINE, Baseline.INDIRECT));
        var indirectCoverageChangesBranch = getBulletListItem(2,
                formatCoverageForMetric(Metric.BRANCH, Baseline.INDIRECT));
        var indirectCoverageChangesLoc = getBulletListItem(2,
                formatRootValueOfMetric(indirectlyChangedCoverage, Metric.LOC));

        return sectionHeader
                + projectCoverageHeader
                + projectCoverageLine
                + projectCoverageBranch
                + projectCoverageComplexity
                + projectCoverageLoc
                + modifiedFilesCoverageHeader
                + modifiedFilesCoverageLine
                + modifiedFilesCoverageBranch
                + modifiedFilesCoverageComplexity
                + modifiedFilesCoverageLoc
                + modifiedLinesCoverageHeader
                + modifiedLinesCoverageLine
                + modifiedLinesCoverageBranch
                + modifiedLinesCoverageLoc
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
    private String getQualityGatesSummary() {
        return getSectionHeader(2, Messages.Checks_QualityGates(action.getQualityGateResult().getOverallStatus().name()));
    }

    private String getProjectMetricsSummary(final Node result) {
        String sectionHeader = getSectionHeader(2, Messages.Checks_ProjectOverview());

        List<String> coverageDisplayNames = FORMATTER.getSortedCoverageDisplayNames();
        String header = formatRow(coverageDisplayNames);
        String headerSeparator = formatRow(
                getTableSeparators(ColumnAlignment.CENTER, coverageDisplayNames.size()));

        String projectCoverageName = String.format("|%s **%s**", Icon.WHITE_CHECK_MARK.markdown,
                FORMATTER.getDisplayName(Baseline.PROJECT));
        List<String> projectCoverage = FORMATTER.getFormattedValues(FORMATTER.getSortedCoverageValues(result),
                Functions.getCurrentLocale());
        String projectCoverageRow = projectCoverageName + formatRow(projectCoverage);

        String projectCoverageDeltaName = String.format("|%s **%s**", Icon.CHART_UPWARDS_TREND.markdown,
                FORMATTER.getDisplayName(Baseline.PROJECT_DELTA));
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

    private String formatCoverageForMetric(final Metric metric, final Baseline baseline) {
        return String.format("%s: %s / %s", FORMATTER.getDisplayName(metric),
                action.formatValue(baseline, metric), action.formatDelta(baseline, metric));
    }

    private String formatRootValueOfMetric(final Node root, final Metric metric) {
        var value = root.getValue(metric);
        return value.map(FORMATTER::formatValueWithMetric)
                .orElseGet(() -> FORMATTER.getDisplayName(metric) + ": " + Messages.Coverage_Not_Available());
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
                        FORMATTER.formatDelta(coverage, metric, Functions.getCurrentLocale())
                                + getTrendIcon(coverage.doubleValue()));
            }
            else {
                coverageDelta.putIfAbsent(metric,
                        FORMATTER.formatPercentage(Coverage.nullObject(metric), Functions.getCurrentLocale()));
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
