package io.jenkins.plugins.coverage.targets;

/**
 * Type of program construct being covered.
 *
 * @author Stephen Connolly
 * @author manolo
 * @since 22-Aug-2007 18:36:01
 */
public enum CoverageElement {
    AGGREGATED_REPORT("Aggregated Report"),
    REPORT("Report", AGGREGATED_REPORT),
    JAVA_GROUP("Group", REPORT),
    JAVA_PACKAGE("Package", JAVA_GROUP),
    JAVA_FILE("File", JAVA_PACKAGE),
    JAVA_CLASS("Class", JAVA_FILE),
    JAVA_METHOD("Method", JAVA_CLASS);

    private final CoverageElement parent;
    private final String name;

    CoverageElement(String name) {
        this.parent = null;
        this.name = name;
    }

    CoverageElement(String name, CoverageElement parent) {
        this.parent = parent;
        this.name = name;
    }


    /**
     * Getter for property 'parent'.
     *
     * @return Value for property 'parent'.
     */
    public CoverageElement getParent() {
        return parent;
    }

    /**
     * Return displayName of this coverage element.
     * <p>
     * Note: This getter has to be evaluated each time in a non static
     * way because the user could change its language
     *
     * @return Value for property 'displayName'.
     */
    public String getDisplayName() {
        return name;
    }
}
