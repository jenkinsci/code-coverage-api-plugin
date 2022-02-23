package io.jenkins.plugins.coverage.model.util;

/**
 * Provides URLs for accessing coverage reports.
 *
 * @author Florian Orendi
 */
public class WebUtils {

    private WebUtils() {
        // prevents instantiation
    }

    /**
     * Relative URL for accessing the default page of the coverage report of a build.
     */
    public static final String COVERAGE_DEFAULT_URL = "coverage";
}
