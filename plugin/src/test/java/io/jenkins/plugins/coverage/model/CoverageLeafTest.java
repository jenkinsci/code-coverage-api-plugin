package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;

import static io.jenkins.plugins.coverage.metrics.Assertions.*;

/**
 * Tests the class {@link CoverageLeaf}.
 *
 * @author Ullrich Hafner
 */
class CoverageLeafTest extends AbstractCoverageTest {
    private static final Coverage COVERED = new Coverage.CoverageBuilder().setCovered(1).setMissed(0).build();

    @Test
    void shouldCreateLeaf() {
        CoverageLeaf coverageLeaf = new CoverageLeaf(LINE, COVERED);

        assertThat(coverageLeaf).hasMetric(LINE).hasToString("[Line]: 100.00% (1/1)");
        assertThat(coverageLeaf.getCoverage(LINE)).isEqualTo(COVERED);
        assertThat(coverageLeaf.getCoverage(CoverageMetric.MODULE)).isEqualTo(CoverageBuilder.NO_COVERAGE);
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(CoverageLeaf.class).verify();
    }
}
