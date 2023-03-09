package io.jenkins.plugins.coverage.metrics.model;

import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.FractionValue;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;

/**
 * Represents the different mappings of coverage metric and baseline to actual values.
 */
public class CoverageStatistics {
    private final List<Value> projectValueMapping;
    private final NavigableMap<Metric, Value> projectDelta;
    private final List<Value> changeValueMapping;
    private final NavigableMap<Metric, Value> changeDelta;
    private final List<Value> fileValueMapping;
    private final NavigableMap<Metric, Value> fileDelta;

    /**
     * Creates a new instance of {@link CoverageStatistics}.
     *
     * @param projectValueMapping
     *         mapping of metrics to values for {@link Baseline#PROJECT}
     * @param projectDeltaMapping
     *         mapping of metrics to delta values for {@link Baseline#PROJECT_DELTA}
     * @param modifiedLinesValueMapping
     *         mapping of metrics to values for {@link Baseline#MODIFIED_LINES}
     * @param modifiedLinesDeltaMapping
     *         mapping of metrics to delta values for {@link Baseline#MODIFIED_LINES_DELTA}
     * @param modifiedFilesValueMapping
     *         mapping of metrics to values for {@link Baseline#MODIFIED_FILES}
     * @param modifiedFilesDeltaMapping
     *         mapping of metrics to delta values for {@link Baseline#MODIFIED_FILES_DELTA}
     */
    public CoverageStatistics(
            final List<? extends Value> projectValueMapping,
            final NavigableMap<Metric, Fraction> projectDeltaMapping,
            final List<? extends Value> modifiedLinesValueMapping,
            final NavigableMap<Metric, Fraction> modifiedLinesDeltaMapping,
            final List<? extends Value> modifiedFilesValueMapping,
            final NavigableMap<Metric, Fraction> modifiedFilesDeltaMapping) {
        this.projectValueMapping = List.copyOf(projectValueMapping);
        this.changeValueMapping = List.copyOf(modifiedLinesValueMapping);
        this.fileValueMapping = List.copyOf(modifiedFilesValueMapping);

        this.projectDelta = asValueMap(projectDeltaMapping);
        this.changeDelta = asValueMap(modifiedLinesDeltaMapping);
        this.fileDelta = asValueMap(modifiedFilesDeltaMapping);
    }

    private static NavigableMap<Metric, Value> asValueMap(final NavigableMap<Metric, Fraction> projectDelta) {
        return projectDelta.entrySet().stream().collect(
                Collectors.toMap(Entry::getKey, e -> new FractionValue(e.getKey(), e.getValue()), (o1, o2) -> o1,
                        TreeMap::new));
    }

    /**
     * Returns the value for the specified baseline and metric.
     *
     * @param baseline
     *         the baseline of the value
     * @param metric
     *         the metric of the value
     *
     * @return the value, if available
     */
    public Optional<Value> getValue(final Baseline baseline, final Metric metric) {
        if (baseline == Baseline.PROJECT) {
            return Value.findValue(metric, projectValueMapping);
        }
        if (baseline == Baseline.MODIFIED_FILES) {
            return Value.findValue(metric, fileValueMapping);
        }
        if (baseline == Baseline.MODIFIED_LINES) {
            return Value.findValue(metric, changeValueMapping);
        }
        if (baseline == Baseline.PROJECT_DELTA) {
            return getValue(metric, projectDelta);
        }
        if (baseline == Baseline.MODIFIED_LINES_DELTA) {
            return getValue(metric, changeDelta);
        }
        if (baseline == Baseline.MODIFIED_FILES_DELTA) {
            return getValue(metric, fileDelta);
        }

        throw new NoSuchElementException("No such baseline: " + baseline);
    }

    private Optional<Value> getValue(final Metric metric, final NavigableMap<Metric, Value> mapping) {
        return Optional.ofNullable(mapping.get(metric));
    }

    /**
     * Returns whether a value for the specified metric and baseline is available.
     *
     * @param baseline
     *         the baseline of the value
     * @param metric
     *         the metric of the value
     *
     * @return {@code true}, if a value is available, {@code false} otherwise
     */
    public boolean containsValue(final Baseline baseline, final Metric metric) {
        return getValue(baseline, metric).isPresent();
    }
}
