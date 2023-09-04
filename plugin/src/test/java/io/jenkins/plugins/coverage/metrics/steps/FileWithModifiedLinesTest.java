package io.jenkins.plugins.coverage.metrics.steps;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;
import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the {@link FileWithModifiedLines} class.
 */
class FileWithModifiedLinesTest extends AbstractModifiedFilesCoverageTest {

    /**
     * Test to ensure the overridden {@link FileWithModifiedLines#equals(Object)} works as expected.
     */
    @Test
    void testOverriddenEqualsMethod() {
        var fileOneLinesList = createListOfModifiedLines(LineCoverageType.COVERED, 15, 16, 21, 22);
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.MISSED, 35, 36));
        fileOneLinesList.addAll(createListOfModifiedLines(LineCoverageType.PARTIALLY_COVERED, 20, 20));

        var fileOne = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);
        var fileTwo = new FileWithModifiedLines("fileTwo", null);
        var fileThree = new FileWithModifiedLines("test/example/Test1.java", fileOneLinesList);

        assertThat(fileOne).isEqualTo(fileOne);
        assertThat(fileOne).isNotEqualTo(fileTwo);
        assertThat(fileOne).isEqualTo(fileThree);
    }
}
