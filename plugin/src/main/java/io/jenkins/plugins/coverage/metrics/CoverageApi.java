package io.jenkins.plugins.coverage.metrics;

import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

import edu.hm.hafner.metric.Metric;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Remote API to list the details of the coverage results.
 *
 * @author Ullrich Hafner
 */
@ExportedBean
public class CoverageApi {
    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final CoverageStatistics statistics;

    CoverageApi(final CoverageStatistics statistics) {
        this.statistics = statistics;
    }

    /**
     * Returns the statistics for the project coverage.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getProjectStatistics() {
        var values = new TreeMap<String, String>();

        for (Metric metric : Metric.values()) {
            statistics.getValue(Baseline.PROJECT, metric).ifPresent(value -> values.put(metric.name(), FORMATTER.format(value, Locale.ENGLISH)));
        }

        return values;
    }
}
