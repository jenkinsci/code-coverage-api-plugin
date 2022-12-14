package io.jenkins.plugins.coverage.metrics;

import org.jvnet.localizer.Localizable;

/**
 * The baseline for the code coverage computation.
 */
public enum Baseline {
    /**
     * Coverage of the whole project. This is an absolute value that might not change much from build to build.
     */
    PROJECT(Messages._Baseline_PROJECT(), "lineCoverage"),
    /**
     * Difference between the project coverages of the current build and the reference build. Teams can use this delta
     * value to ensure that the coverage will not decrease.
     */
    PROJECT_DELTA(Messages._Baseline_PROJECT_DELTA(), "lineCoverage"),
    /**
     * Coverage of the changed lines (e.g., within the changed lines of a pull or merge request) will focus on new or
     * changed code only.
     */
    CHANGE(Messages._Baseline_CHANGE(), "changeCoverage"),
    /**
     * Difference between the project coverage and the change coverage of the current build. Teams can use this delta
     * value to ensure that the coverage of pull requests is better than the whole project coverage.
     */
    CHANGE_DELTA(Messages._Baseline_CHANGE_DELTA(), "changeCoverage"),
    /**
     * Coverage of the changed files (e.g., within the files that have been touched in a pull or merge request) will
     * focus on new or changed code only.
     */
    FILE(Messages._Baseline_FILE(), "fileCoverage"),
    /**
     * Difference between the project coverage and the file coverage of the current build. Teams can use this delta
     * value to ensure that the coverage of pull requests is better than the whole project coverage.
     */
    FILE_DELTA(Messages._Baseline_FILE_DELTA(), "fileCoverage"),
    /**
     * Indirect changes of the overall code coverage that are not part of the changed code. These changes might occur,
     * if new tests will be added without touching the underlying code under test.
     */
    INDIRECT(Messages._Baseline_INDIRECT(), "indirectCoverage");

    private final Localizable title;
    private final String url;

    Baseline(final Localizable title, final String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title.toString();
    }

    public String getUrl() {
        return url;
    }
}
