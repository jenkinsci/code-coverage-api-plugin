package io.jenkins.plugins.coverage.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.StaplerProxy;
import hudson.Functions;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageXmlStream.MetricFractionMapConverter;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.AbstractXmlStream;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JenkinsFacade;

import static hudson.model.Run.*;

/**
 * Controls the life cycle of the coverage results in a job. This action persists the results of a build and displays a
 * summary on the build page. The actual visualization of the results is defined in the matching {@code summary.jelly}
 * file. This action also provides access to the coverage details: these are rendered using a new view instance.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
public class CoverageBuildAction extends BuildAction<Node> implements StaplerProxy {
    /** The coverage report symbol from the Ionicons plugin. */
    public static final String ICON = "symbol-footsteps-outline plugin-ionicons-api";

    private static final long serialVersionUID = -6023811049340671399L;

    private static final String NO_REFERENCE_BUILD = "-";

    private static final ElementFormatter FORMATTER = new ElementFormatter();

    private final String id;
    private final String name;

    private final String referenceBuildId;

    private final QualityGateStatus qualityGateStatus;

    // FIXME: Rethink if we need a separate result object that stores all data?
    private final FilteredLog log;

    /** The aggregated values of the result for the root of the tree. */
    private final List<? extends Value> projectValues;

    /** The delta of this build's coverages with respect to the reference build. */
    private final NavigableMap<Metric, Fraction> difference;

    /** The coverages filtered by changed lines of the associated change request. */
    private final List<? extends Value> changeCoverage;

    /** The delta of the coverages of the associated change request with respect to the reference build. */
    private final NavigableMap<Metric, Fraction> changeCoverageDifference;

    /** The indirect coverage changes of the associated change request with respect to the reference build. */
    private final List<? extends Value> indirectCoverageChanges;

    static {
        CoverageXmlStream.registerConverters(XSTREAM2);
        XSTREAM2.registerLocalConverter(CoverageBuildAction.class, "difference", new MetricFractionMapConverter());
        XSTREAM2.registerLocalConverter(CoverageBuildAction.class, "changeCoverageDifference",
                new MetricFractionMapConverter());
    }

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param id
     *         ID (URL) of the results
     * @param optionalName
     *         optional name that overrides the default name of the results
     * @param result
     *         the coverage tree as result to persist with this action
     * @param qualityGateStatus
     *         status of the quality gates
     * @param log
     *         the logging statements of the recording step
     */
    public CoverageBuildAction(final Run<?, ?> owner,
            final String id, final String optionalName,
            final Node result, final QualityGateStatus qualityGateStatus, final FilteredLog log) {
        this(owner, id, optionalName, result, qualityGateStatus, log, NO_REFERENCE_BUILD,
                new TreeMap<>(), List.of(), new TreeMap<>(), List.of());
    }

    @VisibleForTesting
    CoverageBuildAction(final Run<?, ?> owner,
            final Node result, final QualityGateStatus qualityGateStatus, final FilteredLog log) {
        this(owner, CoverageRecorder.DEFAULT_ID, StringUtils.EMPTY, result, qualityGateStatus, log, NO_REFERENCE_BUILD,
                new TreeMap<>(), List.of(), new TreeMap<>(), List.of());
    }

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param id
     *         ID (URL) of the results
     * @param optionalName
     *         optional name that overrides the default name of the results
     * @param result
     *         the coverage tree as result to persist with this action
     * @param qualityGateStatus
     *         status of the quality gates
     * @param log
     *         the logging statements of the recording step
     * @param referenceBuildId
     *         the ID of the reference build
     * @param delta
     *         delta of this build's coverages with respect to the reference build
     * @param changeCoverage
     *         the coverages filtered by changed lines of the associated change request
     * @param changeCoverageDifference
     *         the delta of the coverages of the associated change request with respect to the reference build
     * @param indirectCoverageChanges
     *         the indirect coverage changes of the associated change request with respect to the reference build
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public CoverageBuildAction(final Run<?, ?> owner,
            final String id, final String optionalName,
            final Node result, final QualityGateStatus qualityGateStatus, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final List<? extends Value> indirectCoverageChanges) {
        this(owner, id, optionalName, result, qualityGateStatus, log, referenceBuildId, delta, changeCoverage,
                changeCoverageDifference, indirectCoverageChanges, true);
    }

    @VisibleForTesting
    CoverageBuildAction(final Run<?, ?> owner,
            final Node result, final QualityGateStatus qualityGateStatus, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final List<? extends Value> indirectCoverageChanges) {
        this(owner, CoverageRecorder.DEFAULT_ID, StringUtils.EMPTY, result, qualityGateStatus, log, referenceBuildId, delta, changeCoverage,
                changeCoverageDifference, indirectCoverageChanges, true);
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageBuildAction(final Run<?, ?> owner,
            final String id, final String name,
            final Node result, final QualityGateStatus qualityGateStatus, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final List<? extends Value> indirectCoverageChanges,
            final boolean canSerialize) {
        super(owner, result, canSerialize);

        this.id = id;
        this.name = name;
        this.log = log;

        projectValues = result.aggregateValues();
        this.qualityGateStatus = qualityGateStatus;
        difference = delta;
        this.changeCoverage = new ArrayList<>(changeCoverage);
        this.changeCoverageDifference = changeCoverageDifference;
        this.indirectCoverageChanges = new ArrayList<>(indirectCoverageChanges);
        this.referenceBuildId = referenceBuildId;
    }

    /**
     * Returns the actual name of the tool. If no user defined name is given, then the default name is returned.
     *
     * @return the name
     */
    private String getActualName() {
        return StringUtils.defaultIfBlank(name, Messages.Coverage_Link_Name());
    }

    public FilteredLog getLog() {
        return log;
    }

    public QualityGateStatus getQualityGateStatus() {
        return qualityGateStatus;
    }

    /**
     * Returns the supported baselines.
     *
     * @return all supported baselines
     */
    @SuppressWarnings("unused") // Called by jelly view
    public List<Baseline> getBaselines() {
        return List.of(Baseline.PROJECT, Baseline.CHANGE, Baseline.INDIRECT);
    }

    /**
     * Returns whether a delta metric for the specified metric exists.
     *
     * @param baseline
     *         the baseline to use
     *
     * @return {@code true} if a delta is available for the specified metric, {@code false} otherwise
     */
    @SuppressWarnings("unused") // Called by jelly view
    public boolean hasBaselineResult(final Baseline baseline) {
        if (baseline == Baseline.CHANGE) {
            return hasChangeCoverage();
        }
        if (baseline == Baseline.INDIRECT) {
            return hasIndirectCoverageChanges();
        }
        return true;
    }

    /**
     * Returns the associate delta baseline for the specified baseline.
     *
     * @param baseline
     *         the baseline to get the delta baseline for
     *
     * @return the delta baseline
     * @throws NoSuchElementException
     *         if this baseline does not provide a delta baseline
     */
    public Baseline getDeltaBaseline(final Baseline baseline) {
        if (baseline == Baseline.PROJECT) {
            return Baseline.PROJECT_DELTA;
        }
        if (baseline == Baseline.CHANGE) {
            return Baseline.CHANGE_DELTA;
        }
        if (baseline == Baseline.FILE) {
            return Baseline.FILE_DELTA;
        }
        throw new NoSuchElementException("No delta baseline for this baseline: " + baseline);
    }

    /**
     * Returns all available values for the specified baseline.
     *
     * @param baseline
     *         the baseline to get the values for
     *
     * @return the available values
     * @throws NoSuchElementException
     *         if this baseline does not provide values
     */
    // Called by jelly view
    public List<Value> getAllValues(final Baseline baseline) {
        return getValueStream(baseline).collect(Collectors.toList());
    }

    /**
     * Returns all important values for the specified baseline.
     *
     * @param baseline
     *         the baseline to get the values for
     *
     * @return the available values
     * @throws NoSuchElementException
     *         if this baseline does not provide values
     */
    // Called by jelly view
    public List<Value> getValues(final Baseline baseline) {
        return filterImportantMetrics(getValueStream(baseline));
    }

    private List<Value> filterImportantMetrics(final Stream<? extends Value> values) {
        return values.filter(v -> getMetricsForSummary().contains(v.getMetric()))
                .collect(Collectors.toList());
    }

    private Stream<? extends Value> getValueStream(final Baseline baseline) {
        if (baseline == Baseline.PROJECT) {
            return projectValues.stream();
        }
        if (baseline == Baseline.CHANGE) {
            return changeCoverage.stream();
        }
        if (baseline == Baseline.INDIRECT) {
            return indirectCoverageChanges.stream();
        }
        throw new NoSuchElementException("No such baseline: " + baseline);
    }

    @SuppressWarnings("unused") // Called by jelly view
    public String getTooltip(final Baseline baseline) {
        var values = getValueStream(baseline).map(v -> FORMATTER.format(v, Functions.getCurrentLocale()))
                .collect(Collectors.joining("\n", "- ", ""));

        return "All <b>Available Values</b>:\n" + values;
    }

    /**
     * Returns a formatted and localized String representation of the specified value.
     *
     * @param value
     *         the value to format
     *
     * @return the value formatted as a string
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatValue(final Value value) {
        return FORMATTER.getDisplayName(value.getMetric()) + ": "
                + FORMATTER.format(value, Functions.getCurrentLocale());
    }

    /**
     * Returns a formatted and localized String representation of the specified value.
     *
     * @param value
     *         the value to format
     *
     * @return the value formatted as a string
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatValueWithDetails(final Value value) {
        return FORMATTER.getDisplayName(value.getMetric()) + ": "
                + FORMATTER.formatDetails(value, Functions.getCurrentLocale());
    }

    /**
     * Returns whether a delta metric for the specified baseline exists.
     *
     * @param baseline
     *         the baseline to use
     *
     * @return {@code true} if a delta is available for the specified baseline, {@code false} otherwise
     */
    public boolean hasDelta(final Baseline baseline) {
        return baseline == Baseline.PROJECT || baseline == Baseline.CHANGE;
    }

    /**
     * Returns whether a delta metric for the specified metric exists.
     *
     * @param baseline
     *         the baseline to use
     * @param metric
     *         the metric to check
     *
     * @return {@code true} if a delta is available for the specified metric, {@code false} otherwise
     */
    public boolean hasDelta(final Baseline baseline, final Metric metric) {
        if (baseline == Baseline.PROJECT) {
            return difference.containsKey(metric);
        }
        if (baseline == Baseline.CHANGE) {
            return changeCoverageDifference.containsKey(metric)
                    && Set.of(Metric.BRANCH, Metric.LINE).contains(metric);
        }
        if (baseline == Baseline.INDIRECT) {
            return false;
        }
        throw new NoSuchElementException("No such baseline: " + baseline);
    }

    /**
     * Returns a formatted and localized String representation of the delta for the specified metric (with respect to
     * the given baseline).
     *
     * @param baseline
     *         the baseline to use
     * @param metric
     *         the metric to get the delta for
     *
     * @return the delta metric
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatDelta(final Baseline baseline, final Metric metric) {
        if (baseline == Baseline.PROJECT) {
            if (hasDelta(baseline, metric)) {
                return FORMATTER.formatDelta(metric, difference.get(metric),
                        Functions.getCurrentLocale());
            }
        }
        if (baseline == Baseline.CHANGE) {
            if (hasDelta(baseline, metric)) {
                return FORMATTER.formatDelta(metric, changeCoverageDifference.get(metric),
                        Functions.getCurrentLocale());
            }
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Returns the visible metrics for the project summary.
     *
     * @return the metrics to be shown in the project summary
     */
    @VisibleForTesting
    NavigableSet<Metric> getMetricsForSummary() {
        return new TreeSet<>(Set.of(Metric.LINE, Metric.LOC, Metric.BRANCH, Metric.COMPLEXITY_DENSITY));
    }

    public Coverage getLineCoverage() {
        return (Coverage) getCoverage(Metric.LINE);
    }

    public Coverage getBranchCoverage() {
        return (Coverage) getCoverage(Metric.BRANCH);
    }

    /**
     * Returns whether a {@link Coverage} for the specified metric exists.
     *
     * @param metric
     *         the coverage metric
     *
     * @return {@code true} if a coverage is available for the specified metric, {@code false} otherwise
     */
    public boolean hasCoverage(final Metric metric) {
        return containsMetric(metric, projectValues);
    }

    private boolean containsMetric(final Metric metric, final List<? extends Value> values) {
        return values.stream()
                .map(Value::getMetric)
                .anyMatch(metric::equals);
    }

    /**
     * Returns the {@link Coverage} for the specified metric.
     *
     * @param metric
     *         the coverage metric
     *
     * @return the coverage
     */
    public Value getCoverage(final Metric metric) {
        return Value.getValue(metric, projectValues);
    }

    /**
     * Returns whether a change coverage exists at all.
     *
     * @return {@code true} if the change coverage exist, else {@code false}
     */
    public boolean hasChangeCoverage() {
        return hasChangeCoverage(Metric.LINE) || hasChangeCoverage(Metric.BRANCH);
    }

    /**
     * Returns whether a change coverage exists for the passed {@link Metric}.
     *
     * @param metric
     *         The coverage metric
     *
     * @return {@code true} if the change coverage exist for the metric, else {@code false}
     */
    public boolean hasChangeCoverage(final Metric metric) {
        return containsMetric(metric, changeCoverage);
    }

    /**
     * Gets the {@link Coverage change coverage} for the passed metric.
     *
     * @param metric
     *         The coverage metric
     *
     * @return the change coverage
     */
    public Value getChangeCoverage(final Metric metric) {
        return Value.getValue(metric, changeCoverage);
    }

    /**
     * Returns whether indirect coverage changes exist at all.
     *
     * @return {@code true} if indirect coverage changes exist, else {@code false}
     */
    public boolean hasIndirectCoverageChanges() {
        return hasIndirectCoverageChanges(Metric.LINE) || hasIndirectCoverageChanges(Metric.BRANCH);
    }

    /**
     * Returns whether indirect coverage changes exist for the passed {@link Metric}.
     *
     * @param metric
     *         The coverage metric
     *
     * @return {@code true} if indirect coverage changes exist for the metric, else {@code false}
     */
    public boolean hasIndirectCoverageChanges(final Metric metric) {
        return containsMetric(metric, indirectCoverageChanges);
    }

    /**
     * Gets the {@link Coverage indirect coverage changes} for the passed metric.
     *
     * @param metric
     *         The coverage metric
     *
     * @return the indirect coverage changes
     */
    public Value getIndirectCoverageChanges(final Metric metric) {
        return Value.getValue(metric, indirectCoverageChanges);
    }

    /**
     * Returns the possible reference build that has been used to compute the coverage delta.
     *
     * @return the reference build, if available
     */
    public Optional<Run<?, ?>> getReferenceBuild() {
        if (NO_REFERENCE_BUILD.equals(referenceBuildId)) {
            return Optional.empty();
        }
        return new JenkinsFacade().getBuild(referenceBuildId);
    }

    /**
     * Renders the reference build as HTML link.
     *
     * @return the reference build
     * @see #getReferenceBuild()
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getReferenceBuildLink() {
        return ReferenceBuild.getReferenceBuildLink(referenceBuildId);
    }

    /**
     * Returns the delta metrics, i.e. the coverage results of the current build minus the same results of the reference
     * build.
     *
     * @return the delta for each available coverage metric
     */
    @SuppressWarnings("unused") // Called by jelly view
    public NavigableMap<Metric, Fraction> getDelta() {
        return difference;
    }

    /**
     * Returns whether a delta metric for the specified metric exist.
     *
     * @param metric
     *         the metric to check
     *
     * @return {@code true} if a delta is available for the specified metric
     */
    public boolean hasDelta(final Metric metric) {
        return difference.containsKey(metric);
    }

    /**
     * Returns a formatted and localized String representation of the delta for the specified metric (with respect to
     * the reference build).
     *
     * @param metric
     *         the metric to get the delta for
     *
     * @return the delta metric
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatDelta(final Metric metric) {
        if (hasDelta(metric)) {
            return FORMATTER.formatDelta(metric, difference.get(metric), Functions.getCurrentLocale());
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Returns a formatted and localized String representation of the change coverage for the specified metric (with
     * respect to the reference build).
     *
     * @param metric
     *         the metric to get the change coverage for
     *
     * @return the change coverage metric
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatChangeCoverage(final Metric metric) {
        return formatCoverageForMetric(metric, changeCoverage);
    }

    /**
     * Returns a formatted and localized String representation of an overview of the indirect coverage changes (with
     * respect to the reference build).
     *
     * @param metric
     *         the metric to get the indirect coverage changes for
     *
     * @return the formatted representation of the indirect coverage changes
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatIndirectCoverageChanges(final Metric metric) {
        return formatCoverageForMetric(metric, indirectCoverageChanges);
    }

    /**
     * Returns a formatted and localized String representation of an overview of the passed coverage values.
     *
     * @param metric
     *         the metric to get the coverage for
     * @param values
     *         the coverage values of a specific type, mapped by their metrics
     *
     * @return the formatted text representation of the coverage value corresponding to the passed metric
     */
    private String formatCoverageForMetric(final Metric metric, final List<? extends Value> values) {
        var possibleValue = values.stream()
                .filter(v -> metric.equals(v.getMetric()))
                .findAny();
        return metric + ": "
                + possibleValue.map(v -> FORMATTER.format(v, Functions.getCurrentLocale()))
                .orElse(Messages.Coverage_Not_Available());
    }

    /**
     * Returns the change coverage delta for the passed metric, i.e. the coverage results of the current build minus the
     * same results of the reference build.
     *
     * @param Metric
     *         The change coverage metric
     *
     * @return the delta for each available coverage metric
     */
    public Fraction getChangeCoverageDifference(final Metric Metric) {
        return changeCoverageDifference.get(Metric);
    }

    /**
     * Returns whether a change coverage delta metric for the specified metric exist.
     *
     * @param metric
     *         the metric to check
     *
     * @return {@code true} if a delta is available for the specified metric
     */
    public boolean hasChangeCoverageDifference(final Metric metric) {
        return changeCoverageDifference.containsKey(metric);
    }

    /**
     * Checks whether any code changes have been detected no matter if the code coverage is affected or not.
     *
     * @return {@code true} whether code changes have been detected
     */
    @SuppressWarnings("unused") // Called by jelly view
    public boolean hasCodeChanges() {
        return getResult().hasChangedLines();
    }

    /**
     * Returns a formatted and localized String representation of the change coverage delta for the specified metric
     * (with respect to the reference build).
     *
     * @param metric
     *         the metric to get the delta for
     *
     * @return the delta metric
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatChangeCoverageDifference(final Metric metric) {
        if (hasChangeCoverage(metric)) {
            return FORMATTER.formatDelta(metric, changeCoverageDifference.get(metric), Functions.getCurrentLocale());
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Returns a formatted and localized String representation of an overview of how many lines and files are affected
     * by the change coverage (with respect to the reference build).
     *
     * @return the formatted representation of the change coverage
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatChangeCoverageOverview() {
        if (hasChangeCoverage()) {
            int fileAmount = getFileAmountWithChangedCoverage();
            long lineAmount = getLineAmountWithChangedCoverage();
            return getFormattedChangesOverview(lineAmount, fileAmount);
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Returns a formatted and localized String representation of an overview of how many lines and files are affected
     * by the indirect coverage changes (with respect to the reference build).
     *
     * @return the formatted representation of the indirect coverage changes
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatIndirectCoverageChangesOverview() {
        if (hasIndirectCoverageChanges()) {
            int fileAmount = getFileAmountWithIndirectCoverageChanges();
            long lineAmount = getLineAmountWithIndirectCoverageChanges();
            return getFormattedChangesOverview(lineAmount, fileAmount);
        }
        return Messages.Coverage_Not_Available();
    }

    public int getFileAmountWithChangedCoverage() {
        return extractFileNodesWithChangeCoverage().size();
    }

    public long getLineAmountWithChangedCoverage() {
        return extractFileNodesWithChangeCoverage().stream()
                .map(FileNode::getCoveredLinesOfChangeSet)
                .mapToLong(Collection::size)
                .sum();
    }

    private Set<FileNode> extractFileNodesWithChangeCoverage() {
        var allFileNodes = getResult().filterChanges().getAllFileNodes();
        return allFileNodes.stream()
                .filter(FileNode::hasCoveredLinesInChangeSet)
                .collect(Collectors.toSet());
    }

    public int getFileAmountWithIndirectCoverageChanges() {
        return extractFileNodesWithIndirectCoverageChanges().size();
    }

    public long getLineAmountWithIndirectCoverageChanges() {
        return extractFileNodesWithIndirectCoverageChanges().stream()
                .map(node -> node.getIndirectCoverageChanges().values())
                .mapToLong(Collection::size)
                .sum();
    }

    private Set<FileNode> extractFileNodesWithIndirectCoverageChanges() {
        return getResult().filterByIndirectlyChangedCoverage().getAllFileNodes().stream()
                .filter(FileNode::hasIndirectCoverageChanges)
                .collect(Collectors.toSet());
    }

    /**
     * Gets a formatted String representation of an overview of how many lines in how many files changed.
     *
     * @param lineAmount
     *         The amount of lines
     * @param fileAmount
     *         The amount of files
     *
     * @return the formatted string
     */
    private String getFormattedChangesOverview(final long lineAmount, final int fileAmount) {
        String affected = "is affected";
        String line = "line";
        if (lineAmount > 1) {
            line = "lines";
            affected = "are affected";
        }
        String file = "file";
        if (fileAmount > 1) {
            file = "files";
        }
        return String.format("%d %s (%d %s) %s", lineAmount, line, fileAmount, file, affected);
    }

    /**
     * Returns a formatted and localized String representation of the coverage percentage for the specified metric (with
     * respect to the reference build).
     *
     * @param metric
     *         the metric to get the coverage percentage for
     *
     * @return the delta metric
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String formatCoverage(final Metric metric) {
        return FORMATTER.getDisplayName(metric) + ": "
                + FORMATTER.format(getCoverage(metric), Functions.getCurrentLocale());
    }

    @Override
    protected AbstractXmlStream<Node> createXmlStream() {
        return new CoverageXmlStream();
    }

    @Override
    protected CoverageJobAction createProjectAction() {
        return new CoverageJobAction(getOwner().getParent());
    }

    @Override
    protected String getBuildResultBaseName() {
        return "coverage.xml";
    }

    @Override
    public CoverageViewModel getTarget() {
        return new CoverageViewModel(getOwner(), getUrlName(), name, getResult(), log);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return ICON;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return getActualName();
    }

    @NonNull
    @Override
    public String getUrlName() {
        return id;
    }
}
