package io.jenkins.plugins.coverage.metrics.visualization.dashboard;

import java.util.Locale;
import java.util.Optional;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;
import io.jenkins.plugins.coverage.metrics.CoveragePercentage;
import io.jenkins.plugins.coverage.metrics.Messages;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageLevel;

/**
 * Concrete implementation of {@link CoverageColumnType} which represents the change coverage.
 *
 * @author Florian Orendi
 */
public class ChangeCoverage extends CoverageColumnType {
    /**
     * Creates a column type to be used for representing the change coverage.
     */
    public ChangeCoverage() {
        super(Messages._Change_Coverage_Type());
    }

    @Override
    public Optional<CoveragePercentage> getCoverage(final CoverageBuildAction action, final Metric metric) {
        if (action.hasChangeCoverage(metric)) {
            Value changeCoverage = action.getChangeCoverage(metric);
            if (changeCoverage instanceof Coverage) {
                return Optional.of(CoveragePercentage.valueOf(((Coverage) changeCoverage).getCoveredPercentage()));
            }
        }
        return Optional.empty();
    }

    @Override
    public DisplayColors getDisplayColors(final CoveragePercentage coverage) {
        return CoverageLevel.getDisplayColorsOfCoverageLevel(coverage.getDoubleValue(), getColorProvider());
    }

    @Override
    public String formatCoverage(final CoveragePercentage coverage, final Locale locale) {
        return coverage.formatPercentage(locale);
    }

    @Override
    public String getAnchor() {
        return "#changeCoverage";
    }
}