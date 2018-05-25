package io.jenkins.plugins.coverage;

import hudson.model.*;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.StaplerProxy;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CoverageAction implements StaplerProxy, SimpleBuildStep.LastBuildAction, RunAction2, HealthReportingAction {

    private transient Run<?, ?> owner;
    private transient WeakReference<CoverageResult> report;
    private HealthReport healthReport;

    public CoverageAction(CoverageResult result) {
        this.report = new WeakReference<>(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> getProjectActions() {
        //TODO Only stable should be replaced by variable
        return Collections.singleton(new CoverageProjectAction(owner, false));
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
    private CoverageResult getResult() {
        if (report != null) {
            CoverageResult r = report.get();
            if (r != null) {
                return r;
            }
        }

        CoverageResult r = null;
        try {
            r = CoverageProcessor.recoverCoverageResult(owner);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (r != null) {
            r.setOwner(owner);
            report = new WeakReference<>(r);
        }
        return r;
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

    private synchronized void setOwner(Run<?, ?> owner) {
        this.owner = owner;
        if (report != null) {
            CoverageResult r = report.get();
            if (r != null) {
                r.setOwner(owner);
            }

        }
    }

    public Run<?, ?> getOwner() {
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
