package io.jenkins.plugins.coverage;

public class QualityGate {

    private final int threshold;
    private final QualityGateType type;
    private final QualityGateStatus status;

    public QualityGate(final int threshold, final QualityGateType type, final boolean unstable) {
        this.threshold = threshold;
        this.type = type;
        this.status = unstable ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
    }
}
