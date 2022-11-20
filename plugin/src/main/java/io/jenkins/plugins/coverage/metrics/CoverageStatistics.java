package io.jenkins.plugins.coverage.metrics;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Delta;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

/**
 * Represents the different mappings of coverage metric and baseline to actual values.
 */
class CoverageStatistics {
    private final NavigableMap<Metric, Value> projectValueMapping;
    private final NavigableMap<Metric, Value> projectDelta;
    private final NavigableMap<Metric, Value> changeValueMapping;
    private final NavigableMap<Metric, Value> changeDelta;
    private final NavigableMap<Metric, Value> fileValueMapping;
    private final NavigableMap<Metric, Value> fileDelta;

    CoverageStatistics(
            final NavigableMap<Metric, Value> projectValueMapping,
            final NavigableMap<Metric, Fraction> projectDelta,
            final NavigableMap<Metric, Value> changeValueMapping,
            final NavigableMap<Metric, Fraction> changeDelta,
            final NavigableMap<Metric, Value> fileValueMapping,
            final NavigableMap<Metric, Fraction> fileDelta)  {
        this.projectValueMapping = copy(projectValueMapping);
        this.changeValueMapping = copy(changeValueMapping);
        this.fileValueMapping = copy(fileValueMapping);

        this.projectDelta = asValueMap(projectDelta);
        this.changeDelta = asValueMap(changeDelta);
        this.fileDelta = asValueMap(fileDelta);
    }

    private static TreeMap<Metric, Value> asValueMap(final NavigableMap<Metric, Fraction> projectDelta) {
        return projectDelta.entrySet().stream().collect(
                Collectors.toMap(Entry::getKey, e -> new Delta(e.getKey(), e.getValue()), (o1, o2) -> o1,
                        TreeMap::new));
    }

    private static <K, V> NavigableMap<K, V> copy(final NavigableMap<K, V> original) {
        return new TreeMap<>(original);
    }

    public Optional<Value> getValue(final Baseline baseline, final Metric metric) {
        if (baseline == Baseline.PROJECT) {
            return getValue(metric, projectValueMapping);
        }
        if (baseline == Baseline.FILE) {
            return getValue(metric, fileValueMapping);
        }
        if (baseline == Baseline.CHANGE) {
            return getValue(metric, changeValueMapping);
        }
        if (baseline == Baseline.PROJECT_DELTA) {
            return getValue(metric, projectDelta);
        }
        if (baseline == Baseline.CHANGE_DELTA) {
            return getValue(metric, changeDelta);
        }
        if (baseline == Baseline.FILE_DELTA) {
            return getValue(metric, fileDelta);
        }

        throw new NoSuchElementException("No such baseline: " + baseline);
    }

    private Optional<Value> getValue(final Metric metric, final NavigableMap<Metric, Value> mapping) {
        return Optional.ofNullable(mapping.get(metric));
    }
}
