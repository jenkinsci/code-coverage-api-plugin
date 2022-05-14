package io.jenkins.plugins.coverage.model;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import one.util.streamex.StreamEx;

import org.kohsuke.stapler.StaplerProxy;
import hudson.Functions;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import hudson.util.XStream2;

import io.jenkins.plugins.forensics.reference.ReferenceBuild;
import io.jenkins.plugins.util.AbstractXmlStream;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JenkinsFacade;

/**
 * Controls the life cycle of the coverage results in a job. This action persists the results of a build and displays a
 * summary on the build page. The actual visualization of the results is defined in the matching {@code summary.jelly}
 * file. This action also provides access to the coverage details: these are rendered using a new view instance.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
public class CoverageBuildAction extends BuildAction<CoverageNode> implements HealthReportingAction, StaplerProxy {

    /** Relative URL to the details of the code coverage results. */
    public static final String DETAILS_URL = "coverage";
    /** The coverage report icon. */
    public static final String SMALL_ICON = "/plugin/code-coverage-api/icons/coverage.svg";

    private static final long serialVersionUID = -6023811049340671399L;

    private static final String NO_REFERENCE_BUILD = "-";

    private final HealthReport healthReport;

    private final Coverage lineCoverage;
    private final Coverage branchCoverage;

    private final String referenceBuildId;

    // since 3.0.0
    private SortedMap<CoverageMetric, CoveragePercentage> difference;
    private SortedMap<CoverageMetric, CoveragePercentage> changeCoverage;
    private SortedMap<CoverageMetric, CoveragePercentage> changeCoverageDifference;
    private SortedMap<CoverageMetric, CoveragePercentage> indirectCoverageChanges;

    @SuppressWarnings("unused")
    private final transient SortedMap<CoverageMetric, Double> delta = new TreeMap<>(); // not used anymore

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
    public CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result, final HealthReport healthReport) {
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
     *         the delta coverage with respect to the reference build
     * @param changeCoverage
     *         the change coverage with respect to the reference build
     * @param changeCoverageDifference
     *         the change coverage delta with respect to the reference build
     * @param indirectCoverageChanges
     *         the indirect coverage changes with respect to the reference build
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result,
            final HealthReport healthReport, final String referenceBuildId,
            final SortedMap<CoverageMetric, CoveragePercentage> delta,
            final SortedMap<CoverageMetric, CoveragePercentage> changeCoverage,
            final SortedMap<CoverageMetric, CoveragePercentage> changeCoverageDifference,
            final SortedMap<CoverageMetric, CoveragePercentage> indirectCoverageChanges) {
        this(owner, result, healthReport, referenceBuildId, delta, changeCoverage,
                changeCoverageDifference, indirectCoverageChanges, true);
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result,
            final HealthReport healthReport, final String referenceBuildId,
            final SortedMap<CoverageMetric, CoveragePercentage> delta,
            final SortedMap<CoverageMetric, CoveragePercentage> changeCoverage,
            final SortedMap<CoverageMetric, CoveragePercentage> changeCoverageDifference,
            final SortedMap<CoverageMetric, CoveragePercentage> indirectCoverageChanges, final boolean canSerialize) {
        super(owner, result, canSerialize);

        lineCoverage = result.getCoverage(CoverageMetric.LINE);
        branchCoverage = result.getCoverage(CoverageMetric.BRANCH);

        this.difference = delta;
        this.changeCoverage = changeCoverage;
        this.changeCoverageDifference = changeCoverageDifference;
        this.indirectCoverageChanges = indirectCoverageChanges;
        this.referenceBuildId = referenceBuildId;
        this.healthReport = healthReport;
    }

    @Override
    protected Object readResolve() {
        if (difference == null) {
            difference = StreamEx.of(delta.entrySet())
                    .toSortedMap(Entry::getKey,
                            e -> CoveragePercentage.valueOf(e.getValue()));
        }
        if (changeCoverage == null) {
            changeCoverage = new TreeMap<>();
        }
        if (changeCoverageDifference == null) {
            changeCoverageDifference = new TreeMap<>();
        }
        if (indirectCoverageChanges == null) {
            indirectCoverageChanges = new TreeMap<>();
        }
        return super.readResolve();
    }

    public Coverage getLineCoverage() {
        return lineCoverage;
    }

    public Coverage getBranchCoverage() {
        return branchCoverage;
    }

    /**
     * Returns whether the {@link Coverage} for the passed metric exists.
     *
     * @param coverageMetric
     *         the metric to check
     *
     * @return {@code true} if a coverage is available for the specified metric
     */
    public boolean hasCoverage(final CoverageMetric coverageMetric) {
        return getResult().getCoverage(coverageMetric).isSet();
    }

    /**
     * Gets the {@link Coverage} for the passed metric.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return the coverage
     */
    public Coverage getCoverage(final CoverageMetric coverageMetric) {
        return getResult().getCoverage(coverageMetric);
    }

    /**
     * Returns whether a change coverage exists at all.
     *
     * @return {@code true} if the change coverage exist, else {@code false}
     */
    public boolean hasChangeCoverage() {
        return getResult().hasChangeCoverage();
    }

    /**
     * Returns whether a change coverage exists for the passed {@link CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return {@code true} if the change coverage exist for the metric, else {@code false}
     */
    public boolean hasChangeCoverage(final CoverageMetric coverageMetric) {
        return getResult().hasChangeCoverage(coverageMetric);
    }

    /**
     * Gets the {@link Coverage change coverage} for the passed metric.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return the change coverage
     */
    public Coverage getChangeCoverage(final CoverageMetric coverageMetric) {
        return getResult().getChangeCoverageTree().getCoverage(coverageMetric);
    }

    /**
     * Returns whether indirect coverage changes exist at all.
     *
     * @return {@code true} if indirect coverage changes exist, else {@code false}
     */
    public boolean hasIndirectCoverageChanges() {
        return getResult().hasIndirectCoverageChanges();
    }

    /**
     * Returns whether indirect coverage changes exist for the passed {@link CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return {@code true} if indirect coverage changes exist for the metric, else {@code false}
     */
    public boolean hasIndirectCoverageChanges(final CoverageMetric coverageMetric) {
        return getResult().hasIndirectCoverageChanges(coverageMetric);
    }

    /**
     * Gets the {@link Coverage indirect coverage changes} for the passed metric.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return the indirect coverage changes
     */
    public Coverage getIndirectCoverageChanges(final CoverageMetric coverageMetric) {
        return getResult().getIndirectCoverageChangesTree().getCoverage(coverageMetric);
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
    public SortedMap<CoverageMetric, CoveragePercentage> getDifference() {
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
    public boolean hasDelta(final CoverageMetric metric) {
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
    public String formatDelta(final CoverageMetric metric) {
        Locale clientLocale = Functions.getCurrentLocale();
        if (hasDelta(metric)) {
            return difference.get(metric).formatDeltaPercentage(clientLocale);
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
    public String formatChangeCoverage(final CoverageMetric metric) {
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
    public String formatIndirectCoverageChanges(final CoverageMetric metric) {
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
    private String formatCoverageForMetric(final CoverageMetric metric,
            final Map<CoverageMetric, CoveragePercentage> coverages) {
        String coverage = Messages.Coverage_Not_Available();
        if (coverages != null && coverages.containsKey(metric)) {
            coverage = coverages.get(metric).formatPercentage(Functions.getCurrentLocale());
        }
        return metric.getName() + ": " + coverage;
    }

    /**
     * Returns the change coverage delta for the passed metric, i.e. the coverage results of the current build minus the
     * same results of the reference build.
     *
     * @param coverageMetric
     *         The change coverage metric
     *
     * @return the delta for each available coverage metric
     */
    public CoveragePercentage getChangeCoverageDifference(final CoverageMetric coverageMetric) {
        return changeCoverageDifference.get(coverageMetric);
    }

    /**
     * Returns whether a change coverage delta metric for the specified metric exist.
     *
     * @param metric
     *         the metric to check
     *
     * @return {@code true} if a delta is available for the specified metric
     */
    public boolean hasChangeCoverageDifference(final CoverageMetric metric) {
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
    public String formatChangeCoverageDifference(final CoverageMetric metric) {
        Locale clientLocale = Functions.getCurrentLocale();
        if (hasChangeCoverage(metric)) {
            return changeCoverageDifference.get(metric).formatDeltaPercentage(clientLocale);
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
            int fileAmount = getResult().getFileAmountWithChangedCoverage();
            long lineAmount = getResult().getLineAmountWithChangedCoverage();
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
            int fileAmount = getResult().getFileAmountWithIndirectCoverageChanges();
            long lineAmount = getResult().getLineAmountWithIndirectCoverageChanges();
            return getFormattedChangesOverview(lineAmount, fileAmount);
        }
        return Messages.Coverage_Not_Available();
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
    public String formatCoverage(final CoverageMetric metric) {
        String coverage = getResult().printCoverageFor(metric, Functions.getCurrentLocale());
        return metric.getName() + ": " + coverage;
    }

    @Override
    protected AbstractXmlStream<CoverageNode> createXmlStream() {
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
        return SMALL_ICON;
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

    /**
     * {@link Converter} for {@link CoverageMetric} instances so that only the string name will be serialized. After
     * reading the values back from the stream, the string representation will be converted to an actual instance
     * again.
     */
    private static final class MetricsConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof CoverageMetric ? ((CoverageMetric) source).getName() : null);
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return CoverageMetric.valueOf(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == CoverageMetric.class;
        }
    }

    /**
     * {@link Converter} for {@link Coverage} instances so that only the values will be serialized. After
     * reading the values back from the stream, the string representation will be converted to an actual instance
     * again.
     */
    private static final class CoverageConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof Coverage ? ((Coverage) source).serializeToString() : null);
        }

        @Override
        public Coverage unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return Coverage.valueOf(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == Coverage.class;
        }
    }

    /**
     * {@link Converter} for {@link CoveragePercentage} instances so that only the values will be serialized. After
     * reading the values back from the stream, the string representation will be converted to an actual instance
     * again.
     */
    private static final class PercentageConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof CoveragePercentage ? ((CoveragePercentage) source).serializeToString() : null);
        }

        @Override
        public CoveragePercentage unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return CoveragePercentage.valueOf(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == CoveragePercentage.class;
        }
    }

    /**
     * Configures the XML stream for the coverage tree, which consists of {@link CoverageNode}.
     */
    static class CoverageXmlStream extends AbstractXmlStream<CoverageNode> {

        /**
         * Creates a XML stream for {@link CoverageNode}.
         */
        CoverageXmlStream() {
            super(CoverageNode.class);
        }

        @Override
        protected void configureXStream(final XStream2 xStream) {
            xStream.alias("node", CoverageNode.class);
            xStream.alias("package", PackageCoverageNode.class);
            xStream.alias("file", FileCoverageNode.class);
            xStream.alias("method", MethodCoverageNode.class);
            xStream.alias("leaf", CoverageLeaf.class);
            xStream.alias("coverage", Coverage.class);
            xStream.alias("percentage", CoveragePercentage.class);
            xStream.addImmutableType(CoverageMetric.class, false);
            xStream.registerConverter(new MetricsConverter());
            xStream.addImmutableType(Coverage.class, false);
            xStream.registerConverter(new PercentageConverter());
        }

        @Override
        protected CoverageNode createDefaultValue() {
            return new CoverageNode(CoverageMetric.MODULE, "Empty");
        }
    }
}
