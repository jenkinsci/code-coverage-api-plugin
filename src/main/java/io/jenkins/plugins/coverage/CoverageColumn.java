package io.jenkins.plugins.coverage;


import hudson.Extension;
import hudson.model.Job;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Column that shows coverage of a job.
 */
public class CoverageColumn extends ListViewColumn {

    @DataBoundConstructor
    public CoverageColumn() {
    }

    @Override
    public String getColumnCaption() {
        return Messages.CoverageColumn_Caption();
    }

    public String getUrl(final Job<?, ?> project) {
        CoverageAction action = getAction(project);
        if (action == null) {
            return null;
        }

        return project.getUrl() + action.getUrlName();
    }

    /**
     * Get the CoverageAction of last successful build of this project.
     *
     * @param project the selected project
     * @return CoverageAction of last successful build
     */
    public CoverageAction getAction(final Job<?, ?> project) {
        CoverageProjectAction action = project.getAction(CoverageProjectAction.class);

        if (action != null) {
            return action.getLastResult();
        }

        return null;
    }

    /**
     * Returns whether a link can be shown that shows the results of the referenced project action for the selected job.
     *
     * @param project the selected project
     * @return the URL of the project action
     */
    public boolean hasUrl(final Job<?, ?> project) {
        return getAction(project) != null;
    }


    public String getCoverage(final Job<?, ?> project) {
        CoverageAction action = getAction(project);
        if (action == null || action.getResult() == null) {
            return Messages.CoverageColumn_CoverageEmpty();
        }

        CoverageResult result = action.getResult();
        Ratio coverageRatio = result.getCoverage(CoverageElement.LINE);
        if (coverageRatio == null) {
            return Messages.CoverageColumn_CoverageEmpty();
        }

        String coveragePercentage = coverageRatio.getPercentageString();
        coveragePercentage = StringUtils.stripStart(coveragePercentage, "0");
        coveragePercentage = StringUtils.stripEnd(coveragePercentage, "0");
        if (coveragePercentage.equals("0") || coveragePercentage.equals("")) {
            return "0%";
        }

        return coveragePercentage + "%";
    }

    /**
     * Descriptor for the column.
     */
    @Extension
    public static class ColumnDescriptor extends ListViewColumnDescriptor {
        @Override
        public boolean shownByDefault() {
            return false;
        }

        @Override
        public String getDisplayName() {
            return Messages.CoverageColumn_ColumnName();
        }
    }
}
