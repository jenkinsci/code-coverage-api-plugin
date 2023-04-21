package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.VisibleForTesting;

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
    private static final int TITLE_HEADER_LEVEL = 4;
    private static final char NEW_LINE = '\n';
    private static final String COLUMN = "|";

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
                .withText(getProjectMetricsSummary())
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
        return getMetricsForTitle().stream()
                .map(this::format)
                .flatMap(Optional::stream)
                .collect(Collectors.joining(", "));
    }

    private Optional<String> format(final Metric metric) {
        var baseline = selectBaseline();
        return action.getValueForMetric(baseline, metric)
                .map(value -> formatValue(baseline, metric, value));

    }

    private Baseline selectBaseline() {
        if (action.hasBaselineResult(Baseline.MODIFIED_LINES)) {
            return Baseline.MODIFIED_LINES;
        }
        return Baseline.PROJECT;
    }

    private String formatValue(final Baseline baseline, final Metric metric, final Value value) {
        return String.format("%s: %s%s",
                FORMATTER.getDisplayName(metric), FORMATTER.format(value), getDeltaDetails(baseline, metric));
    }

    private String getDeltaDetails(final Baseline baseline, final Metric metric) {
        if (action.hasDelta(baseline, metric)) {
            return String.format(" (%s)", action.formatDelta(baseline, metric));
        }
        return StringUtils.EMPTY;
    }

    private NavigableSet<Metric> getMetricsForTitle() {
        return new TreeSet<>(
                Set.of(Metric.LINE, Metric.BRANCH, Metric.MUTATION));
    }

    private String getSummary() {
        return getAnnotationSummary()
                + getOverallCoverageSummary()
                + getQualityGatesSummary();
    }

    private String getAnnotationSummary() {
        if (rootNode.hasModifiedLines()) {
            var filteredRoot = rootNode.filterByModifiedLines();
            var modifiedFiles = filteredRoot.getAllFileNodes();

            var summary = new StringBuilder("#### Summary for modified lines\n");

            createTotalLinesSummary(modifiedFiles, summary);
            createLineCoverageSummary(modifiedFiles, summary);
            createBranchCoverageSummary(filteredRoot, modifiedFiles, summary);
            createMutationCoverageSummary(filteredRoot, modifiedFiles, summary);

            return summary.toString();
        }
        return StringUtils.EMPTY;
    }

    private void createTotalLinesSummary(final List<FileNode> modifiedFiles, final StringBuilder summary) {
        var total = countLines(modifiedFiles, FileNode::getModifiedLines);
        if (total == 1) {
            summary.append("- 1 line has been modified");
        }
        else {
            summary.append(String.format("- %d lines have been modified", total));
        }
        summary.append(NEW_LINE);
    }

    private int countLines(final List<FileNode> modifiedFiles, final Function<FileNode, Collection<?>> linesGetter) {
        return modifiedFiles.stream().map(linesGetter).mapToInt(Collection::size).sum();
    }

    private void createLineCoverageSummary(final List<FileNode> modifiedFiles, final StringBuilder summary) {
        var missed = countLines(modifiedFiles, FileNode::getMissedLines);
        if (missed == 0) {
            summary.append("- all lines are covered");
        }
        else if (missed == 1) {
            summary.append("- 1 line is not covered");
        }
        else {
            summary.append(String.format("- %d lines are not covered", missed));
        }
        summary.append(NEW_LINE);
    }

    private void createBranchCoverageSummary(final Node filteredRoot, final List<FileNode> modifiedFiles,
            final StringBuilder summary) {
        if (filteredRoot.containsMetric(Metric.BRANCH)) {
            var partiallyCovered = modifiedFiles.stream()
                    .map(FileNode::getPartiallyCoveredLines)
                    .map(Map::size)
                    .count();
            if (partiallyCovered == 1) {
                summary.append("- 1 line is covered only partially");
            }
            else {
                summary.append(String.format("- %d lines are covered only partially", partiallyCovered));
            }
            summary.append(NEW_LINE);
        }
    }

    private void createMutationCoverageSummary(final Node filteredRoot, final List<FileNode> modifiedFiles,
            final StringBuilder summary) {
        if (filteredRoot.containsMetric(Metric.MUTATION)) {
            var survived = modifiedFiles.stream()
                    .map(FileNode::getSurvivedMutations)
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .map(Entry::getValue)
                    .count();
            var mutations = countLines(modifiedFiles, FileNode::getMutations);
            if (survived == 0) {
                if (mutations == 1) {
                    summary.append("- 1 mutation has been killed");
                }
                else {
                    summary.append(String.format("- all %d mutations have been killed", mutations));
                }
            }
            else if (survived == 1) {
                summary.append(String.format("- 1 mutation survived (of %d)", mutations));
            }
            else {
                summary.append(String.format("- %d mutations survived (of %d)", survived, mutations));
            }
            summary.append(NEW_LINE);
        }
    }

    private List<ChecksAnnotation> getAnnotations() {
        if (annotationScope == ChecksAnnotationScope.SKIP) {
            return List.of();
        }

        var annotations = new ArrayList<ChecksAnnotation>();
        for (var fileNode : filterAnnotations().getAllFileNodes()) {
            annotations.addAll(getMissingLines(fileNode));
            annotations.addAll(getPartiallyCoveredLines(fileNode));
            annotations.addAll(getSurvivedMutations(fileNode));
        }
        return annotations;
    }

    private Node filterAnnotations() {
        if (annotationScope == ChecksAnnotationScope.ALL_LINES) {
            return rootNode;
        }
        else {
            return rootNode.filterByModifiedLines();
        }
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
                        .withRawDetails(createMutationDetails(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private String createMutationDetails(final List<Mutation> entry) {
        var details = new StringBuilder("Survived mutations:\n");
        entry.forEach(mutation -> details.append(
                String.format("- %s (%s)\n", mutation.getDescription(), mutation.getMutator())));
        return details.toString();
    }

    private String createMutationMessage(final int line, final List<Mutation> survived) {
        if (survived.size() == 1) {
            return "One mutation survived in line " + line;
        }
        return String.format("%d mutations survived in line %d", survived.size(), line);
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
            return String.format("Line %d is only partially covered, one branch is missing", line);

        }
        return String.format("Line %d is only partially covered, %d branches are missing", line, missed);
    }

    private ChecksAnnotationBuilder createAnnotationBuilder(final FileNode fileNode) {
        return new ChecksAnnotationBuilder()
                .withPath(fileNode.getRelativePath())
                .withAnnotationLevel(ChecksAnnotationLevel.WARNING);
    }

    private String getBaseUrl() {
        return jenkinsFacade.getAbsoluteUrl(action.getOwner().getUrl(), action.getUrlName());
    }

    private List<Baseline> getBaselines() {
        return List.of(Baseline.PROJECT, Baseline.MODIFIED_FILES, Baseline.MODIFIED_LINES, Baseline.INDIRECT);
    }

    private String getOverallCoverageSummary() {
        StringBuilder description = new StringBuilder(getSectionHeader(TITLE_HEADER_LEVEL, "Overview by baseline"));

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
                    description.append(getBulletListItem(TITLE_HEADER_LEVEL, display));
                }
            }
        }
        description.append(NEW_LINE);
        return description.toString();
    }

    /**
     * Checks overview regarding the quality gate status.
     *
     * @return the markdown string representing the status summary
     */
    private String getQualityGatesSummary() {
        String summary = getSectionHeader(TITLE_HEADER_LEVEL, "Quality Gates Summary");
        var qualityGateResult = action.getQualityGateResult();
        if (qualityGateResult.isInactive()) {
            return summary + "No active quality gates.";
        }
        return summary
                + "Overall result: " + qualityGateResult.getOverallStatus().getDescription() + "\n"
                + qualityGateResult.getMessages().stream()
                .map(s -> s.replaceAll("-> ", ""))
                .map(s -> s.replaceAll("[\\[\\]]", ""))
                .collect(asSeparateLines());
    }

    private Collector<CharSequence, ?, String> asSeparateLines() {
        return Collectors.joining("\n- ", "- ", "\n");
    }

    private String getProjectMetricsSummary() {
        var builder = new StringBuilder(getSectionHeader(TITLE_HEADER_LEVEL, "Project coverage details"));
        builder.append(COLUMN);
        builder.append(COLUMN);
        builder.append(getMetricStream()
                .map(FORMATTER::getDisplayName)
                .collect(asColumn()));
        builder.append(COLUMN);
        builder.append(":---:");
        builder.append(COLUMN);
        builder.append(getMetricStream()
                .map(i -> ":---:")
                .collect(asColumn()));
        for (Baseline baseline : action.getBaselines()) {
            if (action.hasBaselineResult(baseline)) {
                builder.append(String.format("%s **%s**|", Icon.FEET.markdown,
                        FORMATTER.getDisplayName(baseline)));
                builder.append(getMetricStream()
                        .map(metric -> action.formatValue(baseline, metric))
                        .collect(asColumn()));

                var deltaBaseline = action.getDeltaBaseline(baseline);
                if (deltaBaseline != baseline) {
                    builder.append(String.format("%s **%s**|", Icon.CHART_UPWARDS_TREND.markdown,
                            FORMATTER.getDisplayName(deltaBaseline)));
                    builder.append(getMetricStream()
                            .map(metric -> getFormatDelta(baseline, metric))
                            .collect(asColumn()));
                }
            }
        }

        return builder.toString();
    }

    private String getFormatDelta(final Baseline baseline, final Metric metric) {
        var delta = action.formatDelta(baseline, metric);
        return delta + getTrendIcon(delta);
    }

    private Stream<Metric> getMetricStream() {
        return Metric.getCoverageMetrics().stream().skip(1);
    }

    private Collector<CharSequence, ?, String> asColumn() {
        return Collectors.joining(COLUMN, "", "\n");
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

    private String getTrendIcon(final String trend) {
        if (trend.startsWith("+")) {
            return " " + Icon.ARROW_UP.markdown;
        }
        else if (trend.startsWith("-")) {
            return " " + Icon.ARROW_DOWN.markdown;
        }
        else if (trend.startsWith("n/a")) {
            return StringUtils.EMPTY;
        }
        return " " + Icon.ARROW_RIGHT.markdown;
    }

    private String getBulletListItem(final int level, final String text) {
        int whitespaces = (level - 1) * TITLE_HEADER_LEVEL;
        return String.join("", Collections.nCopies(whitespaces, " ")) + "* " + text + "\n";
    }

    private String getUrlText(final String text, final String url) {
        return String.format("[%s](%s)", text, url);
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

    private enum Icon {
        FEET(":feet:"),
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
