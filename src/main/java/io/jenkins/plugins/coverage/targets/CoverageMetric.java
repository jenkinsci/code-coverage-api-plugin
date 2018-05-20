package io.jenkins.plugins.coverage.targets;

/**
 * @author connollys
 * @author manolo
 * @since 10-Jul-2007 14:59:50
 */
public enum CoverageMetric {
    REPORTS("Reports"),
    GROUPS("Groups"),
    PACKAGES("Packages"),
    FILES("Files"),
    CLASSES("Classes"),
    METHOD("Methods"),
    LINE("Lines"),
    CONDITIONAL("Conditionals");

    private final String name;

    CoverageMetric(String name) {
        this.name = name;
    }

    /**
     * Return the name of this metric element.
     * <p>
     * Note: This getter has to be evaluated each time in a non static
     * way because the user could change its language
     *
     * @return Value for property 'displayName'.
     */
    public String getName() {
        return name;
    }
}

