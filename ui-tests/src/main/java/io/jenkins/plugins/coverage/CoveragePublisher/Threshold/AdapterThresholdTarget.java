package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

/**
 * Enum for Options of {@link AdapterThreshold}.
 */
public enum AdapterThresholdTarget {
    AGGREGATED_REPORT("Aggregated Report"),
    REPORT("Report"),
    GROUP("Group"),
    PACKAGE("Package"),
    FILE("File"),
    CLASS("Class"),
    METHOD("Method"),
    INSTRUCTION("Instruction"),
    LINE("Line"),
    CONDITIONAL("Conditional"),
    ;

    private final String value;

    /**
     * Constructor of enum.
     *
     * @param value
     *         is value-attribute of option-tag.
     */
    AdapterThresholdTarget(final String value) {
        this.value = value;
    }

    /**
     * Get value of option-tag which should be selected.
     *
     * @return value of option-tag to select.
     */
    public String getValue() {
        return value;
    }
}
