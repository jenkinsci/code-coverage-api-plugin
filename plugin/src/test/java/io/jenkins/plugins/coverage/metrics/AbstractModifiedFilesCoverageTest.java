package io.jenkins.plugins.coverage.metrics;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import io.jenkins.plugins.coverage.metrics.steps.FileChangesProcessor;
import io.jenkins.plugins.forensics.delta.Change;
import io.jenkins.plugins.forensics.delta.ChangeEditType;
import io.jenkins.plugins.forensics.delta.FileChanges;
import io.jenkins.plugins.forensics.delta.FileEditType;

/**
 * Provides a coverage tree that consists of {@link FileNode}s with modified code lines and corresponding coverage
 * information. The test is based on the coverage reports 'file-changes-test-before.xml' and
 * 'file-changes-test-after.xml'.
 *
 * @author Florian Orendi
 */
public abstract class AbstractModifiedFilesCoverageTest extends AbstractCoverageTest {

    private static final String TEST_FILE_MODIFIED = "Test1.java";
    private static final String TEST_FILE_NOT_MODIFIED = "Main.java";
    private static final String TEST_FILE_MODIFIED_PATH = "test/example/" + TEST_FILE_MODIFIED;
    private static final String TEST_FILE_MODIFIED_PATH_OLD = "test/example/old/" + TEST_FILE_MODIFIED;

    /**
     * A JaCoCo report which contains the code coverage of a test project <b>before</b> the {@link #CODE_CHANGES} has
     * been inserted.
     */
    private static final String TEST_REPORT_BEFORE = "file-changes-test-before.xml";
    /**
     * A JaCoCo report which contains the code coverage of a test project <b>after</b> the {@link #CODE_CHANGES} has
     * been inserted.
     */
    private static final String TEST_REPORT_AFTER = "file-changes-test-after.xml";

    /**
     * The code changes that took place between the generation of {@link #TEST_REPORT_BEFORE} and
     * {@link #TEST_REPORT_AFTER}.
     */
    private static final Map<String, FileChanges> CODE_CHANGES = new HashMap<>();

    /**
     * The mapping of the used paths between the generation of {@link #TEST_REPORT_BEFORE} and
     * {@link #TEST_REPORT_AFTER}.
     */
    private static final Map<String, String> OLD_PATH_MAPPING = new HashMap<>();

    /**
     * Initializes a map with the inserted {@link #CODE_CHANGES}.
     */
    @BeforeAll
    static void initFileChanges() {
        var insert1 = new Change(ChangeEditType.INSERT, 4, 4, 5, 9);
        var insert2 = new Change(ChangeEditType.INSERT, 8, 8, 14, 18);
        var insert3 = new Change(ChangeEditType.INSERT, 25, 25, 33, 36);
        var replace = new Change(ChangeEditType.REPLACE, 10, 11, 20, 22);
        var delete = new Change(ChangeEditType.DELETE, 16, 19, 26, 26);
        var fileChanges = new FileChanges(TEST_FILE_MODIFIED_PATH, TEST_FILE_MODIFIED_PATH_OLD,
                "test", FileEditType.RENAME, new HashMap<>());
        fileChanges.addChange(insert1);
        fileChanges.addChange(insert2);
        fileChanges.addChange(insert3);
        fileChanges.addChange(replace);
        fileChanges.addChange(delete);
        CODE_CHANGES.put(TEST_FILE_MODIFIED_PATH, fileChanges);
        CODE_CHANGES.put(TEST_FILE_NOT_MODIFIED,
                new FileChanges("empty", "empty", "", FileEditType.MODIFY, new HashMap<>()));
        OLD_PATH_MAPPING.put(TEST_FILE_MODIFIED_PATH, TEST_FILE_MODIFIED_PATH_OLD);
    }

    /**
     * Creates a coverage tree that consists of {@link FileNode}s with and without modified lines together with the
     * corresponding coverage information.
     */
    protected Node createCoverageTree() {
        var fileChangesProcessor = createFileChangesProcessor();
        var reference = readJacocoResult(TEST_REPORT_BEFORE);
        var tree = readJacocoResult(TEST_REPORT_AFTER);
        fileChangesProcessor.attachChangedCodeLines(tree, CODE_CHANGES);
        fileChangesProcessor.attachFileCoverageDeltas(tree, reference, OLD_PATH_MAPPING);
        fileChangesProcessor.attachIndirectCoveragesChanges(tree, reference, CODE_CHANGES, OLD_PATH_MAPPING);
        return tree;
    }

    /**
     * Gets the name of the test file with modified lines.
     */
    protected String getNameOfFileWithModifiedLines() {
        return TEST_FILE_MODIFIED;
    }

    /**
     * Gets the path of the test file with modified lines.
     */
    protected String getPathOfFileWithModifiedLines() {
        return TEST_FILE_MODIFIED_PATH;
    }

    /**
     * Gets the name of the test file without modified lines.
     */
    protected String getNameOfFileWithoutModifiedLines() {
        return TEST_FILE_NOT_MODIFIED;
    }

    /**
     * Creates an instance of {@link FileChangesProcessor}.
     *
     * @return the created instance
     */
    private FileChangesProcessor createFileChangesProcessor() {
        return new FileChangesProcessor();
    }
}
