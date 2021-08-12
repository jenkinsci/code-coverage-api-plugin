package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.StaplerProxy;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;

import io.jenkins.plugins.coverage.Messages;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.util.AbstractXmlStream;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JobAction;

/**
 * Controls the life cycle of the coverage results in a job. This action persists the results of a build and displays a
 * summary on the build page. The actual visualization of the results is defined in the matching {@code summary.jelly}
 * file. This action also provides access to the coverage details: these are rendered using a new view instance.
 *
 * @author Ullrich Hafner
 */
public class CoverageBuildAction extends BuildAction<CoverageResult> implements HealthReportingAction, StaplerProxy {
    private static final long serialVersionUID = -6023811049340671399L;

    static final String SMALL_ICON = "/plugin/code-coverage-api/icons/coverage-24x24.png";

    private HealthReport healthReport;
    private String failMessage;

    private final Ratio lineCoverage;
    private final Ratio branchCoverage;

    /**
     * Creates a new instance of {@link CoverageBuildAction}.
     *
     * @param owner
     *         the associated build that created the statistics
     * @param result
     *         the coverage results to persist with this action
     */
    public CoverageBuildAction(final Run<?, ?> owner, final CoverageResult result) {
        this(owner, result, true);
    }

    @VisibleForTesting
    CoverageBuildAction(final Run<?, ?> owner, final CoverageResult result, final boolean canSerialize) {
        super(owner, result, canSerialize);

        lineCoverage = result.getCoverage(CoverageElement.LINE);
        branchCoverage = result.getCoverage(CoverageElement.CONDITIONAL);
    }

    public Ratio getLineCoverage() {
        return lineCoverage;
    }

    public Ratio getBranchCoverage() {
        return branchCoverage;
    }

    @Override
    protected AbstractXmlStream<CoverageResult> createXmlStream() {
        return new CoverageXmlStream();
    }

    @Override
    protected JobAction<? extends BuildAction<CoverageResult>> createProjectAction() {
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
        return getResult();
    }

    @Override
    public CoverageResult getResult() {
        CoverageResult result = super.getResult();
        if (!getOwner().equals(result.getOwner())) {
            result.setOwner(getOwner());
        }
        return result;
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

    private static class CoverageXmlStream extends AbstractXmlStream<CoverageResult> {
        CoverageXmlStream() {
            super(CoverageResult.class);
        }

        @Override
        protected CoverageResult createDefaultValue() {
            return new CoverageResult(CoverageElement.REPORT, null, "Empty");
        }
    }
}
