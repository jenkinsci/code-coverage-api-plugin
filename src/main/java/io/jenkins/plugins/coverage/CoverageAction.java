package io.jenkins.plugins.coverage;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.stapler.StaplerProxy;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.targets.CoverageResult;

public class CoverageAction implements StaplerProxy, SimpleBuildStep.LastBuildAction, RunAction2, HealthReportingAction {

    private transient Run<?, ?> owner;
    private transient WeakReference<CoverageResult> report;
    private HealthReport healthReport;
    private String failMessage;

    public CoverageAction(final CoverageResult result) {
        this.report = new WeakReference<>(result);
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (isNewActionAvailable()) {
            return Collections.emptyList();
        }
        return Collections.singleton(new CoverageProjectAction(owner));
    }


    /**
     * @return Health report
     */
    @Override
    public HealthReport getBuildHealth() {
        return getHealthReport();
    }

    /**
     * Get coverage result. If not exist, try to find it in build dir.
     *
     * @return coverage result
     */
    public CoverageResult getResult() {
        if (report != null) {
            CoverageResult coverageResult = report.get();
            if (coverageResult != null) {
                return coverageResult;
            }
        }

        CoverageResult coverageResult = null;
        try {
            coverageResult = CoverageProcessor.recoverCoverageResult(owner);
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (coverageResult != null) {
            coverageResult.setOwner(owner);
            report = new WeakReference<>(coverageResult);
        }
        return coverageResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getTarget() {
        return getResult();
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

    private synchronized void setOwner(final Run<?, ?> owner) {
        this.owner = owner;
        if (report != null) {
            CoverageResult coverageResult = report.get();
            if (coverageResult != null) {
                coverageResult.setOwner(owner);
            }
        }
    }

    public synchronized Run<?, ?> getOwner() {
        return owner;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return Messages.CoverageAction_displayName();
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        if (isNewActionAvailable()) {
            return null;
        }
        return "coverage";
    }

    public boolean isNewActionAvailable() {
        return getOwner().getAction(CoverageBuildAction.class) != null;
    }

    @Override
    public void onAttached(final Run<?, ?> r) {
        setOwner(r);
    }

    @Override
    public void onLoad(final Run<?, ?> r) {
        setOwner(r);
    }
}
