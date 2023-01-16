package io.jenkins.plugins.coverage.metrics.steps;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.metric.Node;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.model.Job;

import io.jenkins.plugins.coverage.metrics.charts.CoverageTrendChart;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;
import io.jenkins.plugins.echarts.GenericBuildActionIterator.BuildActionIterable;

/**
 * Project level action for the coverage results. A job action displays a link on the side panel of a job that refers to
 * the last build that contains coverage results (i.e. a {@link CoverageBuildAction} with a {@link Node}
 * instance). This action also is responsible to render the historical trend via its associated 'floatingBox.jelly'
 * view.
 *
 * @author Ullrich Hafner
 */
public class CoverageJobAction extends AsyncConfigurableTrendJobAction<CoverageBuildAction> {
    private final String id;
    private final String name;

    CoverageJobAction(final Job<?, ?> owner, final String id, final String name) {
        super(owner, CoverageBuildAction.class);

        this.id = id;
        this.name = name;
    }

    @Override
    public String getIconFileName() {
        return CoverageBuildAction.ICON;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.defaultIfBlank(name, Messages.Coverage_Link_Name());
    }

    /**
     * Returns a label for the trend chart.
     *
     * @return a label for the trend chart
     */
    public String getTrendName() {
        if (StringUtils.isBlank(name)) {
            Messages.Coverage_Trend_Default_Name();
        }
        return Messages.Coverage_Trend_Name(name);
    }

    @Override @NonNull
    public String getUrlName() {
        return id;
    }

    public Job<?, ?> getProject() {
        return getOwner();
    }

    public String getSearchUrl() {
        return getUrlName();
    }

    @Override
    protected LinesChartModel createChartModel(final String configuration) {
        var iterable = new BuildActionIterable<>(CoverageBuildAction.class, getLatestAction(),
                action -> getUrlName().equals(action.getUrlName()), CoverageBuildAction::getStatistics);
        return new CoverageTrendChart().create(iterable, ChartModelConfiguration.fromJson(configuration));
    }
}
