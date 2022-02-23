package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.testutil.CoverageStubs;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageColumnType}.
 *
 * @author Florian Orendi
 */
class CoverageColumnTypeTest {

    protected static final String PROJECT_COVERAGE_NAME = Messages.Project_Coverage_Type();
    protected static final String PROJECT_COVERAGE_DELTA_NAME = Messages.Project_Coverage_Delta_Type();

    protected static final ProjectCoverage PROJECT_COVERAGE = new ProjectCoverage();
    protected static final ProjectCoverageDelta PROJECT_COVERAGE_DELTA = new ProjectCoverageDelta();

    protected static final Fraction COVERAGE = Fraction.getFraction(50, 1);
    protected static final Fraction COVERAGE_DELTA = Fraction.getFraction(50, 1);
    protected static final CoverageMetric COVERAGE_METRIC = CoverageMetric.BRANCH;

    protected static final Locale LOCALE = Locale.GERMAN;

    @Test
    void shouldGetDisplayName() {
        assertThat(PROJECT_COVERAGE.getDisplayName()).isEqualTo(PROJECT_COVERAGE_NAME);
        assertThat(PROJECT_COVERAGE_DELTA.getDisplayName()).isEqualTo(PROJECT_COVERAGE_DELTA_NAME);
    }

    @Test
    void shouldGetAvailableCoverageTypeNames() {
        List<String> availableCoverageTypes = CoverageColumnType.getAvailableCoverageTypeNames();
        assertThat(availableCoverageTypes).containsExactlyInAnyOrder(PROJECT_COVERAGE_NAME, PROJECT_COVERAGE_DELTA_NAME);
    }

    protected CoverageBuildAction createCoverageBuildAction() {
        return CoverageStubs.createCoverageBuildAction(COVERAGE_METRIC, COVERAGE_DELTA, COVERAGE);
    }
}
