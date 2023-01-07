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
        return mapToStrings(Baseline.PROJECT);
    }

    /**
     * Returns the delta values for the project coverage.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getProjectDelta() {
        return mapToStrings(Baseline.PROJECT_DELTA);
    }

    /**
     * Returns the statistics for the coverage of modified files.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedFilesStatistics() {
        return mapToStrings(Baseline.FILE);
    }

    /**
     * Returns the delta values for the modified files coverage.
     *
     * @return a mapping of metrics to their delta values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedFilesDelta() {
        return mapToStrings(Baseline.FILE_DELTA);
    }

    /**
     * Returns the statistics for the coverage of modified lines.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedLinesStatistics() {
        return mapToStrings(Baseline.CHANGE);
    }

    /**
     * Returns the delta values for the modified lines coverage.
     *
     * @return a mapping of metrics to their delta values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedLinesDelta() {
        return mapToStrings(Baseline.CHANGE_DELTA);
    }

    private TreeMap<String, String> mapToStrings(final Baseline baseline) {
        var values = new TreeMap<String, String>();

        for (Metric metric : Metric.values()) {
            statistics.getValue(baseline, metric).ifPresent(value -> values.put(metric.toTagName(), FORMATTER.format(value, Locale.ENGLISH)));
        }

        return values;
    }
}
