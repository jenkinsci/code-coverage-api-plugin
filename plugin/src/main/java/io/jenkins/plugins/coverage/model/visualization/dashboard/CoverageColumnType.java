package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;

import org.jvnet.localizer.Localizable;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProviderFactory;

/**
 * Provides functions for different types of how coverage can be represented within a {@link CoverageColumn}.
 *
 * @author Florian Orendi
 */
public abstract class CoverageColumnType {

    protected final Localizable displayName;

    public CoverageColumnType(final Localizable displayName) {
        this.displayName = displayName;
    }

    protected ColorProvider getColorProvider() {
        return ColorProviderFactory.createColorProvider();
    }

    public abstract Optional<Fraction> getCoverage(final CoverageBuildAction action, final CoverageMetric metric);

    public abstract DisplayColors getDisplayColors(final Fraction coverage);

    public abstract String formatCoverage(final Fraction coverage);

    public static List<String> getAvailableCoverageTypeNames() {
        return Arrays.asList(
                Messages.Project_Coverage_Type(),
                Messages.Project_Coverage_Delta_Type()
        );
    }

    public String getDisplayName() {
        return displayName.toString();
    }
}
