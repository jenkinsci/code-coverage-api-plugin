package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

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

    private final String name;

    GlobalThresholdTarget(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
