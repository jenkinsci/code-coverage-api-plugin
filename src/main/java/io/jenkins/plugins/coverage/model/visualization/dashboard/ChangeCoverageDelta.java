package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Locale;
import java.util.Optional;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoveragePercentage;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageChangeTendency;

/**
 * Concrete implementation of {@link CoverageColumnType} which represents the change coverage delta.
 *
 * @author Florian Orendi
 */
public class ChangeCoverageDelta extends CoverageColumnType {

    /**
     * Creates a column type to be used for representing the change coverage delta.
     */
    public ChangeCoverageDelta() {
        super(Messages._Change_Coverage_Delta_Type());
    }

    @Override
    public Optional<CoveragePercentage> getCoverage(final CoverageBuildAction action, final CoverageMetric metric) {
        if (action.hasChangeCoverageDifference(metric)) {
            return Optional.of(action.getChangeCoverageDifference(metric));
        }
        return Optional.empty();
    }

    @Override
    public DisplayColors getDisplayColors(final CoveragePercentage coverage) {
        return CoverageChangeTendency.getDisplayColorsForTendency(coverage.getDoubleValue(), getColorProvider());
    }

    @Override
    public String formatCoverage(final CoveragePercentage coverage, final Locale locale) {
        return coverage.formatDeltaPercentage(locale);
    }

    @Override
    public String getAnchor() {
        return "#changeCoverage";
    }
}
