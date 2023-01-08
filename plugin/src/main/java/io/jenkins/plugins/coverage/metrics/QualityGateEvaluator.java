package io.jenkins.plugins.coverage.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import edu.hm.hafner.metric.Metric;

import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateCriticality;

/**
 * Evaluates a given set of quality gates.
 *
 * @author Johannes Walter
 */
public class QualityGateEvaluator {
    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final List<QualityGate> qualityGates = new ArrayList<>();

    /**
     * Enforces the quality gates for the specified run.
     *
     * @param report
     *         the report to evaluate
     *
     * @return result of the evaluation, expressed by a build state
     */
    public QualityGateResult evaluate(final CoverageStatistics report) {
        if (qualityGates.isEmpty()) {
            return new QualityGateResult();
        }

        var result = new QualityGateResult();
        for (QualityGate qualityGate : qualityGates) {
            var baseline = qualityGate.getBaseline();
            var possibleValue = report.getValue(baseline, qualityGate.getMetric());
            if (possibleValue.isPresent()) {
                var actualValue = possibleValue.get();

                var status = actualValue.isBelowThreshold(qualityGate.getThreshold()) ? qualityGate.getStatus() : QualityGateStatus.PASSED;
                result.add(qualityGate, status, FORMATTER.format(actualValue, Locale.ENGLISH));
            }
            else {
                result.add(qualityGate, qualityGate.getStatus(), "n/a");
            }
        }
        return result;
    }

    /**
     * Appends the specified quality gates to the end of the list of quality gates.
     *
     * @param size
     *         the minimum coverage that is needed for the quality gate
     * @param metric
     *         the metric to compare
     * @param baseline
     *         the baseline to use the bto compare
     * @param result
     *         determines whether the quality gate is a warning or failure
     */
    public void add(final double size, final Metric metric, final Baseline baseline, final QualityGateCriticality result) {
        var qualityGate = new QualityGate(size, metric, baseline, result);
        qualityGates.add(qualityGate);
    }

    /**
     * Appends all the quality gates in the specified collection to the end of the list of quality gates.
     *
     * @param additionalQualityGates
     *         the quality gates to add
     */
    public void addAll(final Collection<? extends QualityGate> additionalQualityGates) {
        this.qualityGates.addAll(additionalQualityGates);
    }

    /**
     * Returns whether at least one quality gate has been added.
     *
     * @return {@code true} if at least one quality gate has been added, {@code false} otherwise
     */
    public boolean isEnabled() {
        return !qualityGates.isEmpty();
    }
}
