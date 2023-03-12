package io.jenkins.plugins.coverage.metrics.steps;

import java.util.Collection;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import edu.hm.hafner.coverage.Metric;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import hudson.model.Result;

import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.util.QualityGateResult;
import io.jenkins.plugins.util.QualityGateResult.QualityGateResultItem;

/**
 * Remote API to list the details of the coverage results.
 *
 * @author Ullrich Hafner
 */
@ExportedBean
public class CoverageApi {
    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final CoverageStatistics statistics;
    private final QualityGateResult qualityGateResult;
    private final String referenceBuild;

    CoverageApi(final CoverageStatistics statistics, final QualityGateResult qualityGateResult,
            final String referenceBuild) {
        this.statistics = statistics;
        this.qualityGateResult = qualityGateResult;
        this.referenceBuild = referenceBuild;
    }

    @Exported(inline = true)
    public QualityGateResultApi getQualityGates() {
        return new QualityGateResultApi(qualityGateResult);
    }

    @Exported
    public String getReferenceBuild() {
        return referenceBuild;
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
        return mapToStrings(Baseline.MODIFIED_FILES);
    }

    /**
     * Returns the delta values for the modified files coverage.
     *
     * @return a mapping of metrics to their delta values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedFilesDelta() {
        return mapToStrings(Baseline.MODIFIED_FILES_DELTA);
    }

    /**
     * Returns the statistics for the coverage of modified lines.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedLinesStatistics() {
        return mapToStrings(Baseline.MODIFIED_LINES);
    }

    /**
     * Returns the delta values for the modified lines coverage.
     *
     * @return a mapping of metrics to their delta values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedLinesDelta() {
        return mapToStrings(Baseline.MODIFIED_LINES_DELTA);
    }

    private NavigableMap<String, String> mapToStrings(final Baseline baseline) {
        var values = new TreeMap<String, String>();

        for (Metric metric : Metric.values()) {
            statistics.getValue(baseline, metric)
                    .ifPresent(value -> values.put(metric.toTagName(), FORMATTER.format(value, Locale.ENGLISH)));
        }

        return values;
    }

    /**
     * Remote API to list the overview of the quality gate evaluation.
     */
    @ExportedBean
    public static class QualityGateResultApi {
        private final QualityGateResult qualityGateResult;

        QualityGateResultApi(final QualityGateResult qualityGateResult) {
            this.qualityGateResult = qualityGateResult;
        }

        @Exported(inline = true)
        public Result getOverallResult() {
            return qualityGateResult.getOverallStatus().getResult();
        }

        @Exported(inline = true)
        public Collection<QualityGateItemApi> getResultItems() {
            return qualityGateResult.getResultItems().stream().map(QualityGateItemApi::new).collect(Collectors.toList());
        }
    }

    /**
     * Remote API to show the content of an individual quality gate item.
     */
    @ExportedBean
    public static class QualityGateItemApi {
        private final QualityGateResultItem item;

        QualityGateItemApi(final QualityGateResultItem item) {
            this.item = item;
        }

        @Exported
        public String getQualityGate() {
            return item.getQualityGate().getName();
        }

        @Exported
        public double getThreshold() {
            return item.getQualityGate().getThreshold();
        }

        @Exported(inline = true)
        public Result getResult() {
            return item.getStatus().getResult();
        }

        @Exported
        public String getValue() {
            return item.getActualValue();
        }
    }
}
