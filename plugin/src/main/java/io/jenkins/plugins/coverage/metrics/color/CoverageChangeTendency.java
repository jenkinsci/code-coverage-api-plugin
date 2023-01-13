package io.jenkins.plugins.coverage.metrics.color;

import edu.umd.cs.findbugs.annotations.NonNull;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider.DisplayColors;

/**
 * Provides the colorization for different coverage change tendencies.
 *
 * @author Florian Orendi
 */
public enum CoverageChangeTendency {

    INCREASED(ColorId.EXCELLENT),
    EQUALS(ColorId.AVERAGE),
    DECREASED(ColorId.INSUFFICIENT),
    NA(ColorId.WHITE);

    private final ColorId colorizationId;

    CoverageChangeTendency(final ColorId colorizationId) {
        this.colorizationId = colorizationId;
    }

    /**
     * Provides the {@link DisplayColors display colors} which match with the passed coverage change tendency.
     *
     * @param change
     *         The coverage change
     * @param colorProvider
     *         The {@link ColorProvider color provider} to be used
     *
     * @return the matching change level
     */
    public static DisplayColors getDisplayColorsForTendency(final Double change,
            @NonNull final ColorProvider colorProvider) {
        ColorId colorId;
        if (change == null || change.isNaN()) {
            colorId = NA.colorizationId;
        }
        else if (change > 0) {
            colorId = INCREASED.colorizationId;
        }
        else if (change < 0) {
            colorId = DECREASED.colorizationId;
        }
        else {
            colorId = EQUALS.colorizationId;
        }
        return colorProvider.getDisplayColorsOf(colorId);
    }

    public ColorId getColorizationId() {
        return colorizationId;
    }
}
