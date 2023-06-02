package io.jenkins.plugins.coverage.metrics.steps;

import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.Metric;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;

/**
 * Provides a token that evaluates to the number of issues.
 *
 * @author Ullrich Hafner
 */
@Extension(optional = true)
public class CoverageTokenMacro extends DataBoundTokenMacro {
    private static final ElementFormatter FORMATTER = new ElementFormatter();
    static final String COVERAGE = "COVERAGE";

    private String id = CoverageRecorder.DEFAULT_ID;
    private Metric metric = Metric.LINE;
    private Baseline baseline = Baseline.PROJECT;

    /**
     * Determines which ID should be used to choose the action for the macro evaluation.
     *
     * @param id
     *         the ID of the results to select
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Defines which metric should be used when evaluating the macro.
     *
     * @param metric
     *         the metric of the values to show
     */
    @Parameter
    public void setMetric(final String metric) {
        this.metric = Metric.valueOf(metric);
    }

    /**
     * Defines which baseline should be used when evaluating the macro.
     *
     * @param baseline
     *         the baseline of the values to show
     */
    @Parameter
    public void setBaseline(final String baseline) {
        this.baseline = Baseline.valueOf(baseline);
    }

    @Override
    public boolean acceptsMacroName(final String macroName) {
        return COVERAGE.equals(macroName);
    }

    @Override
    public String evaluate(final AbstractBuild<?, ?> abstractBuild, final TaskListener taskListener,
            final String macroName) {
        return extractCoverageFromBuild(abstractBuild);
    }

    @Override
    public String evaluate(final Run<?, ?> run, final FilePath workspace, final TaskListener listener,
            final String macroName) {
        return extractCoverageFromBuild(run);
    }

    private String extractCoverageFromBuild(final Run<?, ?> run) {
        var statistics = run.getActions(CoverageBuildAction.class).stream()
                .filter(createIdFilter())
                .map(CoverageBuildAction::getStatistics)
                .findFirst();
        if (statistics.isPresent()) {
            var value = statistics.get().getValue(baseline, metric);
            if (value.isPresent()) {
                return FORMATTER.format(value.get());
            }
        }
        return "n/a";
    }

    private Predicate<CoverageBuildAction> createIdFilter() {
        if (StringUtils.isBlank(id)) {
            return jobAction -> true;
        }
        else {
            return jobAction -> jobAction.getUrlName().equals(id);
        }
    }
}

