package io.jenkins.plugins.coverage;

import hudson.model.Action;
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

public class CoverageAction implements StaplerProxy, SimpleBuildStep.LastBuildAction, RunAction2 {

    private transient Run<?, ?> owner;
    private transient WeakReference<CoverageResult> report;


    public CoverageAction(CoverageResult result) {
        this.report = new WeakReference<>(result);
    }

    @Override
    public Object getTarget() {
        return getResult();
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        //TODO Only stable should be replaced by variable
        return Collections.singleton(new CoverageProjectAction(owner, false));
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
        return "coverage";
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        setOwner(r);
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        setOwner(r);
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

    private CoverageResult getResult() {
        if (report != null) {
            CoverageResult r = report.get();
            if (r != null) {
                return r;
            }
        }

        CoverageResult r = null;
        try {
            r = CoverageProcessor.recoverReport(owner);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (r != null) {
            r.setOwner(owner);
            report = new WeakReference<>(r);
        }
        return r;
    }
}
