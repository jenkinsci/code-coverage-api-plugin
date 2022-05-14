package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoveragePercentage;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.testutil.CoverageStubs;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProviderFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageColumnType}.
 *
 * @author Florian Orendi
 */
class CoverageColumnTypeTest {

    protected static final String PROJECT_COVERAGE_NAME = Messages.Project_Coverage_Type();
    protected static final String PROJECT_COVERAGE_DELTA_NAME = Messages.Project_Coverage_Delta_Type();
    protected static final String CHANGE_COVERAGE_NAME = Messages.Change_Coverage_Type();
    protected static final String CHANGE_COVERAGE_DELTA_NAME = Messages.Change_Coverage_Delta_Type();
    protected static final String INDIRECT_COVERAGE_CHANGES_NAME = Messages.Indirect_Coverage_Changes_Type();

    protected static final ProjectCoverage PROJECT_COVERAGE = new ProjectCoverage();
    protected static final ProjectCoverageDelta PROJECT_COVERAGE_DELTA = new ProjectCoverageDelta();
    protected static final ChangeCoverage CHANGE_COVERAGE = new ChangeCoverage();
    protected static final ChangeCoverageDelta CHANGE_COVERAGE_DELTA = new ChangeCoverageDelta();
    protected static final IndirectCoverageChanges INDIRECT_COVERAGE_CHANGES = new IndirectCoverageChanges();

    protected static final Fraction COVERAGE_FRACTION = Fraction.getFraction(0.5);
    protected static final CoveragePercentage COVERAGE_PERCENTAGE =
            CoveragePercentage.valueOf(COVERAGE_FRACTION);
    protected static final CoverageMetric COVERAGE_METRIC = CoverageMetric.BRANCH;

    protected static final Locale LOCALE = Locale.GERMAN;
    protected static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createColorProvider();

    @Test
    void shouldGetDisplayName() {
        assertThat(PROJECT_COVERAGE.getDisplayName()).isEqualTo(PROJECT_COVERAGE_NAME);
        assertThat(PROJECT_COVERAGE_DELTA.getDisplayName()).isEqualTo(PROJECT_COVERAGE_DELTA_NAME);
        assertThat(CHANGE_COVERAGE.getDisplayName()).isEqualTo(CHANGE_COVERAGE_NAME);
        assertThat(CHANGE_COVERAGE_DELTA.getDisplayName()).isEqualTo(CHANGE_COVERAGE_DELTA_NAME);
        assertThat(INDIRECT_COVERAGE_CHANGES.getDisplayName()).isEqualTo(INDIRECT_COVERAGE_CHANGES_NAME);
    }

    @Test
    void shouldGetAvailableCoverageTypeNames() {
        List<String> availableCoverageTypes = CoverageColumnType.getAvailableCoverageTypeNames();
        assertThat(availableCoverageTypes).containsExactlyInAnyOrder(
                PROJECT_COVERAGE_NAME, PROJECT_COVERAGE_DELTA_NAME,
                CHANGE_COVERAGE_NAME, CHANGE_COVERAGE_DELTA_NAME, INDIRECT_COVERAGE_CHANGES_NAME);
    }

    protected CoverageBuildAction createCoverageBuildAction() {
        return CoverageStubs.createCoverageBuildAction(COVERAGE_METRIC, COVERAGE_FRACTION);
    }
}
