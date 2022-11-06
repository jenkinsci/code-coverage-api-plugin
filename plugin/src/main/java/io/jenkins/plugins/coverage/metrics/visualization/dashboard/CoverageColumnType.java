package io.jenkins.plugins.coverage.metrics.visualization.dashboard;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import edu.hm.hafner.metric.Metric;

import org.jvnet.localizer.Localizable;

import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;
import io.jenkins.plugins.coverage.metrics.CoveragePercentage;
import io.jenkins.plugins.coverage.metrics.Messages;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProviderFactory;

/**
 * Provides functions for different types of coverage that can be represented within a {@link CoverageColumn}.
 *
 * @author Florian Orendi
 */
public abstract class CoverageColumnType {
    private final Localizable displayName;

    /**
     * Constructor.
     *
     * @param displayName
     *         The name of the coverage type
     */
    public CoverageColumnType(final Localizable displayName) {
        this.displayName = displayName;
    }

    /**
     * Provides a {@link ColorProvider color provider} which is used to determine fill and line colors.
     *
     * @return the color provider
     */
    protected ColorProvider getColorProvider() {
        return ColorProviderFactory.createDefaultColorProvider();
    }

    /**
     * Gets the coverage of the passed metric from the passed action.
     *
     * @param action
     *         The {@link CoverageBuildAction action} which contains the coverage
     * @param metric
     *         The {@link Metric coverage metric}
     *
     * @return the coverage as optional or an empty optional if no coverage has been found
     */
    public abstract Optional<CoveragePercentage> getCoverage(CoverageBuildAction action, Metric metric);

    /**
     * Gets the {@link DisplayColors display colors} which are used for visualizing the passed coverage.
     *
     * @param coverage
     *         The coverage percentage
     *
     * @return the display colors
     */
    public abstract DisplayColors getDisplayColors(CoveragePercentage coverage);

    /**
     * Formats the passed coverage using the passed {@link Locale}.
     *
     * @param coverage
     *         The coverage to be formatted
     * @param locale
     *         The locale
     *
     * @return the formatted coverage string
     */
    public abstract String formatCoverage(CoveragePercentage coverage, Locale locale);

    /**
     * Returns the anchor which stands for a specific part of the coverage report which belongs to this coverage
     * column. The default value is '#overview'.
     *
     * @return the anchor
     */
    public String getAnchor() {
        return "#overview";
    }

    /**
     * Gets the names of the available coverage types.
     *
     * @return the display names
     */
    // FIXME: these texts should not be used as IDs
    public static List<String> getAvailableCoverageTypeNames() {
        return Arrays.asList(
                Messages.Project_Coverage_Type(),
                Messages.Project_Coverage_Delta_Type(),
                Messages.Change_Coverage_Type(),
                Messages.Change_Coverage_Delta_Type(),
                Messages.Indirect_Coverage_Changes_Type()
        );
    }

    public String getDisplayName() {
        return displayName.toString();
    }
}
