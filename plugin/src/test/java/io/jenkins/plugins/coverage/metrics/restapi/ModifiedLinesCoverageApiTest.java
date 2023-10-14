package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.util.LineRange;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests {@link ModifiedLinesCoverageApi}.
 */

class ModifiedLinesCoverageApiTest extends AbstractModifiedFilesCoverageTest {

    /**
     * Test to assert that all modified lines and their respective coverage types are correctly extracted from the
     * coverage tree created by the {@link #createCoverageTree()} method.
     */
    @Test
    void shouldCalculateCoverageForAllModifiedLines() {
        var node = createCoverageTree();
        var modifiedLineCoverageApi = new ModifiedLinesCoverageApi(node);
        var filesWithChangedLines = modifiedLineCoverageApi.getFilesWithModifiedLines();

        var expectedLineBlocks = createListOfModifiedLines(LineCoverageType.COVERED,
                new LineRange(15, 16), new LineRange(21, 22));
        expectedLineBlocks.addAll(createListOfModifiedLines(LineCoverageType.MISSED, new LineRange(35, 36)));
        expectedLineBlocks.addAll(createListOfModifiedLines(LineCoverageType.PARTIALLY_COVERED, new LineRange(20, 20)));
        var expectedFileWithChangedLines = new FileWithModifiedLines("test/example/Test1.java", expectedLineBlocks);

        assertThat(filesWithChangedLines).containsExactly(expectedFileWithChangedLines);

    }

    @Test
    void shouldIncludeLinesWithoutCoverage() {
        var fileNode = new FileNode("Test.java", "path");
        fileNode.addModifiedLines(1, 2, 3, 4);
        fileNode.addCounters(1, 1, 0);
        fileNode.addCounters(4, 1, 0);
        var parentNode = new PackageNode("package");
        parentNode.addChild(fileNode);
        var expectedBlocks = createListOfModifiedLines(LineCoverageType.COVERED, new LineRange(1, 4));

        var filesWithModifiedLines = new ModifiedLinesCoverageApi(parentNode).getFilesWithModifiedLines();

        assertThat(filesWithModifiedLines.get(0).getModifiedLinesBlocks()).containsOnlyOnceElementsOf(expectedBlocks);
    }

    @Test
    void shouldIgnoreNotModifiedLines() {
        var fileNode = new FileNode("Test.java", "path");
        fileNode.addModifiedLines(1);
        fileNode.addCounters(1, 1, 0);
        fileNode.addCounters(2, 1, 0);
        var parentNode = new PackageNode("package");
        parentNode.addChild(fileNode);
        var expectedBlocks = createListOfModifiedLines(LineCoverageType.COVERED, new LineRange(1));

        var filesWithModifiedLines = new ModifiedLinesCoverageApi(parentNode).getFilesWithModifiedLines();

        assertThat(filesWithModifiedLines.get(0).getModifiedLinesBlocks()).containsOnlyOnceElementsOf(expectedBlocks);
    }

    /**
     * Creates a list of {@link ModifiedLinesBlock} objects for testing purposes.
     *
     * @param type
     *         of line coverage: {@link LineCoverageType#COVERED}, {@link LineCoverageType#MISSED}, or
     *         {@link LineCoverageType#PARTIALLY_COVERED}
     * @param ranges
     *         the {@link LineRange lines ranges} to be transformed to {@link ModifiedLinesBlock} elements with the
     *         given coverage type
     *
     * @return the list {@link ModifiedLinesBlock} objects, sharing a {@link LineCoverageType}.
     */
    private SortedSet<ModifiedLinesBlock> createListOfModifiedLines(final LineCoverageType type,
            final LineRange... ranges) {
        var modifiedLinesBlocks = new TreeSet<ModifiedLinesBlock>();
        for (LineRange range : ranges) {
            var block = new ModifiedLinesBlock(range.getStart(), range.getEnd(), type);
            modifiedLinesBlocks.add(block);
        }
        return modifiedLinesBlocks;
    }

}
