package io.jenkins.plugins.coverage.metrics.steps;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the {@link ModifiedLinesBlock} class.
 */
class ModifiedLinesBlockTest extends AbstractModifiedFilesCoverageTest {
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
    void testOverriddenEqualsMethod() {
        var linesBlockOne = new ModifiedLinesBlock(0, 0, LineCoverageType.COVERED);
        var linesBlockTwo = new ModifiedLinesBlock(0, 0, LineCoverageType.COVERED);
        var linesBlockThree = new ModifiedLinesBlock(0, 0, LineCoverageType.MISSED);

        assertThat(linesBlockOne).isEqualTo(linesBlockTwo);
        assertThat(linesBlockOne).isNotEqualTo(linesBlockThree);
        assertThat(linesBlockOne).isEqualTo(linesBlockOne);

    }
}
