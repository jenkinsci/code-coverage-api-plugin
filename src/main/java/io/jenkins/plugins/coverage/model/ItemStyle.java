package io.jenkins.plugins.coverage.model;

/**
 * Item style for a chart.
 * <p>
 * This class will be automatically converted to a JSON object.
 * </p>
 *
 * @author Ullrich Hafner
 */
public class ItemStyle {
    private final String color;

    /**
     * Creates a new {@link ItemStyle} instance with the specified color.
     *
     * @param color
     *         the color to use
     */
    public ItemStyle(final String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
