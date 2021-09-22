package io.jenkins.plugins.coverage.model;

import java.util.Optional;

import hudson.model.Run;

/**
 * Navigates from the current results to the same results of any other build of the same job.
 *
 * @author Ullrich Hafner
 */
// FIXME: move to plugin-util or echarts
public class BuildResultNavigator {
    private static final String SLASH = "/";

    /**
     * Navigates from the current results to the same results of any other build of the same job.
     *
     * @param currentBuild
     *         the current build that owns the view results
     * @param currentAbsoluteBrowserUrl
     *         the absolute URL to the view results
     * @param resultId
     *         the ID of the static analysis results
     * @param selectedBuildDisplayName
     *         the selected build to open the new results for
     *
     * @return the URL to the results if possible
     */
    public Optional<String> getSameUrlForOtherBuild(final Run<?, ?> currentBuild, final String currentAbsoluteBrowserUrl,
            final String resultId, final String selectedBuildDisplayName) {
        for (Run<?, ?> run = currentBuild.getParent().getLastBuild(); run != null; run = run.getPreviousBuild()) {
            if (selectedBuildDisplayName.equals(run.getDisplayName())) {
                return getSameUrlForOtherBuild(currentBuild, currentAbsoluteBrowserUrl, resultId, run);
            }
        }
        return Optional.empty();
    }

    /**
     * Navigates from the current results to the same results of any other build of the same job.
     *
     * @param currentBuild
     *         the current build that owns the view results
     * @param viewUrl
     *         the absolute URL to the view results
     * @param resultId
     *         the ID of the static analysis results
     * @param selectedBuild
     *         the selected build to open the new results for
     *
     * @return the URL to the results if possible
     */
    public Optional<String> getSameUrlForOtherBuild(final Run<?, ?> currentBuild, final String viewUrl,
            final String resultId, final Run<?, ?> selectedBuild) {
        String match = SLASH + currentBuild.getNumber() + SLASH + resultId;
        if (viewUrl.contains(match)) {
            return Optional.of(viewUrl.replaceFirst(
                    match + ".*", SLASH + selectedBuild.getNumber() + SLASH + resultId));
        }
        return Optional.empty();
    }
}
