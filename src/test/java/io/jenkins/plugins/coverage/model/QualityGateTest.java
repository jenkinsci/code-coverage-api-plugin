package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.exception.QualityGatesInvalidException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class QualityGateTest {

    @Test
    void shouldCreateAndChangeValuesOfQualityGate() throws QualityGatesInvalidException {
        QualityGate gate = new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.FAILED);

        assertThat(gate.getQualityGateStatus()).isEqualTo(QualityGateStatus.FAILED);
        assertThat(gate.getMetric()).isEqualTo(CoverageMetric.PACKAGE);
        assertThat(gate.getLimit()).isEqualTo(30);

        gate.setLimit(50);
        gate.setQualityGateStatus(QualityGateStatus.WARNING);
        gate.setMetric(CoverageMetric.METHOD);

        assertThat(gate.getQualityGateStatus()).isEqualTo(QualityGateStatus.WARNING);
        assertThat(gate.getMetric()).isEqualTo(CoverageMetric.METHOD);
        assertThat(gate.getLimit()).isEqualTo(50);

    }

    @Test
    void shouldThrowQualityGatesInvalidException() {

        Exception exception = assertThrows(QualityGatesInvalidException.class, () ->
                new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.INACTIVE)
        );

        String expectedMessage = "Quality Gates can only have FAILED or WARNING as returning QualityGateStatus.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        Exception exception2 = assertThrows(QualityGatesInvalidException.class, () ->
                new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.SUCCESSFUL)
        );

        String expectedMessage2 = "Quality Gates can only have FAILED or WARNING as returning QualityGateStatus.";
        String actualMessage2 = exception2.getMessage();
        assertTrue(actualMessage2.contains(expectedMessage2));

    }

    @Test
    void testEquals() throws QualityGatesInvalidException {
        QualityGate gate1 = new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.FAILED);
        QualityGate gate2 = new QualityGate(CoverageMetric.PACKAGE, 50, QualityGateStatus.FAILED);
        assertThat(gate1).isEqualTo(gate1);
        assertThat(gate1).isEqualTo(gate2);

        QualityGate gate3 = new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.WARNING);
        assertThat(gate1).isNotEqualTo(gate3);
    }

    @Test
    void testHashCode() throws QualityGatesInvalidException {
        QualityGate gate1 = new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.FAILED);
        QualityGate gate2 = new QualityGate(CoverageMetric.PACKAGE, 50, QualityGateStatus.FAILED);
        assertThat(gate1.hashCode()).isEqualTo(gate2.hashCode());

        QualityGate gate3 = new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.WARNING);
        assertThat(gate1.hashCode()).isNotEqualTo(gate3.hashCode());

    }

    @Test
    void testCompareTo() throws QualityGatesInvalidException {

        QualityGate gatePackageFailure = new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.FAILED);
        QualityGate gateLineWarning = new QualityGate(CoverageMetric.LINE, 40, QualityGateStatus.WARNING);
        QualityGate gateFileFailure = new QualityGate(CoverageMetric.FILE, 30, QualityGateStatus.FAILED);
        QualityGate gateBranchFailure = new QualityGate(CoverageMetric.BRANCH, 30, QualityGateStatus.FAILED);

        assertThat(gatePackageFailure.compareTo(gateLineWarning)).isLessThan(0);
        assertThat(gateLineWarning.compareTo(gateFileFailure)).isGreaterThan(0);
        assertThat(gateFileFailure.compareTo(gateBranchFailure)).isLessThan(0);
        assertThat(gateFileFailure.compareTo(gateBranchFailure)).isLessThan(0);
    }
}