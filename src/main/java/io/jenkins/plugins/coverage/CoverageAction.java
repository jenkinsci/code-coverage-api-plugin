package io.jenkins.plugins.coverage;

import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.StaplerProxy;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;

public class CoverageAction implements StaplerProxy, SimpleBuildStep.LastBuildAction, RunAction2, HealthReportingAction {

    private transient Run<?, ?> owner;
    private transient WeakReference<CoverageResult> report;
    private HealthReport healthReport;
    private String failMessage;


    public CoverageAction(CoverageResult result) {
        this.report = new WeakReference<>(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> getProjectActions() {
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
        } catch (IOException | ClassNotFoundException e) {
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

    public void setHealthReport(HealthReport healthReport) {
        this.healthReport = healthReport;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    private synchronized void setOwner(Run<?, ?> owner) {
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

    /**
     * {@inheritDoc}
     */
    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @CheckForNull
    @Override
    public String getDisplayName() {
        return Messages.CoverageAction_displayName();
    }

    /**
     * {@inheritDoc}
     */
    @CheckForNull
    @Override
    public String getUrlName() {
        return "coverage";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttached(Run<?, ?> r) {
        setOwner(r);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoad(Run<?, ?> r) {
        setOwner(r);
    }

}
