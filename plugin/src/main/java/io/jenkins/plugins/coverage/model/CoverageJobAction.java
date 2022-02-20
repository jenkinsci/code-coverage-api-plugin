package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.model.Job;

import io.jenkins.plugins.coverage.Messages;
import io.jenkins.plugins.coverage.model.visualization.charts.CoverageTrendChart;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;

/**
 * Project level action for the coverage results. A job action displays a link on the side panel of a job that refers to
 * the last build that contains coverage results (i.e. a {@link CoverageBuildAction} with a {@link CoverageNode}
 * instance). This action also is responsible to render the historical trend via its associated 'floatingBox.jelly'
 * view.
 *
 * @author Ullrich Hafner
 */
public class CoverageJobAction extends AsyncConfigurableTrendJobAction<CoverageBuildAction> {
    CoverageJobAction(final Job<?, ?> owner) {
        super(owner, CoverageBuildAction.class);
    }

    @Override
    public String getIconFileName() {
        return CoverageBuildAction.SMALL_ICON;
    }

    @Override
    public String getDisplayName() {
        return Messages.CoverageProjectAction_displayName();
    }

    @Override @NonNull
    public String getUrlName() {
        return "coverage";
    }

    public Job<?, ?> getProject() {
        return getOwner();
    }

    public String getSearchUrl() {
        return getUrlName();
    }

    @Override
    public LinesChartModel createChartModel(final String configuration) {
        return createChart(createBuildHistory(), configuration);
    }

    LinesChartModel createChart(final Iterable<? extends BuildResult<CoverageBuildAction>> buildHistory,
            final String configuration) {
        ChartModelConfiguration modelConfiguration = ChartModelConfiguration.fromJson(configuration);
        return new CoverageTrendChart().create(buildHistory, modelConfiguration);
    }
}
