package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

/**
 * Enum for Options of {@link GlobalThreshold}.
 */
public enum GlobalThresholdTarget {

    AGGREGATED_REPORT("Aggregated Report"),
    REPORT("Report"),
    GROUP("Group"),
    PACKAGE("Package"),
    DIRECTORY("Directory"),
    FILE("File"),
    CLASS("Class"),
    METHOD("Method"),
    FUNCTION("Function"),
    INSTRUCTION("Instruction"),
    LINE("Line"),
    CONDITIONAL("Conditional"),
    ;

    private final String value;

    /**
     * Constructor of enum.
     * @param value is value-attribute of option-tag.
     */
    GlobalThresholdTarget(final String value) {
        this.value = value;
    }

    /**
     * Get value of option-tag which should be selected.
     * @return value of option-tag to select.
     */
    public String getValue() {
        return value;
    }
}
