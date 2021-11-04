package io.jenkins.plugins.coverage;

import java.util.List;

import io.jenkins.plugins.coverage.model.CoverageNode;

public class QualityGateEvaluator {

    public QualityGateStatus evaluate(CoverageNode coverageNode, List<QualityGate> qualityGates) {
        return QualityGateStatus.FAILED;
    }

}
