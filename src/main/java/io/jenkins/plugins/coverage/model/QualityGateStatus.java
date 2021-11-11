package io.jenkins.plugins.coverage.model;

/**
 * QualityGateStatus should be returned based on the evaluation of a CoverageNode using QualityGates.
 */
public enum QualityGateStatus {
    FAILED("failed"),
    WARNING("warning"),
    SUCCESSFUL("successful"),
    INACTIVE("inactive");

    QualityGateStatus(final String qualityGateStatusString) {

    }
}
