package io.jenkins.plugins.coverage.metrics.restapi;

/**
 * Defines the type of line coverage assigned to any {@link ModifiedLinesBlock} object.
 */
enum LineCoverageType {
    COVERED,
    MISSED,
    PARTIALLY_COVERED
}
