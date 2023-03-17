package io.jenkins.plugins.coverage.metrics.steps;

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

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.StaplerProxy;
import hudson.Functions;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.charts.CoverageTrendChart;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.coverage.metrics.steps.CoverageXmlStream.MetricFractionMapConverter;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.echarts.GenericBuildActionIterator.BuildActionIterable;
import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.AbstractXmlStream;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.QualityGateResult;

import static hudson.model.Run.*;

/**
 * Controls the life cycle of the coverage results in a job. This action persists the results of a build and displays a
 * summary on the build page. The actual visualization of the results is defined in the matching {@code summary.jelly}
 * file. This action also provides access to the coverage details: these are rendered using a new view instance.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public final class CoverageBuildAction extends BuildAction<Node> implements StaplerProxy {
    private static final long serialVersionUID = -6023811049340671399L;

    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private static final String NO_REFERENCE_BUILD = "-";

    private final String id;
    private final String name;

    private final String referenceBuildId;

    private final QualityGateResult qualityGateResult;

    private final String icon;
    // FIXME: Rethink if we need a separate result object that stores all data?
    private final FilteredLog log;

    /** The aggregated values of the result for the root of the tree. */
    private final List<? extends Value> projectValues;

    /** The delta of this build's coverages with respect to the reference build. */
    private final NavigableMap<Metric, Fraction> difference;

    /** The coverages filtered by modified lines of the associated change request. */
    private final List<? extends Value> modifiedLinesCoverage;

    /** The delta of the coverages of the associated change request with respect to the reference build. */
    private final NavigableMap<Metric, Fraction> modifiedLinesCoverageDifference;

    /** The coverage of the modified lines. */
    private final List<? extends Value> modifiedFilesCoverage;

    /** The coverage delta of the modified lines. */
    private final NavigableMap<Metric, Fraction> modifiedFilesCoverageDifference;

    /** The indirect coverage changes of the associated change request with respect to the reference build. */
    private final List<? extends Value> indirectCoverageChanges;

    static {
        CoverageXmlStream.registerConverters(XSTREAM2);
        XSTREAM2.registerLocalConverter(CoverageBuildAction.class, "difference",
                new MetricFractionMapConverter());
        XSTREAM2.registerLocalConverter(CoverageBuildAction.class, "modifiedLinesCoverageDifference",
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
     * @param icon
     *         name of the icon that should be used in actions and views
     * @param result
     *         the coverage tree as result to persist with this action
     * @param qualityGateResult
     *         status of the quality gates
     * @param log
     *         the logging statements of the recording step
     */
    public CoverageBuildAction(final Run<?, ?> owner, final String id, final String optionalName, final String icon,
            final Node result, final QualityGateResult qualityGateResult, final FilteredLog log) {
        this(owner, id, optionalName, icon, result, qualityGateResult, log, NO_REFERENCE_BUILD,
                new TreeMap<>(), List.of(), new TreeMap<>(), List.of(), new TreeMap<>(), List.of());
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
     * @param icon
     *         name of the icon that should be used in actions and views
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
     * @param modifiedLinesCoverage
     *         the coverages filtered by modified lines of the associated change request
     * @param modifiedLinesCoverageDifference
     *         difference between the project coverage and the modified lines coverage of the current build
     * @param modifiedFilesCoverage
     *         the coverages filtered by changed files of the associated change request
     * @param modifiedFilesCoverageDifference
     *         difference between the project coverage and the modified files coverage of the current build
     * @param indirectCoverageChanges
     *         the indirect coverage changes of the associated change request with respect to the reference build
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public CoverageBuildAction(final Run<?, ?> owner, final String id, final String optionalName, final String icon,
            final Node result, final QualityGateResult qualityGateResult, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> modifiedLinesCoverage,
            final NavigableMap<Metric, Fraction> modifiedLinesCoverageDifference,
            final List<? extends Value> modifiedFilesCoverage,
            final NavigableMap<Metric, Fraction> modifiedFilesCoverageDifference,
            final List<? extends Value> indirectCoverageChanges) {
        this(owner, id, optionalName, icon, result, qualityGateResult, log, referenceBuildId, delta,
                modifiedLinesCoverage,
                modifiedLinesCoverageDifference, modifiedFilesCoverage, modifiedFilesCoverageDifference,
                indirectCoverageChanges,
                true);
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageBuildAction(final Run<?, ?> owner, final String id, final String name, final String icon,
            final Node result, final QualityGateResult qualityGateResult, final FilteredLog log,
            final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final List<? extends Value> modifiedLinesCoverage,
            final NavigableMap<Metric, Fraction> modifiedLinesCoverageDifference,
            final List<? extends Value> modifiedFilesCoverage,
            final NavigableMap<Metric, Fraction> modifiedFilesCoverageDifference,
            final List<? extends Value> indirectCoverageChanges,
            final boolean canSerialize) {
        super(owner, result, false);

        this.id = id;
        this.name = name;
        this.icon = icon;
        this.log = log;

        projectValues = result.aggregateValues();
        this.qualityGateResult = qualityGateResult;
        difference = delta;
        this.modifiedLinesCoverage = new ArrayList<>(modifiedLinesCoverage);
        this.modifiedLinesCoverageDifference = modifiedLinesCoverageDifference;
        this.modifiedFilesCoverage = new ArrayList<>(modifiedFilesCoverage);
        this.modifiedFilesCoverageDifference = modifiedFilesCoverageDifference;
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

    public ElementFormatter getFormatter() {
        return FORMATTER;
    }

    public CoverageStatistics getStatistics() {
        return new CoverageStatistics(projectValues, difference, modifiedLinesCoverage, modifiedLinesCoverageDifference,
                modifiedFilesCoverage, modifiedFilesCoverageDifference);
    }

    /**
     * Returns the supported baselines.
     *
     * @return all supported baselines
     */
    @SuppressWarnings("unused") // Called by jelly view
    public List<Baseline> getBaselines() {
        return List.of(Baseline.PROJECT, Baseline.MODIFIED_FILES, Baseline.MODIFIED_LINES, Baseline.INDIRECT);
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
     * Returns all available deltas for the specified baseline.
     *
     * @param baseline
     *         the baseline to get the deltas for
     *
     * @return the available values
     * @throws NoSuchElementException
     *         if this baseline does not provide deltas
     */
    public NavigableMap<Metric, Fraction> getAllDeltas(final Baseline baseline) {
        if (baseline == Baseline.PROJECT_DELTA) {
            return difference;
        }
        else if (baseline == Baseline.MODIFIED_LINES_DELTA) {
            return modifiedLinesCoverageDifference;
        }
        else if (baseline == Baseline.MODIFIED_FILES_DELTA) {
            return modifiedFilesCoverageDifference;
        }
        throw new NoSuchElementException("No delta baseline: " + baseline);
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

    /**
     * Returns the value for the specified metric, if available.
     *
     * @param baseline
     *         the baseline to get the value for
     * @param metric
     *         the metric to get the value for
     *
     * @return the optional value
     */
    public Optional<Value> getValueForMetric(final Baseline baseline, final Metric metric) {
        return getAllValues(baseline).stream()
                .filter(value -> value.getMetric() == metric)
                .findFirst();
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
            return modifiedLinesCoverage.stream();
        }
        if (baseline == Baseline.MODIFIED_FILES) {
            return modifiedFilesCoverage.stream();
        }
        if (baseline == Baseline.INDIRECT) {
            return indirectCoverageChanges.stream();
        }
        throw new NoSuchElementException("No such baseline: " + baseline);
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
        return baseline == Baseline.PROJECT || baseline == Baseline.MODIFIED_LINES
                || baseline == Baseline.MODIFIED_FILES;
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
            return modifiedLinesCoverageDifference.containsKey(metric)
                    && Set.of(Metric.BRANCH, Metric.LINE).contains(metric);
        }
        if (baseline == Baseline.MODIFIED_FILES) {
            return modifiedFilesCoverageDifference.containsKey(metric)
                    && Set.of(Metric.BRANCH, Metric.LINE).contains(metric);
        }
        if (baseline == Baseline.INDIRECT) {
            return false;
        }
        throw new NoSuchElementException("No such baseline: " + baseline);
    }

    /**
     * Returns whether a value for the specified metric exists.
     *
     * @param baseline
     *         the baseline to use
     * @param metric
     *         the metric to check
     *
     * @return {@code true} if a value is available for the specified metric, {@code false} otherwise
     */
    public boolean hasValue(final Baseline baseline, final Metric metric) {
        return getAllValues(baseline).stream()
                .anyMatch(v -> v.getMetric() == metric);
    }

    /**
     * Returns a formatted and localized String representation of the value for the specified metric (with respect to
     * the given baseline).
     *
     * @param baseline
     *         the baseline to use
     * @param metric
     *         the metric to get the delta for
     *
     * @return the formatted value
     */
    public String formatValue(final Baseline baseline, final Metric metric) {
        var value = getValueForMetric(baseline, metric);
        return value.isPresent() ? FORMATTER.formatValue(value.get()) : Messages.Coverage_Not_Available();
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
        var currentLocale = Functions.getCurrentLocale();
        if (baseline == Baseline.PROJECT && hasDelta(baseline, metric)) {
            return FORMATTER.formatDelta(difference.get(metric), metric, currentLocale);
        }
        if (baseline == Baseline.MODIFIED_LINES && hasDelta(baseline, metric)) {
            return FORMATTER.formatDelta(modifiedLinesCoverageDifference.get(metric), metric, currentLocale);
        }
        if (baseline == Baseline.MODIFIED_FILES && hasDelta(baseline, metric)) {
            return FORMATTER.formatDelta(modifiedFilesCoverageDifference.get(metric), metric, currentLocale);
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
        return new TreeSet<>(
                Set.of(Metric.LINE, Metric.LOC, Metric.BRANCH, Metric.COMPLEXITY_DENSITY, Metric.MUTATION));
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
        return new CoverageJobAction(getOwner().getParent(), getUrlName(), name, icon);
    }

    @Override
    protected String getBuildResultBaseName() {
        return String.format("%s.xml", id);
    }

    @Override
    public CoverageViewModel getTarget() {
        return new CoverageViewModel(getOwner(), getUrlName(), name, getResult(),
                getStatistics(), getQualityGateResult(), getReferenceBuildLink(), log, this::createChartModel);
    }

    private String createChartModel(final String configuration) {
        // FIXME: add without optional
        var iterable = new BuildActionIterable<>(CoverageBuildAction.class, Optional.of(this),
                action -> getUrlName().equals(action.getUrlName()), CoverageBuildAction::getStatistics);
        return new JacksonFacade().toJson(
                new CoverageTrendChart().create(iterable, ChartModelConfiguration.fromJson(configuration)));
    }

    @NonNull
    @Override
    public String getIconFileName() {
        return icon;
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
