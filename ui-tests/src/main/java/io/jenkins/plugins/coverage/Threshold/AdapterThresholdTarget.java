package io.jenkins.plugins.coverage.Threshold;

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

    private final String name;

    AdapterThresholdTarget(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
