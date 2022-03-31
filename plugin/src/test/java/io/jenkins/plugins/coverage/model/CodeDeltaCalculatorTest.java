package io.jenkins.plugins.coverage.model;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.delta.model.FileEditType;

import static io.jenkins.plugins.coverage.model.CodeDeltaCalculator.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link CodeDeltaCalculator}.
 *
 * @author Florian Orendi
 */
class CodeDeltaCalculatorTest {

    private static final String LOG_NAME = "Errors while calculating changes mapping:";

    private static final String REPORT_PATH_ADD_1 =
            Paths.get("test", "Test.java").toString();
    private static final String REPORT_PATH_ADD_2 =
            Paths.get("package", "example", "test", "Test.java").toString();
    private static final String REPORT_PATH_MODIFY =
            Paths.get("example", "test", "Test.java").toString();

    private static final String SCM_PATH_ADD_1 =
            Paths.get("test", "Test.java").toString();
    private static final String SCM_PATH_ADD_2 =
            Paths.get("src", "package", "example", "test", "Test.java").toString();
    private static final String SCM_PATH_MODIFY =
            Paths.get("src", "main", "java", "example", "test", "Test.java").toString();
    private static final String SCM_PATH_DELETE =
            Paths.get("src", "main", "example", "test", "Test.java").toString();

    @Test
    void shouldGetCoverageRelevantChanges() {
        CodeDeltaCalculator codeDeltaCalculator = createCodeDeltaCalculator();
        Delta delta = createDeltaWithMockedFileChanges();
        Map<String, FileChanges> allChanges = delta.getFileChangesMap();

        assertThat(codeDeltaCalculator.getChangeCoverageRelevantChanges(delta))
                .containsExactlyInAnyOrder(
                        allChanges.get(SCM_PATH_ADD_1),
                        allChanges.get(SCM_PATH_ADD_2),
                        allChanges.get(SCM_PATH_MODIFY)
                );
    }

    @Test
    void shouldMapScmChangesToReportPaths() throws CoverageException {
        CodeDeltaCalculator codeDeltaCalculator = createCodeDeltaCalculator();
        Delta delta = createDeltaWithMockedFileChanges();
        Set<FileChanges> changes = codeDeltaCalculator.getChangeCoverageRelevantChanges(delta);
        Map<String, FileChanges> changesMap = changes.stream()
                .collect(Collectors.toMap(FileChanges::getFileName, Function.identity()));
        CoverageNode tree = createMockedCoverageTree();
        FilteredLog log = createFilteredLog();

        Map<String, FileChanges> should = new HashMap<>();
        should.put(REPORT_PATH_ADD_1, changesMap.get(SCM_PATH_ADD_1));
        should.put(REPORT_PATH_ADD_2, changesMap.get(SCM_PATH_ADD_2));
        should.put(REPORT_PATH_MODIFY, changesMap.get(SCM_PATH_MODIFY));

        assertThat(codeDeltaCalculator.mapScmChangesToReportPaths(changes, tree, log))
                .containsExactlyInAnyOrderEntriesOf(should);
    }

    @Test
    void shouldCreateEmptyMappingWithoutChanges() throws CoverageException {
        CodeDeltaCalculator codeDeltaCalculator = createCodeDeltaCalculator();
        CoverageNode tree = createMockedCoverageTree();
        FilteredLog log = createFilteredLog();
        Set<FileChanges> noChanges = new HashSet<>();

        assertThat(codeDeltaCalculator.mapScmChangesToReportPaths(noChanges, tree, log)).isEmpty();
    }

