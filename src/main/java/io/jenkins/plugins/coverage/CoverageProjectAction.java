

package io.jenkins.plugins.coverage;

import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.Run;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * Project level action.
 *
 * @author Stephen Connolly
 */
public class CoverageProjectAction extends Actionable implements ProminentProjectAction {

    private transient Run<?, ?> run;
    private boolean onlyStable;

    public CoverageProjectAction(Run<?, ?> run, boolean onlyStable) {
        this.run = run;
        this.onlyStable = onlyStable;
    }

    public CoverageProjectAction(Run<?, ?> run) {
        this(run, false);
    }

    /**
     * {@inheritDoc}
     */
    public String getIconFileName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return Messages.CoverageProjectAction_displayName();
    }

    /**
     * {@inheritDoc}
     */
    public String getUrlName() {
        return "cobertura";
    }

    /**
     * Getter for property 'lastResult'.
     *
     * @return Value for property 'lastResult'.
     */
    public CoverageAction getLastResult() {
        for (Run<?, ?> b = getLastBuildToBeConsidered(); b != null; b = BuildUtils.getPreviousNotFailedCompletedBuild(b)) {
            if (b.getResult() == Result.FAILURE || (b.getResult() != Result.SUCCESS && onlyStable))
                continue;
            CoverageAction r = b.getAction(CoverageAction.class);
            if (r != null)
                return r;
        }
        return null;
    }
    private Run<?, ?> getLastBuildToBeConsidered(){
        return onlyStable ? run.getParent().getLastStableBuild() : run.getParent().getLastSuccessfulBuild();
    }
     /**
     * Getter for property 'lastResult'.
     *
     * @return Value for property 'lastResult'.
     */
    public Integer getLastResultBuild() {
        for (Run<?, ?> b = getLastBuildToBeConsidered(); b != null; b = BuildUtils.getPreviousNotFailedCompletedBuild(b)) {
            if (b.getResult() == Result.FAILURE || (b.getResult() != Result.SUCCESS && onlyStable))
                continue;
            CoverageAction r = b.getAction(CoverageAction.class);
            if (r != null)
                return b.getNumber();
        }
        return null;
    }

//    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
//        if (getLastResult() != null)
//            getLastResult().doGraph(req, rsp);
//    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Integer buildNumber = getLastResultBuild();
        if (buildNumber == null) {
            rsp.sendRedirect2("nodata");
        } else {
            rsp.sendRedirect2("../" + buildNumber + "/coverage");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getSearchUrl() {
        return getUrlName();
    }
}
