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
 * Concrete implementation of {@link CoverageColumnType} which represents the project coverage delta.
 *
 * @author Florian Orendi
 */
public class ProjectCoverageDelta extends CoverageColumnType {

    /**
     * Creates a column type to be used for representing the project coverage delta.
     */
    public ProjectCoverageDelta() {
        super(Messages._Project_Coverage_Delta_Type());
    }

    @Override
    public Optional<CoveragePercentage> getCoverage(final CoverageBuildAction action, final CoverageMetric metric) {
        if (action.hasDelta(metric)) {
            return Optional.of(action.getDifference().get(metric));
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
}
