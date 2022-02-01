package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

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
import io.jenkins.plugins.coverage.model.CoverageType;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.util.WebUtil;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;
import io.jenkins.plugins.util.JenkinsFacade;

/**
 * Dashboard column model which represents coverage metrics of different coverage types.
 *
 * @author Florian Orendi
 */
public class CodeCoverageColumn extends ListViewColumn {

    static final String COVERAGE_NA_TEXT = "n/a";

    private String columnName = Messages.Code_Coverage_Column();
    private String coverageType = CoverageType.PROJECT.getType();
    private String coverageMetric = CoverageMetric.FILE.getName();

    /**
     * Empty constructor.
     */
    @DataBoundConstructor
    public CodeCoverageColumn() {
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
     *         The coverage type to be shown
     */
    @DataBoundSetter
    public void setCoverageType(final String coverageType) {
        this.coverageType = coverageType;
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
        if (hasCoverageAction(job)) {
            Optional<BigDecimal> coverageValue = getCoverageValue(job);
            if (coverageValue.isPresent()) {
                return coverageValue.get().toPlainString() + " %";
            }
        }
        return COVERAGE_NA_TEXT;
    }

    /**
     * Provides the coverage value of the selected coverage type and metric.
     *
     * @param job
     *         The processed job
     *
     * @return the coverage value
     */
    public Optional<BigDecimal> getCoverageValue(final Job<?, ?> job) {
        final Run<?, ?> lastCompletedBuild = job.getLastCompletedBuild();
        if (lastCompletedBuild == null) {
            return Optional.empty();
        }
        final CoverageBuildAction action = lastCompletedBuild.getAction(CoverageBuildAction.class);
        if (action == null) {
            return Optional.empty();
        }
        final CoverageType type = CoverageType.getCoverageTypeOf(coverageType);
        final double coverage;
        switch (type) {
            case PROJECT:
                if (!action.hasCoverage(CoverageMetric.valueOf(coverageMetric))) {
                    return Optional.empty();
                }
                coverage = action
                        .getCoverage(CoverageMetric.valueOf(coverageMetric))
                        .getCoveredPercentage() * 100.0;
                break;
            case PROJECT_DELTA:
                if (!action.hasDelta(CoverageMetric.valueOf(coverageMetric))) {
                    return Optional.empty();
                }
                coverage = action.getDelta().get(CoverageMetric.valueOf(coverageMetric));
                break;
            default:
                return Optional.empty();
        }
        return Optional.of(
                new BigDecimal(String.valueOf(coverage))
                        .setScale(3, RoundingMode.DOWN)
                        .stripTrailingZeros());
    }

    /**
     * Provides the line color for representing the passed coverage value.
     *
     * @param job
     *         The processed job
     * @param coverage
     *         The coverage value
     *
     * @return the line color as hex string
     */
    public String getLineColor(final Job<?, ?> job, final BigDecimal coverage) {
        final Color color;
        if (hasCoverageAction(job) && coverage != null) {
            final CoverageType type = CoverageType.getCoverageTypeOf(coverageType);
            if (type == CoverageType.PROJECT) {
                color = CoverageLevel.getCoverageValueOf(coverage.doubleValue()).getLineColor();
            }
            else if (type == CoverageType.PROJECT_DELTA) {
                color = CoverageChangeTendency.getCoverageTendencyOf(coverage.doubleValue()).getLineColor();
            }
            else {
                color = ColorUtils.NA_LINE_COLOR;
            }
        }
        else {
            color = CoverageChangeTendency.NA.getLineColor();
        }
        return ColorUtils.colorAsHex(color);
    }

    /**
     * Provides the fill color for representing the passed coverage value.
     *
     * @param job
     *         The processed job
     * @param coverage
     *         The coverage value
     *
     * @return the fill color as hex string
     */
    public String getFillColor(final Job<?, ?> job, final BigDecimal coverage) {
        final Color color;
        if (hasCoverageAction(job) && coverage != null) {
            final CoverageType type = CoverageType.getCoverageTypeOf(coverageType);
            if (type == CoverageType.PROJECT) {
                color = CoverageLevel.getBlendedFillColorOf(coverage.doubleValue());
            }
            else if (type == CoverageType.PROJECT_DELTA) {
                color = CoverageChangeTendency.getCoverageTendencyOf(coverage.doubleValue()).getFillColor();
            }
            else {
                color = ColorUtils.NA_FILL_COLOR;
            }
        }
        else {
            color = ColorUtils.NA_FILL_COLOR;
        }
        return ColorUtils.colorAsHex(color);
    }

    /**
     * Provides the relative URL which can be used for accessing the coverage report for specific coverage types.
     *
     * @return the relative URL or an empty string when there is no matching URL
     */
    public String getRelativeCoverageUrl() {
        if (coverageType.equals(CoverageType.PROJECT.getType())
                || coverageType.equals(CoverageType.PROJECT_DELTA.getType())) {
            return WebUtil.getRelativeCoverageDefaultUrl();
        }
        return "";
    }

    /**
     * Checks whether a {@link CoverageBuildAction} exists within the passed job.
     *
     * @param job
     *         The processed job
     *
     * @return {@code true} whether the action exists, else {@code false}
     */
    private boolean hasCoverageAction(final Job<?, ?> job) {
        final Run<?, ?> lastSuccessfulBuild = job.getLastCompletedBuild();
        return lastSuccessfulBuild != null && !lastSuccessfulBuild.getActions(CoverageBuildAction.class).isEmpty();
    }

    /**
     * Descriptor of the column.
     */
    @Extension(optional = true)
    public static class CoverageDescriptor extends ListViewColumnDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Code_Coverage_Column();
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
                for (CoverageType coverageType : CoverageType.getAvailableCoverageTypes()) {
                    model.add(coverageType.getType());
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
