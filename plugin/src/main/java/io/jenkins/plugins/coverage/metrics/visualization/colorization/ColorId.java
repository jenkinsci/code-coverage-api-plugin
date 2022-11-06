package io.jenkins.plugins.coverage.metrics.visualization.colorization;

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
    AVERAGE,
    GOOD,
    VERY_GOOD,
    EXCELLENT,

    BLACK,
    WHITE
}
