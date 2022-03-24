package io.jenkins.plugins.coverage.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.delta.model.FileEditType;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link CodeDeltaCalculator}.
 *
 * @author Florian Orendi
 */
class CodeDeltaCalculatorTest {

    private static final String SOURCE_DIRECTORY_ADD = "test1";
    private static final String SOURCE_DIRECTORY_UNUSED = "test";

    private static final String FILE_NAME_ADD = "File1.java";
    private static final String FILE_NAME_MODIFY = "File2.java";
    private static final String FILE_NAME_DELETE = "File3.java";

    private static final String FILE_PATH_ADD = SOURCE_DIRECTORY_ADD + '\\' + FILE_NAME_ADD;
    private static final String FILE_PATH_MODIFY = FILE_NAME_MODIFY;
    private static final String FILE_PATH_DELETE = SOURCE_DIRECTORY_UNUSED + '\\' + FILE_NAME_DELETE;

    @Test
    void shouldGetCoverageRelevantChanges() {
        CodeDeltaCalculator codeDeltaCalculator = createCodeDeltaCalculator();
        Delta delta = createDeltaWithMockedFileChanges();
        Map<String, FileChanges> allChanges = delta.getFileChangesMap();

        assertThat(codeDeltaCalculator.getChangeCoverageRelevantChanges(delta))
                .containsExactly(
                        new SimpleEntry<>(FILE_NAME_ADD, allChanges.get(FILE_PATH_ADD)),
                        new SimpleEntry<>(FILE_NAME_MODIFY, allChanges.get(FILE_PATH_MODIFY))
                );
    }

    /**
     * Creates an instance of {@link CodeDeltaCalculator}.
     *
     * @return the created instance
     */
    private CodeDeltaCalculator createCodeDeltaCalculator() {
        Set<String> sourceDirectories = new HashSet<>();
        sourceDirectories.add(SOURCE_DIRECTORY_ADD);
        sourceDirectories.add(SOURCE_DIRECTORY_UNUSED);
        return new CodeDeltaCalculator(mock(Run.class), mock(FilePath.class),
                mock(TaskListener.class), "", sourceDirectories);
    }

    /**
     * Creates a mock of {@link Delta}.
     *
     * @return the created mock
     */
    private Delta createDeltaWithMockedFileChanges() {
        Delta delta = mock(Delta.class);
        Map<String, FileChanges> fileChanges = new HashMap<>();
        FileChanges fileChangesAdd = createFileChanges(FILE_PATH_ADD, FileEditType.ADD);
        FileChanges fileChangesModify = createFileChanges(FILE_PATH_MODIFY, FileEditType.MODIFY);
        FileChanges fileChangesDelete = createFileChanges(FILE_PATH_DELETE, FileEditType.DELETE);
        fileChanges.put(fileChangesAdd.getFileName(), fileChangesAdd);
        fileChanges.put(fileChangesModify.getFileName(), fileChangesModify);
        fileChanges.put(fileChangesDelete.getFileName(), fileChangesDelete);
        when(delta.getFileChangesMap()).thenReturn(fileChanges);
        return delta;
    }

    /**
     * Creates a mock of {@link FileChanges}.
     *
     * @param filePath
     *         The file path
     * @param fileEditType
     *         The {@link FileEditType edit type}
     *
     * @return the created mock
     */
    private FileChanges createFileChanges(final String filePath, final FileEditType fileEditType) {
        FileChanges change = mock(FileChanges.class);
        when(change.getFileEditType()).thenReturn(fileEditType);
        when(change.getFileName()).thenReturn(filePath);
        return change;
    }
}
