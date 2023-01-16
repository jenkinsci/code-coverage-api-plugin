package io.jenkins.plugins.coverage.metrics.steps;

import java.util.Collection;
import java.util.Locale;

import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.util.QualityGateEvaluator;
import io.jenkins.plugins.util.QualityGateResult;
import io.jenkins.plugins.util.QualityGateStatus;

/**
 * Evaluates a given set of quality gates.
 *
 * @author Johannes Walter
 */
class CoverageQualityGateEvaluator extends QualityGateEvaluator<CoverageQualityGate> {
    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final CoverageStatistics statistics;

    CoverageQualityGateEvaluator(final Collection<? extends CoverageQualityGate> qualityGates, final CoverageStatistics statistics) {
        super(qualityGates);

        this.statistics = statistics;
    }

    @Override
    protected void evaluate(final CoverageQualityGate qualityGate, final QualityGateResult result) {
        var baseline = qualityGate.getBaseline();
        var possibleValue = statistics.getValue(baseline, qualityGate.getMetric());
        if (possibleValue.isPresent()) {
            var actualValue = possibleValue.get();

            var status = actualValue.isBelowThreshold(
                    qualityGate.getThreshold()) ? qualityGate.getStatus() : QualityGateStatus.PASSED;
            result.add(qualityGate, status, FORMATTER.format(actualValue, Locale.ENGLISH));
        }
        else {
            result.add(qualityGate, qualityGate.getStatus(), "n/a");
        }
    }
}
