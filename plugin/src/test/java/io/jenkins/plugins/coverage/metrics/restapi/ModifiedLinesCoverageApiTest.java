package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the {@link ModifiedLinesCoverageApi} class.
 */

class ModifiedLinesCoverageApiTest extends AbstractModifiedFilesCoverageTest {
    //TODO: write shouldProvideRemoteApi() test

    /**
     * Test to assert that all modified lines and their respective coverage types are correctly extracted from the
     * coverage tree created by the {@link #createCoverageTree()} method.
     */
    @Test
    void verifyCoverageForAllModifiedLines() {
        var node = createCoverageTree();
        var modifiedLineCoverageApi = new ModifiedLinesCoverageApi(node);
        var filesWithChangedLines = modifiedLineCoverageApi.createListOfFilesWithModifiedLines();

        var fileOneLinesList = createListOfModifiedLines(LineCoverageType.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.PARTIALLY_COVERED, 20, 20));

        var fileOne = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);

        assertThat(filesWithChangedLines).contains(fileOne);

    }

    /**
     * Creates a list of {@link ModifiedLinesBlock} objects for testing purposes.
     *
     * @param type
     *         of line coverage: {@link LineCoverageType#COVERED}, {@link LineCoverageType#MISSED}, or
     *         {@link LineCoverageType#PARTIALLY_COVERED}
     * @param lines
     *         of code that share the above {@link LineCoverageType}, must be consecutive.
     *
     * @return the list {@link ModifiedLinesBlock} objects, sharing a {@link LineCoverageType}.
     */
    private List<ModifiedLinesBlock> createListOfModifiedLines(final LineCoverageType type,
            final Integer... lines) {
        var modifiedLinesBlocks = new ArrayList<ModifiedLinesBlock>();

        for (int i = 0; i < (lines.length - 1); i += 2) {
            modifiedLinesBlocks.add(new ModifiedLinesBlock(lines[i], lines[i + 1], type));
        }

        return modifiedLinesBlocks;
    }

}
