package io.jenkins.plugins.coverage.metrics.color;

/**
 * Represents different types of color schemes that can be selected in order to load the matching color palette.
 *
 * @author Florian Orendi
 */
public enum ColorScheme {

    /**
     * The default colors.
     */
    DEFAULT,
    /**
     * Colors which are used if the dark mode is activated.
     */
    DARK_MODE,
    /**
     * Colors which are usable in case of color blindness.
     */
    COLOR_BLINDNESS
}
