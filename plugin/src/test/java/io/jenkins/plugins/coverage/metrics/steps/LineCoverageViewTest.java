package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;
import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;

import static org.assertj.core.api.Assertions.*;

/**
 * Test to assert that all modified lines and their respective coverage types are correctly extracted from the
 * coverage tree created by the createCoverageTree() method, using the getFilesWithModifiedLines() method.
 */
class LineCoverageViewTest extends AbstractModifiedFilesCoverageTest {

    @Test
    void verifyCoverageForAllModifiedLines() {
        var node = createCoverageTree();
        var filesWithChangedLines = LineCoverageViewModel.getFilesWithModifiedLines(node);

        var fileOneLinesList = createListOfModifiedLines(LineCoverageType.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.PARTRIALLY_COVERED, 20, 20));

        var fileOne = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);

        assertThat(filesWithChangedLines).contains(fileOne);
    }

    private static List<ModifiedLinesBlock> createListOfModifiedLines(LineCoverageType type, Integer... lines) {
        List<ModifiedLinesBlock> modifiedLinesBlocks = new ArrayList<>();

        for (int i = 0; i < (lines.length - 1); i++) {
            modifiedLinesBlocks.add(new ModifiedLinesBlock(lines[i], lines[i + 1], type));
            i++;
        }

        return modifiedLinesBlocks;
    }
}
