package io.jenkins.plugins.coverage.metrics.steps;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;

import static org.assertj.core.api.Assertions.*;
public class ModifiedLinesBlockTest {
    @Test
    void testGetters() {
        var linesBlockOne = new ModifiedLinesBlock(0,0, LineCoverageType.COVERED);
        var linesBlockTwo = new ModifiedLinesBlock(0,0, LineCoverageType.MISSED);
        var linesBlockThree = new ModifiedLinesBlock(0,0, LineCoverageType.PARTRIALLY_COVERED);

        var startOne = linesBlockOne.getStartLine();
        var endOne = linesBlockOne.getEndLine();
        var typeOne = linesBlockOne.getType();

        var typeTwo = linesBlockTwo.getType();

        var typeThree = linesBlockThree.getType();

        assertThat(startOne).isEqualTo(0);
        assertThat(endOne).isEqualTo(0);
        assertThat(typeOne).isEqualTo(LineCoverageType.COVERED);
        assertThat(typeTwo).isEqualTo(LineCoverageType.MISSED);
        assertThat(typeThree).isEqualTo(LineCoverageType.PARTRIALLY_COVERED);
    }

    @Test
    void testOverriddenEqualsMethod() {
        var linesBlockOne = new ModifiedLinesBlock(0,0, LineCoverageType.COVERED);
        var linesBlockTwo = new ModifiedLinesBlock(0,0, LineCoverageType.COVERED);
        var linesBlockThree = new ModifiedLinesBlock(0,0, LineCoverageType.MISSED);

        var checkTrue = linesBlockOne.equals(linesBlockTwo);
        var checkFalse = linesBlockOne.equals(linesBlockThree);
        var checkSameObject = linesBlockOne.equals(linesBlockOne);

        assertThat(checkTrue).isTrue();
        assertThat(checkFalse).isFalse();
        assertThat(checkSameObject).isTrue();
    }
}
