package io.jenkins.plugins.coverage.model;

/**
 * Status of quality gates.
 *
 * @author Adrian Germeck
 */

public enum QualityGateStatus {
    FAILED("failed"),
    WARNING("warning"),
    SUCCESSFUL("successful"),
    INACTIVE("inactive");

    QualityGateStatus(final String qualityGateStatusString) {
    }
}