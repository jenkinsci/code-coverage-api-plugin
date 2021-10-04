package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageMetric}.
 *
 * @author Ullrich Hafner
 */
class CoverageMetricTest {
    @Test
    void shouldSortMetrics() {
        List<CoverageMetric> all = new ArrayList<>();
        all.add(CoverageMetric.MODULE);
        all.add(CoverageMetric.PACKAGE);
        all.add(CoverageMetric.FILE);
        all.add(CoverageMetric.CLASS);
        all.add(CoverageMetric.LINE);
        all.add(CoverageMetric.BRANCH);
        all.add(CoverageMetric.INSTRUCTION);

        Collections.sort(all);
        verifyOrder(all);

        Collections.reverse(all);
        assertThat(all).containsExactly(
                CoverageMetric.BRANCH,
                CoverageMetric.INSTRUCTION,
                CoverageMetric.LINE,
                CoverageMetric.CLASS,
                CoverageMetric.FILE,
                CoverageMetric.PACKAGE,
                CoverageMetric.MODULE);

        Collections.sort(all);
        verifyOrder(all);
    }

    private void verifyOrder(final List<CoverageMetric> all) {
        assertThat(all).containsExactly(
                CoverageMetric.MODULE,
                CoverageMetric.PACKAGE,
                CoverageMetric.FILE,
                CoverageMetric.CLASS,
                CoverageMetric.LINE,
                CoverageMetric.INSTRUCTION,
                CoverageMetric.BRANCH
        );
    }
}
