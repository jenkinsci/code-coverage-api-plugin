package io.jenkins.plugins.coverage;

import hudson.Extension;
import hudson.model.Run;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.monitoring.MonitorPortlet;
import io.jenkins.plugins.monitoring.MonitorPortletFactory;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.*;

/**
 * A portlet that can be used for the pull-request-monitoring dashboard
 * (https://github.com/jenkinsci/pull-request-monitoring-plugin).
 *
 * It renders the aggregated line, method and instruction coverage in a stacked bar chart.
 *
 * @author Simon Symhoven
 */
public class CoveragePullRequestMonitoringPortlet implements MonitorPortlet {
    private final CoverageAction action;

    /**
     * Creates a new {@link CoveragePullRequestMonitoringPortlet}.
     *
     * @param action
     *          the {@link CoverageAction} of corresponding run.
     */
    public CoveragePullRequestMonitoringPortlet(final CoverageAction action) {
        this.action = action;
    }

    @Override
    public String getTitle() {
        return action.getDisplayName();
    }

    @Override
    public String getId() {
        return "code-coverage";
    }

    @Override
    public int getPreferredWidth() {
        return 600;
    }

    @Override
    public int getPreferredHeight() {
        return 400;
    }

    @Override
    public String getIconUrl() {
        return null;
    }

    @Override
    public Optional<String> getDetailViewUrl() {
        return Optional.ofNullable(action.getUrlName());
    }

    /**
     * Transform the result map of {@link CoverageResult} to a list of {@link CoverageResult.JSCoverageResult}.
     *
     * @return
     *          the transformed list.
     */
    @JavaScriptMethod
    public List<CoverageResult.JSCoverageResult> getResults() {
        List<CoverageResult.JSCoverageResult> results = new LinkedList<>();

        for (Map.Entry<CoverageElement, Ratio> c : action.getResult().getResults().entrySet()) {
            String name = c.getKey().getName();
            if ("Conditional".equals(name) || "Line".equals(name) || "Instruction".equals(name)) {
                results.add(new CoverageResult.JSCoverageResult(c.getKey().getName(), c.getValue()));
            }
        }

        return results;
    }

    /**
     * Get the link to the build, that was used to compare the result with.
     *
     * @return
     *          optional of the link to the build or empty optional.
     */
    public Optional<String> getComparedBuildLink() {
        return Optional.ofNullable(action.getResult().getLinkToBuildThatWasUsedForComparison());
    }

    /**
     * Get the diff to the target branch.
     *
     * @return
     *          the diff as float.
     */
    public float getDiff() {
        return action.getResult().getChangeRequestCoverageDiffWithTargetBranch();
    }

    /**
     * The factory for the {@link CoveragePullRequestMonitoringPortlet}.
     */
    @Extension(optional = true)
    public static class PortletFactory implements MonitorPortletFactory {

        @Override
        public Collection<MonitorPortlet> getPortlets(Run<?, ?> build) {
            CoverageAction action = build.getAction(CoverageAction.class);
            return Collections.singleton(new CoveragePullRequestMonitoringPortlet(action));
        }

        @Override
        public String getDisplayName() {
            return "Code Coverage API";
        }
    }
}
