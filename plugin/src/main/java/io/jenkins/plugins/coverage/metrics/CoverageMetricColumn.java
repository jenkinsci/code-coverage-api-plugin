package io.jenkins.plugins.coverage.metrics;

import java.util.Optional;
import java.util.regex.Pattern;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FractionValue;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.verb.POST;
import org.jenkinsci.Symbol;
import hudson.Extension;
import hudson.Functions;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;

import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.util.JenkinsFacade;

/**
 * Dashboard column model which represents coverage metrics of different coverage types.
 *
 * @author Florian Orendi
 */
public class CoverageMetricColumn extends ListViewColumn {
    private static final ElementFormatter FORMATTER = new ElementFormatter();

    private String columnName = Messages.Coverage_Column();
    private Metric metric = Metric.LINE;
    private Baseline baseline = Baseline.PROJECT;

    /**
     * Creates a new column.
     */
    @DataBoundConstructor
    public CoverageMetricColumn() {
        super();
    }

    /**
     * Sets the display name of the column.
     *
     * @param columnName
     *         the human-readable name of the column
     */
    @DataBoundSetter
    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets the baseline of the values that will be shown.
     *
     * @param baseline
     *         the baseline to use
     */
    @DataBoundSetter
    public void setBaseline(final Baseline baseline) {
        this.baseline = baseline;
    }

    public Baseline getBaseline() {
        return baseline;
    }

    /**
     * Sets the metric of the values that will be shown.
     *
     * @param metric
     *         the metric to use
     */
    @DataBoundSetter
    public void setMetric(final Metric metric) {
        this.metric = metric;
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * Provides a text which represents the coverage percentage of the selected coverage type and metric.
     *
     * @param job
     *         The processed job
     *
     * @return the coverage text
     */
    public String getCoverageText(final Job<?, ?> job) {
        Optional<? extends Value> coverageValue = getCoverageValue(job);
        if (coverageValue.isPresent()) {
            return FORMATTER.format(coverageValue.get(), Functions.getCurrentLocale());
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Provides the coverage value of the selected coverage type and metric.
     *
     * @param job
     *         The processed job
     *
     * @return the coverage percentage
     */
    public Optional<? extends Value> getCoverageValue(final Job<?, ?> job) {
        if (hasCoverageAction(job)) {
            CoverageBuildAction action = job.getLastCompletedBuild().getAction(CoverageBuildAction.class);

            return action.getStatistics().getValue(getBaseline(), metric);
        }
        return Optional.empty();
    }

    /**
     * Provides the line color for representing the passed coverage value.
     *
     * @param job
     *         The processed job
     * @param coverage
     *         The coverage value as percentage
     *
     * @return the line color as hex string
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public DisplayColors getDisplayColors(final Job<?, ?> job, final Optional<? extends Value> coverage) {
        if (hasCoverageAction(job) && coverage.isPresent()) {
            if (coverage.get() instanceof Coverage) {
                return baseline.getDisplayColors(((Coverage)coverage.get()).getCoveredPercentage().doubleValue() * 100.0, ColorProviderFactory.createDefaultColorProvider());
            }
            else if (coverage.get() instanceof FractionValue) {
                return baseline.getDisplayColors(((FractionValue)coverage.get()).getFraction().doubleValue(), ColorProviderFactory.createDefaultColorProvider());
            }
        }
        return ColorProvider.DEFAULT_COLOR;
    }

    /**
     * Provides the relative URL which can be used for accessing the coverage report.
     *
     * @param job
     *         The processed job
     *
     * @return the relative URL or an empty string when there is no matching URL
     */
    public String getRelativeCoverageUrl(final Job<?, ?> job) {
        if (hasCoverageAction(job)) {
            CoverageBuildAction action = job.getLastCompletedBuild().getAction(CoverageBuildAction.class);
            return action.getUrlName() + "/" + baseline.getUrl();
        }
        return "";
    }

    /**
     * Transforms percentages with a ',' decimal separator to a representation using a '.' in order to use the
     * percentage for styling HTML tags.
     *
     * @param percentage
     *         The text representation of a percentage
     *
     * @return the formatted percentage string
     */
    public String getBackgroundColorFillPercentage(final String percentage) {
        String formattedPercentage = percentage.replace(",", ".");
        if (Pattern.compile("\\d+(\\.\\d+)?%").matcher(formattedPercentage).matches()) {
            return formattedPercentage;
        }
        return "100%";
    }

    /**
     * Checks whether a {@link CoverageBuildAction} exists within the completed build.
     *
     * @param job
     *         The processed job
     *
     * @return {@code true} whether the action exists, else {@code false}
     */
    private boolean hasCoverageAction(final Job<?, ?> job) {
        Run<?, ?> lastCompletedBuild = job.getLastCompletedBuild();
        return lastCompletedBuild != null && !lastCompletedBuild.getActions(CoverageBuildAction.class).isEmpty();
    }

    /**
     * Descriptor of the column.
     */
    @Extension(optional = true)
    @Symbol("coverageTotalsColumn")
    public static class CoverageMetricColumnDescriptor extends ListViewColumnDescriptor {
        /**
         * Creates a new descriptor.
         */
        @SuppressWarnings("unused") // Required for Jenkins Extensions
        public CoverageMetricColumnDescriptor() {
            this(new JenkinsFacade());
        }

        @VisibleForTesting
        CoverageMetricColumnDescriptor(final JenkinsFacade jenkins) {
            this.jenkins = jenkins;
        }

        private final JenkinsFacade jenkins;

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Coverage_Column();
        }

        /**
         * Returns a model with all {@link Metric metrics} that can be used in quality gates.
         *
         * @param project
         *         the project that is configured
         *
         * @return a model with all {@link Metric metrics}.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
        public ListBoxModel doFillMetricItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (jenkins.hasPermission(Item.CONFIGURE, project)) {
                return FORMATTER.getMetricItems();
            }
            return new ListBoxModel();
        }

        /**
         * Returns a model with all {@link Metric metrics} that can be used in quality gates.
         *
         * @param project
         *         the project that is configured
         *
         * @return a model with all {@link Metric metrics}.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
        public ListBoxModel doFillBaselineItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (jenkins.hasPermission(Item.CONFIGURE, project)) {
                return FORMATTER.getBaselineItems();
            }
            return new ListBoxModel();
        }
    }
}
