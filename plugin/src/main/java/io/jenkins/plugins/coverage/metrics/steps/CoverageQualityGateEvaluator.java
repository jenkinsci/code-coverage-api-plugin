package io.jenkins.plugins.coverage.metrics.steps;

import java.util.Collection;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.FractionValue;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.SafeFraction;
import edu.hm.hafner.coverage.Value;

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
    private static final Fraction HUNDRED = Fraction.getFraction("100.0");

    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final CoverageStatistics statistics;

    CoverageQualityGateEvaluator(final Collection<? extends CoverageQualityGate> qualityGates,
            final CoverageStatistics statistics) {
        super(qualityGates);

        this.statistics = statistics;
    }

    @Override
    protected void evaluate(final CoverageQualityGate qualityGate, final QualityGateResult result) {
        var baseline = qualityGate.getBaseline();
        var possibleValue = statistics.getValue(baseline, qualityGate.getMetric());
        if (possibleValue.isPresent()) {
            var actualValue = convertActualValue(possibleValue.get());

            var status = actualValue.isOutOfValidRange(
                    qualityGate.getThreshold()) ? qualityGate.getStatus() : QualityGateStatus.PASSED;
            result.add(qualityGate, status, FORMATTER.format(possibleValue.get(), Locale.ENGLISH));
        }
        else {
            result.add(qualityGate, QualityGateStatus.INACTIVE, "n/a");
        }
    }

    /**
     * Converts the actual value to a percentage if necessary. Delta values are internally stored as fractions, but
     * users expect percentages when they are displayed or used in thresholds.
     *
     * @param value
     *         the actual stored value
     *
     * @return the converted value
     */
    private Value convertActualValue(final Value value) {
        var metric = value.getMetric();
        if (metric.equals(Metric.COMPLEXITY)
                || metric.equals(Metric.COMPLEXITY_MAXIMUM)
                || metric.equals(Metric.LOC)) {
            return value; // ignore integer based metrics
        }
        if (value instanceof FractionValue) { // delta percentage
            return new FractionValue(metric, covertToPercentage((FractionValue) value));
        }

        return value;
    }

    private Fraction covertToPercentage(final FractionValue value) {
        return new SafeFraction(value.getFraction()).multiplyBy(HUNDRED);
    }
}
