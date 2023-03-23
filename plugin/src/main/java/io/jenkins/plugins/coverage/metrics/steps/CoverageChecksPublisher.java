package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
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
@SuppressWarnings("PMD.GodClass")
class CoverageChecksPublisher {
    private static final ElementFormatter FORMATTER = new ElementFormatter();

    private final CoverageBuildAction action;
    private final Node rootNode;
    private final JenkinsFacade jenkinsFacade;
    private final String checksName;
    private final ChecksAnnotationScope annotationScope;

    CoverageChecksPublisher(final CoverageBuildAction action, final Node rootNode, final String checksName,
            final ChecksAnnotationScope annotationScope) {
        this(action, rootNode, checksName, annotationScope, new JenkinsFacade());
    }

    @VisibleForTesting
    CoverageChecksPublisher(final CoverageBuildAction action, final Node rootNode, final String checksName,
            final ChecksAnnotationScope annotationScope, final JenkinsFacade jenkinsFacade) {
        this.rootNode = rootNode;
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
                .withDetailsURL(getBaseUrl())
                .withOutput(output)
                .build();
    }

    private String getChecksTitle() {
        Baseline baseline = selectBaseline();
        return getMetricsForTitle().stream()
                .filter(metric -> action.hasValue(baseline, metric))
                .map(metric -> format(baseline, metric))
                .collect(Collectors.joining(", ", "", "."));
    }

    private Baseline selectBaseline() {
        if (action.hasBaselineResult(Baseline.MODIFIED_LINES)) {
            return Baseline.MODIFIED_LINES;
        }
        else {
            return Baseline.PROJECT;
        }
    }

    private String format(final Baseline baseline, final Metric metric) {
        String suffix;
        if (action.hasDelta(baseline, metric)) {
            suffix = String.format(" (%s)", action.formatDelta(baseline, metric));
        }
        else {
            suffix = "";
        }
        return String.format("%s: %s%s",
                FORMATTER.getDisplayName(metric), action.formatValue(baseline, metric), suffix);
    }

    private NavigableSet<Metric> getMetricsForTitle() {
        return new TreeSet<>(
                Set.of(Metric.LINE, Metric.BRANCH, Metric.MUTATION));
    }

    private String getSummary() {
        var root = rootNode;
        return getOverallCoverageSummary(root) + "\n\n"
                + getQualityGatesSummary() + "\n\n"
                + getProjectMetricsSummary(root);
    }

    private List<ChecksAnnotation> getAnnotations() {
        if (annotationScope == ChecksAnnotationScope.SKIP) {
            return List.of();
        }

        var tree = rootNode;
        Node filtered;
        if (annotationScope == ChecksAnnotationScope.ALL_LINES) {
            filtered = tree;
        }
        else {
            filtered = tree.filterByModifiedLines();
        }

        var annotations = new ArrayList<ChecksAnnotation>();
        for (var fileNode : filtered.getAllFileNodes()) {
            annotations.addAll(getMissingLines(fileNode));
            annotations.addAll(getPartiallyCoveredLines(fileNode));
            annotations.addAll(getSurvivedMutations(fileNode));
        }
        return annotations;
    }

    private Collection<? extends ChecksAnnotation> getMissingLines(final FileNode fileNode) {
        var builder = createAnnotationBuilder(fileNode).withTitle("Not covered line");

        return fileNode.getMissedLines().stream()
                .map(line -> builder.withMessage("Line " + line + " is not covered by tests")
                        .withStartLine(line)
                        .withEndLine(line)
                        .build())
                .collect(Collectors.toList());
    }

    private Collection<? extends ChecksAnnotation> getSurvivedMutations(final FileNode fileNode) {
        var builder = createAnnotationBuilder(fileNode).withTitle("Mutation survived");

        return fileNode.getSurvivedMutations().entrySet().stream()
                .map(entry -> builder.withMessage(createMutationMessage(entry.getKey(), entry.getValue()))
                        .withStartLine(entry.getKey())
                        .withEndLine(entry.getKey())
                        .build())
                .collect(Collectors.toList());
    }

    private String createMutationMessage(final int line, final int survived) {
        if (survived == 1) {
            return "One mutation survived in line " + line;
        }
        return String.format("%d mutations survived in line %d", survived, line);
    }

    private Collection<? extends ChecksAnnotation> getPartiallyCoveredLines(final FileNode fileNode) {
        var builder = createAnnotationBuilder(fileNode).withTitle("Partially covered line");

        return fileNode.getPartiallyCoveredLines().entrySet().stream()
                .map(entry -> builder.withMessage(createBranchMessage(entry.getKey(), entry.getValue()))
                        .withStartLine(entry.getKey())
                        .withEndLine(entry.getKey())
                        .build())
                .collect(Collectors.toList());
    }

    private String createBranchMessage(final int line, final int missed) {
        if (missed == 1) {
            return "Line " + line + " is only partially covered, one branch is missing";

        }
        return "Line " + line + " is only partially covered, %d branches are missing.";
    }

    private ChecksAnnotationBuilder createAnnotationBuilder(final FileNode fileNode) {
        return new ChecksAnnotationBuilder()
                .withPath(fileNode.getPath())
                .withAnnotationLevel(ChecksAnnotationLevel.WARNING);
    }

    private String getBaseUrl() {
        return jenkinsFacade.getAbsoluteUrl(action.getOwner().getUrl(), action.getUrlName());
    }

    private List<Baseline> getBaselines() {
        return List.of(Baseline.PROJECT, Baseline.MODIFIED_FILES, Baseline.MODIFIED_LINES, Baseline.INDIRECT);
    }


    private String getOverallCoverageSummary(final Node root) {
        StringBuilder description = new StringBuilder(getSectionHeader(2, Messages.Checks_Summary()));

        for (Baseline baseline : getBaselines()) {
            if (action.hasBaselineResult(baseline)) {
                description.append(getBulletListItem(1,
                        formatText(TextFormat.BOLD,
                                getUrlText(action.getTitle(baseline), getBaseUrl() + baseline.getUrl()))));
                for (Value value : action.getValues(baseline)) {
                    String display = FORMATTER.formatDetailedValueWithMetric(value);
                    if (action.hasDelta(baseline, value.getMetric())) {
                        display += String.format(" - Delta: %s", action.formatDelta(baseline, value.getMetric()));
                    }
                    description.append(getBulletListItem(2, display));
                }
            }
        }
        return description.toString();
    }

    /**
     * Checks overview regarding the quality gate status.
     *
     * @return the markdown string representing the status summary
     */
    // TODO: expand with summary of status of each defined quality gate
    private String getQualityGatesSummary() {
        return getSectionHeader(2,
                Messages.Checks_QualityGates(action.getQualityGateResult().getOverallStatus().name()));
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
     * @return the delta for each metric to be shown in the MD file
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
        if (!columns.isEmpty()) {
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
}