    @Test
    void shouldNotMapScmChangesWithAmbiguousPaths() {
        CodeDeltaCalculator codeDeltaCalculator = createCodeDeltaCalculator();
        FilteredLog log = createFilteredLog();

        String path = "example";
        Set<FileChanges> changes = createAmbiguousFileChanges(path);

        CoverageNode tree = mock(CoverageNode.class);
        FileCoverageNode file1 = mock(FileCoverageNode.class);
        when(file1.getPath()).thenReturn(path);
        when(tree.getAllFileCoverageNodes()).thenReturn(Collections.singletonList(file1));

        assertThatThrownBy(() -> codeDeltaCalculator.mapScmChangesToReportPaths(changes, tree, log))
                .isInstanceOf(CoverageException.class)
                .hasMessage(AMBIGUOUS_PATHS_ERROR);
        assertThat(log.getErrorMessages()).containsExactly(LOG_NAME, AMBIGUOUS_PATHS_ERROR);
    }

    /**
     * Creates an instance of {@link CodeDeltaCalculator}.
     *
     * @return the created instance
     */
    private CodeDeltaCalculator createCodeDeltaCalculator() {
        return new CodeDeltaCalculator(mock(Run.class), mock(FilePath.class),
                mock(TaskListener.class), "");
    }

    /**
     * Creates a mock of {@link Delta}.
     *
     * @return the created mock
     */
    private Delta createDeltaWithMockedFileChanges() {
        Delta delta = mock(Delta.class);
        Map<String, FileChanges> fileChanges = new HashMap<>();
        FileChanges fileChangesAdd1 = createFileChanges(SCM_PATH_ADD_1, FileEditType.ADD);
        FileChanges fileChangesAdd2 = createFileChanges(SCM_PATH_ADD_2, FileEditType.ADD);
        FileChanges fileChangesModify = createFileChanges(SCM_PATH_MODIFY, FileEditType.MODIFY);
        FileChanges fileChangesDelete = createFileChanges(SCM_PATH_DELETE, FileEditType.DELETE);
        fileChanges.put(fileChangesAdd1.getFileName(), fileChangesAdd1);
        fileChanges.put(fileChangesAdd2.getFileName(), fileChangesAdd2);
        fileChanges.put(fileChangesModify.getFileName(), fileChangesModify);
        fileChanges.put(fileChangesDelete.getFileName(), fileChangesDelete);
        when(delta.getFileChangesMap()).thenReturn(fileChanges);
        return delta;
    }

    /**
     * Creates a set of {@link FileChanges} with ambiguous paths.
     *
     * @param path
     *         The ambiguous path
     *
     * @return the set of changes
     */
    private Set<FileChanges> createAmbiguousFileChanges(final String path) {
        FileChanges change1 = mock(FileChanges.class);
        when(change1.getFileName()).thenReturn(Paths.get("src", path).toString());
        FileChanges change2 = mock(FileChanges.class);
        when(change2.getFileName()).thenReturn(Paths.get("main", path).toString());
        Set<FileChanges> changes = new HashSet<>();
        changes.add(change1);
        changes.add(change2);
        return changes;
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

    /**
     * Mocks a coverage tree which contains file nodes which represent {@link #REPORT_PATH_ADD_1}, {@link
     * #REPORT_PATH_ADD_2} and {@link #REPORT_PATH_MODIFY}.
     *
     * @return the {@link CoverageNode root} of the tree
     */
    private CoverageNode createMockedCoverageTree() {
        FileCoverageNode addFile1 = mock(FileCoverageNode.class);
        when(addFile1.getPath()).thenReturn(REPORT_PATH_ADD_1);
        FileCoverageNode addFile2 = mock(FileCoverageNode.class);
        when(addFile2.getPath()).thenReturn(REPORT_PATH_ADD_2);
        FileCoverageNode modifyFile = mock(FileCoverageNode.class);
        when(modifyFile.getPath()).thenReturn(REPORT_PATH_MODIFY);
        CoverageNode root = mock(CoverageNode.class);
        when(root.getAllFileCoverageNodes()).thenReturn(Arrays.asList(addFile1, addFile2, modifyFile));
        return root;
    }

    /**
     * Creates a {@link FilteredLog}.
     *
     * @return the created log
     */
    private FilteredLog createFilteredLog() {
        return new FilteredLog(LOG_NAME);
    }
}
