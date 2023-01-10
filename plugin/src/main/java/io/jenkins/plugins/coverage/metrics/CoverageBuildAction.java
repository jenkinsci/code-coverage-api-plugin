package io.jenkins.plugins.coverage.metrics;

import java.util.ArrayList;
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
    private static final long serialVersionUID = -6023811049340671399L;

    /** The coverage report symbol from the Ionicons plugin. */
    public static final String ICON = "symbol-footsteps-outline plugin-ionicons-api";

    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private static final String NO_REFERENCE_BUILD = "-";

    private final String id;
    private final String name;

    private final String referenceBuildId;

    private final QualityGateResult qualityGateResult;

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
        XSTREAM2.registerLocalConverter(CoverageBuildAction.class, "difference",
                new MetricFractionMapConverter());
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
     * @param qualityGateResult
     *         status of the quality gates
     * @param log
     *         the logging statements of the recording step
     */
    public CoverageBuildAction(final Run<?, ?> owner, final String id, final String optionalName,
            final Node result, final QualityGateResult qualityGateResult, final FilteredLog log) {
        this(owner, id, optionalName, result, qualityGateResult, log, NO_REFERENCE_BUILD,
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
     * @param qualityGateResult
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
    public CoverageBuildAction(final Run<?, ?> owner, final String id, final String optionalName,
            final Node result, final QualityGateResult qualityGateResult, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final List<? extends Value> indirectCoverageChanges) {
        this(owner, id, optionalName, result, qualityGateResult, log, referenceBuildId, delta, changeCoverage,
                changeCoverageDifference, indirectCoverageChanges, true);
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageBuildAction(final Run<?, ?> owner, final String id, final String name,
            final Node result, final QualityGateResult qualityGateResult, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final List<? extends Value> indirectCoverageChanges,
            final boolean canSerialize) {
        super(owner, result, false);

        this.id = id;
        this.name = name;
        this.log = log;

        projectValues = result.aggregateValues();
        this.qualityGateResult = qualityGateResult;
        difference = delta;
        this.changeCoverage = new ArrayList<>(changeCoverage);
        this.changeCoverageDifference = changeCoverageDifference;
        this.indirectCoverageChanges = new ArrayList<>(indirectCoverageChanges);
        this.referenceBuildId = referenceBuildId;

        if (canSerialize) {
            createXmlStream().write(owner.getRootDir().toPath().resolve(getBuildResultBaseName()), result);
        }
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

    public QualityGateResult getQualityGateResult() {
        return qualityGateResult;
    }

    public CoverageStatistics getStatistics() {
        return new CoverageStatistics(projectValues, difference, changeCoverage, changeCoverageDifference,
                List.of(), new TreeMap<>());
    }

    /**
     * Returns the supported baselines.
     *
     * @return all supported baselines
     */
    @SuppressWarnings("unused") // Called by jelly view
    public List<Baseline> getBaselines() {
        return List.of(Baseline.PROJECT, Baseline.MODIFIED_LINES, Baseline.INDIRECT);
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
        return !getValues(baseline).isEmpty();
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
    @SuppressWarnings("unused") // Called by jelly view
    public Baseline getDeltaBaseline(final Baseline baseline) {
        if (baseline == Baseline.PROJECT) {
            return Baseline.PROJECT_DELTA;
        }
        if (baseline == Baseline.MODIFIED_LINES) {
            return Baseline.MODIFIED_LINES_DELTA;
        }
        if (baseline == Baseline.MODIFIED_FILES) {
            return Baseline.MODIFIED_FILES_DELTA;
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
        if (baseline == Baseline.MODIFIED_LINES) {
            return changeCoverage.stream();
        }
        if (baseline == Baseline.INDIRECT) {
            return indirectCoverageChanges.stream();
        }
        throw new NoSuchElementException("No such baseline: " + baseline);
    }

    /**
     * Returns an HTML tooltip that renders all available values of the specified baseline.
     *
     * @param baseline
     *         the baseline to get the tooltip for
     *
     * @return the tooltip showing all available values
     */
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
    @SuppressWarnings("unused") // Called by jelly view
    public boolean hasDelta(final Baseline baseline) {
        return baseline == Baseline.PROJECT || baseline == Baseline.MODIFIED_LINES;
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
        if (baseline == Baseline.MODIFIED_LINES) {
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
                return FORMATTER.formatDelta(difference.get(metric), metric,
                        Functions.getCurrentLocale());
            }
        }
        if (baseline == Baseline.MODIFIED_LINES) {
            if (hasDelta(baseline, metric)) {
                return FORMATTER.formatDelta(changeCoverageDifference.get(metric), metric,
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

    @Override
    protected AbstractXmlStream<Node> createXmlStream() {
        return new CoverageXmlStream();
    }

    @Override
    protected CoverageJobAction createProjectAction() {
        return new CoverageJobAction(getOwner().getParent(), getUrlName(), name);
    }

    @Override
    protected String getBuildResultBaseName() {
        return String.format("%s.xml", id);
    }

    @Override
    public CoverageViewModel getTarget() {
        return new CoverageViewModel(getOwner(), getUrlName(), name, getResult(),
                getStatistics(), getQualityGateResult(), getReferenceBuildLink(), log);
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

    @Override
    public String toString() {
        return String.format("%s (%s): %s", getDisplayName(), getUrlName(), projectValues);
    }
}
