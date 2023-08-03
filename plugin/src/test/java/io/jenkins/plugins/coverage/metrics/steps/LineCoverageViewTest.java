package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;
import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.Is.*;

/**
 * TODO: JavaDoc me
 */
class LineCoverageViewTest extends AbstractModifiedFilesCoverageTest {

    @Test
    void shouldContainAllChangedAndCoveredLines() {
        var node = createCoverageTree();
        node.filterByModifiedLines();
        var filesWithChangedLines = LineCoverageViewModel.getFilesWithModifiedLines(node);

        assertThat(filesWithChangedLines.isEmpty(), is(false));
        assertThat(filesWithChangedLines.get(0).getRelativePath(), is("test/example/Test1.java"));

        for (FileWithModifiedLines filesWithChangedLine : filesWithChangedLines) {
            assertThat(filesWithChangedLine.getListOfModifiedLines().isEmpty(), is(false));
        }

        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().size(), is(4));

        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(0).getStartLine(), is(15));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(0).getEndLine(), is(16));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(0).getType(), is(LineCoverageType.COVERED));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(1).getStartLine(), is(21));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(1).getEndLine(), is(22));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(1).getType(), is(LineCoverageType.COVERED));

        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(2).getStartLine(), is(35));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(2).getEndLine(), is(36));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(2).getType(), is(LineCoverageType.MISSED));

        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(3).getStartLine(), is(20));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(3).getEndLine(), is(20));
        assertThat(filesWithChangedLines.get(0).getListOfModifiedLines().get(3).getType(), is(LineCoverageType.PARTRIALLY_COVERED));


        var fileOneLinesList = createListOfChangedLines(LineCoverageType.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfChangedLines(LineCoverageType.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfChangedLines(LineCoverageType.PARTRIALLY_COVERED, 20, 20));

        var fileOne = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);

        assertThat(filesWithChangedLines, IsCollectionContaining.hasItem(fileOne));


    }

    private static List<ModifiedLinesBlock> createListOfChangedLines(LineCoverageType type, Integer... lines) {
        List<ModifiedLinesBlock> modifiedLinesBlocks = new ArrayList<>();

        for (int i = 0; i < (lines.length - 1); i++) {
            modifiedLinesBlocks.add(new ModifiedLinesBlock(lines[i], lines[i + 1], type));
            i++;
        }

        return modifiedLinesBlocks;
    }
}
