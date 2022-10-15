package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Locale;
import java.util.Optional;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoveragePercentage;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;

/**
 * Concrete implementation of {@link CoverageColumnType} which represents the project coverage.
 *
 * @author Florian Orendi
 */
public class ProjectCoverage extends CoverageColumnType {
    /**
     * Creates a column type to be used for representing the project coverage.
     */
    public ProjectCoverage() {
        super(Messages._Project_Coverage_Type());
    }

    @Override
    public Optional<CoveragePercentage> getCoverage(final CoverageBuildAction action, final Metric metric) {
        if (action.hasCoverage(metric)) {
            Value coverage = action.getCoverage(metric);
            if (coverage instanceof Coverage) {
                return Optional.of(CoveragePercentage.valueOf(((Coverage)coverage).getCoveredPercentage()));
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
}
