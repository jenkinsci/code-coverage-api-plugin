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
 * Test to assert that all modified lines and their respective coverage types are correctly extracted from the coverage
 * tree created by the createCoverageTree() method, using the calculateFilesWithModifiedLines() method in {@link LineCoverageViewModel}.
 */
class LineCoverageViewTest extends AbstractModifiedFilesCoverageTest {

    @Test
    void testOverriddenEqualsMethod() {
        var fileOneLinesList = createListOfModifiedLines(LineCoverageType.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.PARTRIALLY_COVERED, 20, 20));

        var fileOne = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);
        var fileTwo = new FileWithModifiedLines("fileTwo", null);
        var checkTrue = fileOne.equals(fileOne);
        var checkFalse = fileOne.equals(fileTwo);

        assertThat(checkTrue).isTrue();
        assertThat(checkFalse).isFalse();
    }

    @Test
    void verifyCoverageForAllModifiedLines() {
        var node = createCoverageTree();
        var filesWithChangedLines = LineCoverageViewModel.getFilesWithModifiedLines(node);

        var fileOneLinesList = createListOfModifiedLines(LineCoverageType.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.PARTRIALLY_COVERED, 20, 20));

        var fileOne = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);
        var checkEqualsMethod = fileOne.equals(fileOne);

        assertThat(filesWithChangedLines).contains(fileOne);
        assertThat(checkEqualsMethod).isTrue();
    }

    private static List<ModifiedLinesBlock> createListOfModifiedLines(final LineCoverageType type,
            final Integer... lines) {
        List<ModifiedLinesBlock> modifiedLinesBlocks = new ArrayList<>();

        for (int i = 0; i < (lines.length - 1); i += 2) {
            modifiedLinesBlocks.add(new ModifiedLinesBlock(lines[i], lines[i + 1], type));
        }

        return modifiedLinesBlocks;
    }
}
