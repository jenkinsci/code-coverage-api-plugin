package io.jenkins.plugins.coverage.metrics;

import org.jvnet.localizer.Localizable;

public enum Baseline {
    PROJECT(Messages._Baseline_PROJECT(), "lineCoverage"),
    PROJECT_DELTA(Messages._Baseline_PROJECT_DELTA(), "lineCoverage"),
    CHANGE(Messages._Baseline_CHANGE(), "changeCoverage"),
    CHANGE_DELTA(Messages._Baseline_CHANGE_DELTA(), "changeCoverage"),
    FILE(Messages._Baseline_FILE(), "fileCoverage"),
    FILE_DELTA(Messages._Baseline_FILE_DELTA(), "fileCoverage"),
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
