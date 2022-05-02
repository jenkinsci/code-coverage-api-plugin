package io.jenkins.plugins.coverage.model.visualization.colorization;

/**
 * Provides IDs for colors which are used within this plugin in order to separate the color palette from the logic.
 *
 * @author Florian Orendi
 */
public enum ColorId {
    INSUFFICIENT,
    VERY_BAD,
    BAD,
    INADEQUATE,
    BELOW_AVERAGE,
    AVERAGE,
    ABOVE_AVERAGE,
    GOOD,
    VERY_GOOD,
    EXCELLENT,
    OUTSTANDING,

    BLACK,
    WHITE
}
