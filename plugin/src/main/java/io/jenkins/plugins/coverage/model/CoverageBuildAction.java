package io.jenkins.plugins.coverage.model;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;

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
public class CoverageBuildAction extends BuildAction<CoverageNode> implements HealthReportingAction, StaplerProxy {
    private static final long serialVersionUID = -6023811049340671399L;

    static final String SMALL_ICON = "/plugin/code-coverage-api/icons/coverage.svg";
    private static final String NO_REFERENCE_BUILD = "-";

    /** Relative URL to the details of the code coverage results. */
    static final String DETAILS_URL = "coverage";

    private final HealthReport healthReport;

    private final Coverage lineCoverage;
    private final Coverage branchCoverage;

    private final String referenceBuildId;
    private SortedMap<CoverageMetric, Fraction> difference; // since 3.0.0
    @SuppressWarnings("unused")
    private SortedMap<CoverageMetric, Double> delta; // not used anymore

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
        this(owner, result, healthReport, NO_REFERENCE_BUILD, new TreeMap<>());
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
     * @param delta
     *         the delta coverage with respect to the reference build
     * @param referenceBuildId
     *         the ID of the reference build
     */
    public CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result,
            final HealthReport healthReport, final String referenceBuildId, final SortedMap<CoverageMetric, Fraction> delta) {
        this(owner, result, healthReport, referenceBuildId, delta, true);
    }

    @VisibleForTesting
    CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result,
            final HealthReport healthReport, final String referenceBuildId, final SortedMap<CoverageMetric, Fraction> delta,
            final boolean canSerialize) {
        super(owner, result, canSerialize);

        lineCoverage = result.getCoverage(CoverageMetric.LINE);
        branchCoverage = result.getCoverage(CoverageMetric.BRANCH);

        this.difference = delta;
        this.referenceBuildId = referenceBuildId;
        this.healthReport = healthReport;
    }

    @Override
    protected Object readResolve() {
        if (difference == null) {
            difference = StreamEx.of(delta.entrySet()).toSortedMap(Entry::getKey, e -> Fraction.getFraction(e.getValue()));
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
    public SortedMap<CoverageMetric, Fraction> getDifference() {
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
     * Returns a formatted and localized String representation of the delta for the specified metric (with
     * respect to the reference build).
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
            return String.format(clientLocale, "%+.3f", difference.get(metric).doubleValue());
        }
        return Messages.Coverage_Not_Available();
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
        return getResult().printCoverageFor(metric, Functions.getCurrentLocale());
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

    static class CoverageXmlStream extends AbstractXmlStream<CoverageNode> {
        CoverageXmlStream() {
            super(CoverageNode.class);
        }

        @Override
        protected void configureXStream(final XStream2 xStream) {
            xStream.alias("node", CoverageNode.class);
            xStream.alias("leaf", CoverageLeaf.class);
            xStream.addImmutableType(CoverageMetric.class, false);
            xStream.registerConverter(new MetricsConverter());
        }

        @Override
        protected CoverageNode createDefaultValue() {
            return new CoverageNode(CoverageMetric.MODULE, "Empty");
        }
    }
}
