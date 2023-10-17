package io.jenkins.plugins.coverage.metrics.restapi;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests {@link ModifiedLinesBlock}.
 */
class ModifiedLinesBlockTest {
    @Test
    void testGetters() {
        var linesBlockOne = new ModifiedLinesBlock(0, 0, LineCoverageType.COVERED);
        var linesBlockTwo = new ModifiedLinesBlock(0, 0, LineCoverageType.MISSED);
        var linesBlockThree = new ModifiedLinesBlock(0, 0, LineCoverageType.PARTIALLY_COVERED);

        var startOne = linesBlockOne.getStartLine();
        var endOne = linesBlockOne.getEndLine();
        var typeOne = linesBlockOne.getType();
        var typeTwo = linesBlockTwo.getType();
        var typeThree = linesBlockThree.getType();

        assertThat(startOne).isEqualTo(0);
        assertThat(endOne).isEqualTo(0);
        assertThat(typeOne).isEqualTo(LineCoverageType.COVERED);
        assertThat(typeTwo).isEqualTo(LineCoverageType.MISSED);
        assertThat(typeThree).isEqualTo(LineCoverageType.PARTIALLY_COVERED);
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(ModifiedLinesBlock.class).usingGetClass().verify();
    }
}
