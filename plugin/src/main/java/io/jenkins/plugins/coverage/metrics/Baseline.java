package io.jenkins.plugins.coverage.metrics;

import java.util.function.BiFunction;

import org.jvnet.localizer.Localizable;

import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageLevel;

/**
 * The baseline for the code coverage computation.
 */
public enum Baseline {
    /**
     * Coverage of the whole project. This is an absolute value that might not change much from build to build.
     */
    PROJECT(Messages._Baseline_PROJECT(), "overview", CoverageLevel::getDisplayColorsOfCoverageLevel),
    /**
     * Difference between the project coverages of the current build and the reference build. Teams can use this delta
     * value to ensure that the coverage will not decrease.
     */
    PROJECT_DELTA(Messages._Baseline_PROJECT_DELTA(), "overview",
            CoverageChangeTendency::getDisplayColorsForTendency),
    /**
     * Coverage of the changed lines (e.g., within the changed lines of a pull or merge request) will focus on new or
     * changed code only.
     */
    CHANGE(Messages._Baseline_CHANGE(), "changeCoverage", CoverageLevel::getDisplayColorsOfCoverageLevel),
    /**
     * Difference between the project coverage and the change coverage of the current build. Teams can use this delta
     * value to ensure that the coverage of pull requests is better than the whole project coverage.
     */
    CHANGE_DELTA(Messages._Baseline_CHANGE_DELTA(), "changeCoverage",
            CoverageChangeTendency::getDisplayColorsForTendency),
    /**
     * Coverage of the changed files (e.g., within the files that have been touched in a pull or merge request) will
     * focus on new or changed code only.
     */
    FILE(Messages._Baseline_FILE(), "fileCoverage", CoverageLevel::getDisplayColorsOfCoverageLevel),
    /**
     * Difference between the project coverage and the file coverage of the current build. Teams can use this delta
     * value to ensure that the coverage of pull requests is better than the whole project coverage.
     */
    FILE_DELTA(Messages._Baseline_FILE_DELTA(), "fileCoverage", CoverageChangeTendency::getDisplayColorsForTendency),
    /**
     * Indirect changes of the overall code coverage that are not part of the changed code. These changes might occur,
     * if new tests will be added without touching the underlying code under test.
     */
    INDIRECT(Messages._Baseline_INDIRECT(), "indirectCoverage", CoverageLevel::getDisplayColorsOfCoverageLevel);

    private final Localizable title;
    private final String url;
    private final BiFunction<Double, ColorProvider, DisplayColors> colorMapper;

    Baseline(final Localizable title, final String url,
            final BiFunction<Double, ColorProvider, DisplayColors> colorMapper) {
        this.title = title;
        this.url = url;
        this.colorMapper = colorMapper;
    }

    public String getTitle() {
        return title.toString();
    }

    public String getUrl() {
        return "#" + url;
    }

    /**
     * Returns the display colors to use render a value of this baseline.
     *
     * @param value
     *         the value to render
     * @param colorProvider
     *         the color provider to use
     *
     * @return the display colors to use
     */
    public DisplayColors getDisplayColors(final double value, final ColorProvider colorProvider) {
        return colorMapper.apply(value, colorProvider);
    }
}
