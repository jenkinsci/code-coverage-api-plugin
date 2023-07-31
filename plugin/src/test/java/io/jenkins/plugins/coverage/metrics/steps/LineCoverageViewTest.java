package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.ChangedLinesModel;
import io.jenkins.plugins.coverage.metrics.model.FileWithChangedLinesCoverageModel;
import io.jenkins.plugins.coverage.metrics.model.Type;

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
        var filesWithChangedLines = LineCoverageViewModel.getFilesWithChangedLines(node);

        assertThat(filesWithChangedLines.isEmpty(), is(false));
        assertThat(filesWithChangedLines.get(0).getRelativePath(), is("test/example/Test1.java"));

        for (FileWithChangedLinesCoverageModel filesWithChangedLine : filesWithChangedLines) {
            assertThat(filesWithChangedLine.getListOfChangedLines().isEmpty(), is(false));
        }

        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().size(), is(4));

        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(0).getStartLine(), is(15));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(0).getEndLine(), is(16));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(0).getType(), is(Type.COVERED));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(1).getStartLine(), is(21));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(1).getEndLine(), is(22));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(1).getType(), is(Type.COVERED));

        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(2).getStartLine(), is(35));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(2).getEndLine(), is(36));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(2).getType(), is(Type.MISSED));

        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(3).getStartLine(), is(20));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(3).getEndLine(), is(20));
        assertThat(filesWithChangedLines.get(0).getListOfChangedLines().get(3).getType(), is(Type.PARTRIALLY_COVERED));


        var fileOneLinesList = createListOfChangedLines(Type.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfChangedLines(Type.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfChangedLines(Type.PARTRIALLY_COVERED, 20, 20));

        var fileOne = new FileWithChangedLinesCoverageModel("test/example/Test1.java");
        fileOne.setListOfChangedLines(fileOneLinesList);

        assertThat(filesWithChangedLines, IsCollectionContaining.hasItem(fileOne));


    }

    private static List<ChangedLinesModel> createListOfChangedLines(Type type, Integer... lines) {
        List<ChangedLinesModel> changedLinesModels = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            changedLinesModels.add(new ChangedLinesModel(lines[i], lines[i + 1], type));
            i++;
        }

        return changedLinesModels;
    }
}
