package io.jenkins.plugins.coverage.model;

import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.StaplerProxy;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import hudson.util.XStream2;

import io.jenkins.plugins.coverage.Messages;
import io.jenkins.plugins.coverage.targets.CoverageElement;
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

    static final String SMALL_ICON = "/plugin/code-coverage-api/icons/coverage-24x24.png";
    private static final String NO_REFERENCE_BUILD = "-";

    private HealthReport healthReport;
    private String failMessage;

    private final Coverage lineCoverage;
    private final Coverage branchCoverage;

    private final String referenceBuildId;
    private final SortedMap<CoverageElement, Double> delta;

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param result
     *         the coverage results to persist with this action
     */
    public CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result) {
        this(owner, result, NO_REFERENCE_BUILD, new TreeMap<>());
    }

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param result
     *         the coverage results to persist with this action
     * @param delta
     *         the delta coverage with respect to the reference build
     * @param referenceBuildId
     *         the ID of the reference build
     */
    public CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result,
            final String referenceBuildId, final SortedMap<CoverageElement, Double> delta) {
        this(owner, result, referenceBuildId, delta, true);
    }

    @VisibleForTesting
    CoverageBuildAction(final Run<?, ?> owner, final CoverageNode result,
            final String referenceBuildId, final SortedMap<CoverageElement, Double> delta,
            final boolean canSerialize) {
        super(owner, result, canSerialize);

        lineCoverage = result.getCoverage(CoverageElement.LINE);
        branchCoverage = result.getCoverage(CoverageElement.CONDITIONAL);

        this.delta = delta;
        this.referenceBuildId = referenceBuildId;
    }

    public Coverage getLineCoverage() {
        return lineCoverage;
    }

    public Coverage getBranchCoverage() {
        return branchCoverage;
    }

    public Optional<Run<?, ?>> getReferenceBuild() {
        if (NO_REFERENCE_BUILD.equals(referenceBuildId)) {
            return Optional.empty();
        }
        return new JenkinsFacade().getBuild(referenceBuildId);
    }

    public String getReferenceBuildId() {
        return referenceBuildId;
    }

    public SortedMap<CoverageElement, Double> getDelta() {
        return delta;
    }

    public boolean hasDelta(final CoverageElement element) {
        return delta.containsKey(element);
    }

    // TODO: format percentage on the client side
    public String getDelta(final CoverageElement element) {
        if (hasDelta(element)) {
            return String.format("%+.3f", delta.get(element));
        }
        return "n/a";
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
        return getHealthReport();
    }

    @Override
    public Object getTarget() {
        return new CoverageViewModel(getOwner(), getResult());
    }

    public HealthReport getHealthReport() {
        return healthReport;
    }

    public void setHealthReport(final HealthReport healthReport) {
        this.healthReport = healthReport;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(final String failMessage) {
        this.failMessage = failMessage;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return SMALL_ICON;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return Messages.CoverageAction_displayName();
    }

    @NonNull
    @Override
    public String getUrlName() {
        return "coverage";
    }

    private static class CoverageXmlStream extends AbstractXmlStream<CoverageNode> {
        CoverageXmlStream() {
            super(CoverageNode.class);
        }

        @Override
        protected void configureXStream(final XStream2 xStream) {
            xStream.alias("node", CoverageNode.class);
            xStream.alias("leaf", CoverageLeaf.class);
        }

        @Override
        protected CoverageNode createDefaultValue() {
            return new CoverageNode(CoverageElement.REPORT, "Empty");
        }
    }
}
