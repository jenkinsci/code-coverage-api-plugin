

package io.jenkins.plugins.coverage;

import hudson.model.Actionable;
import hudson.model.Job;
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

    public CoverageProjectAction(Run<?, ?> run) {
        this.run = run;
    }

    /**
     * {@inheritDoc}
     */
    public String getIconFileName() {
        return "graph.gif";
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
        return "coverage";
    }

    /**
     * Getter for property 'lastResult'.
     *
     * @return Value for property 'lastResult'.
     */
    public CoverageAction getLastResult() {
        for (Run<?, ?> b = run.getParent().getLastSuccessfulBuild(); b != null; b = BuildUtils.getPreviousNotFailedCompletedBuild(b)) {
            if (b.getResult() != Result.SUCCESS && b.getResult() != Result.UNSTABLE)
                continue;
            CoverageAction r = b.getAction(CoverageAction.class);
            if (r != null)
                return r;
        }
        return null;
    }

    /**
     * Getter for property 'lastResult'.
     *
     * @return Value for property 'lastResult'.
     */
    public Integer getLastResultBuild() {
        for (Run<?, ?> b = run.getParent().getLastSuccessfulBuild(); b != null; b = BuildUtils.getPreviousNotFailedCompletedBuild(b)) {
            if (b.getResult() != Result.SUCCESS && b.getResult() != Result.UNSTABLE)
                continue;
            CoverageAction r = b.getAction(CoverageAction.class);
            if (r != null)
                return b.getNumber();
        }
        return null;
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Integer buildNumber = getLastResultBuild();
        if (buildNumber == null) {
            rsp.sendRedirect2("nodata");
        } else {
            rsp.sendRedirect2("../" + buildNumber + "/coverage");
        }
    }

    public Job<?, ?> getProject() {
        return run.getParent();
    }

    /**
     * {@inheritDoc}
     */
    public String getSearchUrl() {
        return getUrlName();
    }
}
