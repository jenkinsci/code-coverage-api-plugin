package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.Fraction;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.verb.POST;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import jenkins.model.Jenkins;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.util.FractionFormatter;
import io.jenkins.plugins.coverage.model.util.WebUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.util.JenkinsFacade;

/**
 * Dashboard column model which represents coverage metrics of different coverage types.
 *
 * @author Florian Orendi
 */
public class CoverageColumn extends ListViewColumn {

    static final String COVERAGE_NA_TEXT = "n/a";

    private CoverageColumnType selectedCoverageColumnType = new ProjectCoverage();

    private String columnName = Messages.Coverage_Column();
    private String coverageMetric = CoverageMetric.FILE.getName();
    private String coverageType = selectedCoverageColumnType.getDisplayName();

    /**
     * Empty constructor.
     */
    @DataBoundConstructor
    public CoverageColumn() {
        super();
    }

    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets the display name of the column.
     *
     * @param columnName
     *         The name of the column
     */
    @DataBoundSetter
    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }

    public String getCoverageMetric() {
        return coverageMetric;
    }

    /**
     * Defines which coverage type should be shown in the column.
     *
     * @param coverageType
     *         The {@link CoverageColumnType} to be shown
     */
    @DataBoundSetter
    public void setCoverageType(final String coverageType) {
        this.coverageType = coverageType;
        if (Messages.Project_Coverage_Delta_Type().equals(coverageType)) {
            selectedCoverageColumnType = new ProjectCoverageDelta();
        }
        else {
            // the default
            selectedCoverageColumnType = new ProjectCoverage();
        }
    }

    public String getCoverageType() {
        return coverageType;
    }

    /**
     * Defines which coverage metric should be shown in the column.
     *
     * @param coverageMetric
     *         The coverage metric to be shown
     */
    @DataBoundSetter
    public void setCoverageMetric(final String coverageMetric) {
        this.coverageMetric = coverageMetric;
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
        Optional<Fraction> coverageValue = getCoverageValue(job);
        if (coverageValue.isPresent()) {
            return selectedCoverageColumnType.formatCoverage(coverageValue.get());
        }
        return COVERAGE_NA_TEXT;
    }

    /**
     * Provides the coverage value of the selected coverage type and metric.
     *
     * @param job
     *         The processed job
     *
     * @return the coverage percentage
     */
    public Optional<Fraction> getCoverageValue(final Job<?, ?> job) {
        if (hasCoverageAction(job)) {
            CoverageBuildAction action = job.getLastCompletedBuild().getAction(CoverageBuildAction.class);
            return selectedCoverageColumnType.getCoverage(action, CoverageMetric.valueOf(coverageMetric))
                    .map(FractionFormatter::transformFractionToPercentage);
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
    public DisplayColors getDisplayColors(final Job<?, ?> job, final Optional<Fraction> coverage) {
        if (hasCoverageAction(job) && coverage.isPresent()) {
            return selectedCoverageColumnType.getDisplayColors(coverage.get());
        }
        return ColorProvider.DEFAULT_COLOR;
    }

    /**
     * Provides the relative URL which can be used for accessing the coverage report for specific coverage types.
     *
     * @return the relative URL or an empty string when there is no matching URL
     */
    public String getRelativeCoverageUrl() {
        return WebUtils.COVERAGE_DEFAULT_URL;
    }

    /**
     * Provides information about whether the coverage text will represent as a percentage without a sign.
     *
     * @return {@code true} whether the coverage text is shown as a percentage string, else {@code false}
     */
    public String getBackgroundColorFillPercentage(final String text) {
        if (Pattern.compile("\\d+(,\\d+)?%").matcher(text).matches()) {
            return text.replace(",", ".");
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
        final Run<?, ?> lastCompletedBuild = job.getLastCompletedBuild();
        return lastCompletedBuild != null && !lastCompletedBuild.getActions(CoverageBuildAction.class).isEmpty();
    }

    /**
     * Descriptor of the column.
     */
    @Extension(optional = true)
    public static class CoverageDescriptor extends ListViewColumnDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Coverage_Column();
        }

        /**
         * Returns the model for the select widget for selecting the displayed coverage type.
         *
         * @return the available coverage types
         */
        @POST
        public ListBoxModel doFillCoverageTypeItems() {
            ListBoxModel model = new ListBoxModel();
            if (new JenkinsFacade().hasPermission(Jenkins.READ)) {
                for (String coverageType : CoverageColumnType.getAvailableCoverageTypeNames()) {
                    model.add(coverageType);
                }
            }
            return model;
        }

        /**
         * Returns the model for the select widget for selecting the displayed coverage metric.
         *
         * @return the available coverage metrics
         */
        @POST
        public ListBoxModel doFillCoverageMetricItems() {
            ListBoxModel model = new ListBoxModel();
            if (new JenkinsFacade().hasPermission(Jenkins.READ)) {
                for (CoverageMetric coverageMetric : CoverageMetric.getAvailableCoverageMetrics()) {
                    model.add(coverageMetric.getName());
                }
            }
            return model;
        }
    }
}
