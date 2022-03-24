package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.util.FractionFormatter;
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
    public Optional<Fraction> getCoverage(final CoverageBuildAction action, final CoverageMetric metric) {
        if (action.hasChangeCoverageDifference(metric)) {
            return Optional.of(action.getChangeCoverageDifference(metric));
        }
        return Optional.empty();
    }

    @Override
    public DisplayColors getDisplayColors(final Fraction coverage) {
        return CoverageChangeTendency.getDisplayColorsForTendency(coverage.doubleValue(), getColorProvider());
    }

    @Override
    public String formatCoverage(final Fraction coverage, final Locale locale) {
        return FractionFormatter.formatDeltaPercentage(coverage, locale);
    }
}
