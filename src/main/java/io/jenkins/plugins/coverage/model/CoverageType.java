package io.jenkins.plugins.coverage.model;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the different types of how coverage can be represented.
 *
 * @author Florian Orendi
 */
public enum CoverageType {

    /**
     * The total coverage of a project.
     */
    PROJECT("Project Coverage"),

    /**
     * The total project coverage delta between a build and its reference build.
     */
    PROJECT_DELTA("Project Coverage Delta"),

    // CHANGE("Change Coverage"), TODO: has to be implemented first

    UNDEFINED("Undefined");

    private final String type;

    CoverageType(final String type) {
        this.type = type;
    }

    /**
     * Provides the {@link CoverageType} for the passed text representation.
     *
     * @param coverageType
     *         The coverage type text representation
     *
     * @return the matching {@link CoverageType}
     */
    public static CoverageType getCoverageTypeOf(final String coverageType) {
        if (PROJECT.type.equals(coverageType)) {
            return PROJECT;
        }
        else if (PROJECT_DELTA.type.equals(coverageType)) {
            return PROJECT_DELTA;
        }
        return UNDEFINED;
    }

    /**
     * Provides all available values of {@link CoverageType}.
     *
     * @return the available coverage types
     */
    public static List<CoverageType> getAvailableCoverageTypes() {
        return Arrays.asList(PROJECT, PROJECT_DELTA);
    }

    public String getType() {
        return type;
    }
}
