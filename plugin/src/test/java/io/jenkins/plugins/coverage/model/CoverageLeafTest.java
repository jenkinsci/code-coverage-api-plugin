package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageLeaf}.
 *
 * @author Ullrich Hafner
 */
class CoverageLeafTest extends AbstractCoverageTest {
    private static final Coverage COVERED = new Coverage(1, 0);

    @Test
    void shouldCreateLeaf() {
        CoverageLeaf coverageLeaf = new CoverageLeaf(LINE, COVERED);

        assertThat(coverageLeaf).hasMetric(LINE).hasToString("[Line]: 100.00% (1/1)");
        assertThat(coverageLeaf.getCoverage(LINE)).isEqualTo(COVERED);
        assertThat(coverageLeaf.getCoverage(CoverageMetric.MODULE)).isEqualTo(Coverage.NO_COVERAGE);
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(CoverageLeaf.class).verify();
    }
}
