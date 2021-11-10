package io.jenkins.plugins.coverage.model;

public enum QualityGateStatus {
    FAILED("failed"),
    WARNING("warning"),
    SUCCESSFUL("successful"),
    INACTIVE("inactive");

    QualityGateStatus(final String qualityGateStatusString) {

    }
}
