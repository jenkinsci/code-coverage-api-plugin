package io.jenkins.plugins.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hudson.Extension;
import hudson.model.Run;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.monitoring.MonitorPortlet;
import io.jenkins.plugins.monitoring.MonitorPortletFactory;

import java.util.*;

/**
 * A portlet that can be used for the
 * <a href="https://github.com/jenkinsci/pull-request-monitoring-plugin">pull-request-monitoring</a> dashboard.
 *
 * It renders the aggregated line and conditional coverage in a stacked bar chart and displays the delta,
 * if a reference build is found.
 *
 * @author Simon Symhoven
 */
public class CoveragePullRequestMonitoringPortlet extends MonitorPortlet {
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
    public boolean isDefault() {
        return true;
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
    public Optional<String> getIconUrl() {
        return Optional.of("/images/48x48/graph.png");
    }

    @Override
    public Optional<String> getDetailViewUrl() {
        return Optional.ofNullable(action.getUrlName());
    }

    /**
     * Get the json data for the stacked bar chart. (used by jelly view)
     *
     * @return
     *          the data as json string.
     */
    public String getCoverageResultsAsJsonModel() {
        Ratio line = action.getResult().getResults().get(CoverageElement.LINE);
        Ratio conditional = action.getResult().getResults().get(CoverageElement.CONDITIONAL);

        JsonObject data = new JsonObject();

        JsonArray metrics = new JsonArray();
        metrics.add(CoverageElement.LINE.getName());
        metrics.add(CoverageElement.CONDITIONAL.getName());
        data.add("metrics", metrics);

        JsonArray covered = new JsonArray();
        covered.add(line.numerator);
        covered.add(conditional.numerator);
        data.add("covered", covered);

        JsonArray missed = new JsonArray();
        missed.add(line.denominator - line.numerator);
        missed.add(conditional.denominator - conditional.numerator);
        data.add("missed", missed);

        JsonArray coveredPercentage = new JsonArray();
        coveredPercentage.add(line.denominator == 0 ? 0 : (double) (100 * (covered.get(0).getAsInt() / line.denominator)));
        coveredPercentage.add(conditional.denominator == 0 ? 0 : (double) (100 * (covered.get(1).getAsInt() / conditional.denominator)));
        data.add("coveredPercentage", coveredPercentage);

        JsonArray missedPercentage = new JsonArray();
        missedPercentage.add(100 - coveredPercentage.get(0).getAsDouble());
        missedPercentage.add(100 - coveredPercentage.get(1).getAsDouble());
        data.add("missedPercentage", missedPercentage);

        String deltaLineLabel = getReferenceBuildUrl().isPresent()
                ? String.format("%.2f%% (%s %.2f%%)", coveredPercentage.get(0).getAsDouble(), (char) 0x0394,
                action.getResult().getCoverageDelta(CoverageElement.LINE))
                : String.format("%.2f%% (%s unknown)", coveredPercentage.get(0).getAsDouble(), (char) 0x0394);

        String deltaConditionalLabel = getReferenceBuildUrl().isPresent()
                ? String.format("%.2f%% (%s %.2f%%)", coveredPercentage.get(1).getAsDouble(), (char) 0x0394,
                action.getResult().getCoverageDelta(CoverageElement.CONDITIONAL))
                : String.format("%.2f%% (%s unknown)", coveredPercentage.get(1).getAsDouble(), (char) 0x0394);

        JsonArray coveredPercentageLabels = new JsonArray();
        coveredPercentageLabels.add(deltaLineLabel);
        coveredPercentageLabels.add(deltaConditionalLabel);
        data.add("coveredPercentageLabels", coveredPercentageLabels);

        return data.toString();
    }

    /**
     * Get the link to the build, that was used to compare the result with.
     *
     * @return
     *          optional of the link to the build or empty optional.
     */
    public Optional<String> getReferenceBuildUrl() {
        return Optional.ofNullable(action.getResult().getReferenceBuildUrl());
    }

    /**
     * The factory for the {@link CoveragePullRequestMonitoringPortlet}.
     */
    @Extension(optional = true)
    public static class PortletFactory extends MonitorPortletFactory {

        @Override
        public Collection<MonitorPortlet> getPortlets(Run<?, ?> build) {
            CoverageAction action = build.getAction(CoverageAction.class);

            if (action == null) {
                return Collections.emptyList();
            }

            return Collections.singleton(new CoveragePullRequestMonitoringPortlet(action));
        }

        @Override
        public String getDisplayName() {
            return "Code Coverage API";
        }
    }

}
