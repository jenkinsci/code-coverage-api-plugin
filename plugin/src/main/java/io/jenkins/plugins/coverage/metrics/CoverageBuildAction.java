package io.jenkins.plugins.coverage.metrics;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.StaplerProxy;
import hudson.Functions;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.CoverageViewModel.ValueLabelProvider;
import io.jenkins.plugins.coverage.metrics.CoverageXmlStream.FractionConverter;
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
public class CoverageBuildAction extends BuildAction<Node> implements HealthReportingAction, StaplerProxy {
    /** Relative URL to the details of the code coverage results. */
    public static final String DETAILS_URL = "coverage";
    /** The coverage report symbol from the Ionicons plugin. */
    public static final String ICON = "symbol-footsteps-outline plugin-ionicons-api";

    private static final long serialVersionUID = -6023811049340671399L;

    private static final String NO_REFERENCE_BUILD = "-";

    private static final CoverageFormatter FORMATTER = new CoverageFormatter();
    private static final ValueLabelProvider VALUE_LABEL_PROVIDER = new ValueLabelProvider();

    private final HealthReport healthReport;

    private final String referenceBuildId;

    /** The coverages of the result. */
    private final NavigableMap<Metric, Value> coverage;

    /** The delta of this build's coverages with respect to the reference build. */
    private final NavigableMap<Metric, Fraction> difference;

    /** The coverages filtered by changed lines of the associated change request. */
    private final NavigableMap<Metric, Value> changeCoverage;

    /** The delta of the coverages of the associated change request with respect to the reference build. */
    private final NavigableMap<Metric, Fraction> changeCoverageDifference;

    /** The indirect coverage changes of the associated change request with respect to the reference build. */
    private final NavigableMap<Metric, Value> indirectCoverageChanges;

    static {
        XSTREAM.registerConverter(new FractionConverter());
    }

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param result
     *         the coverage results to persist with this action
     * @param healthReport
     *         health report
     */
    public CoverageBuildAction(final Run<?, ?> owner, final Node result, final HealthReport healthReport) {
        this(owner, result, healthReport, NO_REFERENCE_BUILD, new TreeMap<>(), new TreeMap<>(),
                new TreeMap<>(), new TreeMap<>());
    }

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param result
     *         the coverage results to persist with this action
     * @param healthReport
     *         health report
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
    public CoverageBuildAction(final Run<?, ?> owner, final Node result,
            final HealthReport healthReport, final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final NavigableMap<Metric, Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final NavigableMap<Metric, Value> indirectCoverageChanges) {
        this(owner, result, healthReport, referenceBuildId, delta, changeCoverage,
                changeCoverageDifference, indirectCoverageChanges, true);
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageBuildAction(final Run<?, ?> owner, final Node result,
            final HealthReport healthReport, final String referenceBuildId,
            final NavigableMap<Metric, Fraction> delta,
            final NavigableMap<Metric, Value> changeCoverage,
            final NavigableMap<Metric, Fraction> changeCoverageDifference,
            final NavigableMap<Metric, Value> indirectCoverageChanges,
            final boolean canSerialize) {
        super(owner, result, canSerialize);

        coverage = result.getMetricsDistribution();
        difference = delta;
        this.changeCoverage = changeCoverage;
        this.changeCoverageDifference = changeCoverageDifference;
        this.indirectCoverageChanges = indirectCoverageChanges;
        this.referenceBuildId = referenceBuildId;
        this.healthReport = healthReport;
    }

    public NavigableSet<Metric> getMetricsForSummary() {
        var metrics = new TreeSet<Metric>();
        if (hasCoverage(Metric.LINE)) {
            metrics.add(Metric.LINE);
            metrics.add(Metric.LOC);
        }
        if (hasCoverage(Metric.BRANCH)) {
            metrics.add(Metric.BRANCH);
        }
        if (hasCoverage(Metric.COMPLEXITY)) {
            metrics.add(Metric.COMPLEXITY);
        }
        return metrics;
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
        return coverage.containsKey(metric);
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
        return coverage.get(metric);
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
        return changeCoverage.containsKey(metric);
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
        return changeCoverage.get(metric);
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
        return indirectCoverageChanges.containsKey(metric);
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
        return indirectCoverageChanges.get(metric);
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
     * @param coverages
     *         the coverage values of a specific type, mapped by their metrics
     *
     * @return the formatted text representation of the coverage value corresponding to the passed metric
     */
    private String formatCoverageForMetric(final Metric metric, final Map<Metric, Value> coverages) {
        String coverage = Messages.Coverage_Not_Available();
        if (coverages.containsKey(metric)) {
            coverage = FORMATTER.format(coverages.get(metric), Functions.getCurrentLocale());
        }
        return metric + ": " + coverage;
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
        return getResult().hasCodeChanges();
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
        return VALUE_LABEL_PROVIDER.getDisplayName(metric) + ": "
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
    public HealthReport getBuildHealth() {
        return healthReport;
    }

    @Override
    public CoverageViewModel getTarget() {
        return new CoverageViewModel(getOwner(), getResult());
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return ICON;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return Messages.Coverage_Link_Name();
    }

    @NonNull
    @Override
    public String getUrlName() {
        return DETAILS_URL;
    }
}
