package io.jenkins.plugins.coverage.model.visualization.colorization;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;

public enum CoverageChangeTendency {

    INCREASED(ColorId.GREEN),
    EQUALS(ColorId.LIGHT_YELLOW),
    DECREASED(ColorId.RED),
    NA(ColorId.WHITE);

    private final ColorId colorizationId;

    CoverageChangeTendency(final ColorId colorizationId) {
        this.colorizationId = colorizationId;
    }

    /**
     * Provides the {@link DisplayColors} which matches with the passed coverage change.
     *
     * @param change
     *         The coverage change
     *
     * @return the matching change level
     */
    public static DisplayColors getDisplayColorsForTendency(final Double change, final ColorProvider colorProvider) {
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
