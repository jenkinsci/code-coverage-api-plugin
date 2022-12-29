package io.jenkins.plugins.coverage.metrics;

import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.FractionValue;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

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

    CoverageStatistics(
            final List<? extends Value> projectValueMapping,
            final NavigableMap<Metric, Fraction> projectDelta,
            final List<? extends Value> changeValueMapping,
            final NavigableMap<Metric, Fraction> changeDelta,
            final List<? extends Value> fileValueMapping,
            final NavigableMap<Metric, Fraction> fileDelta)  {
        this.projectValueMapping = List.copyOf(projectValueMapping);
        this.changeValueMapping = List.copyOf(changeValueMapping);
        this.fileValueMapping = List.copyOf(fileValueMapping);

        this.projectDelta = asValueMap(projectDelta);
        this.changeDelta = asValueMap(changeDelta);
        this.fileDelta = asValueMap(fileDelta);
    }

    private static TreeMap<Metric, Value> asValueMap(final NavigableMap<Metric, Fraction> projectDelta) {
        return projectDelta.entrySet().stream().collect(
                Collectors.toMap(Entry::getKey, e -> new FractionValue(e.getKey(), e.getValue()), (o1, o2) -> o1,
                        TreeMap::new));
    }

    private static <K, V> NavigableMap<K, V> copy(final NavigableMap<K, V> original) {
        return new TreeMap<>(original);
    }

    public Optional<? extends Value> getValue(final Baseline baseline, final Metric metric) {
        if (baseline == Baseline.PROJECT) {
            return Value.findValue(metric, projectValueMapping);
        }
        if (baseline == Baseline.FILE) {
            return Value.findValue(metric, fileValueMapping);
        }
        if (baseline == Baseline.CHANGE) {
            return Value.findValue(metric, changeValueMapping);
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
