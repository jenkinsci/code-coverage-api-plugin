package io.jenkins.plugins.coverage.metrics.steps;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.forensics.delta.Change;
import io.jenkins.plugins.forensics.delta.ChangeEditType;
import io.jenkins.plugins.forensics.delta.FileChanges;
import io.jenkins.plugins.forensics.delta.FileEditType;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link FileChangesProcessor}.
 *
 * @author Florian Orendi
 */
class FileChangesProcessorTest extends AbstractCoverageTest {
    private static final String TEST_FILE_1 = "Test1.java";
    private static final String TEST_FILE_2 = "Main.java";
    private static final String TEST_FILE_1_PATH = "test/example/" + TEST_FILE_1;
    private static final String TEST_FILE_1_PATH_OLD = "test/example/old/" + TEST_FILE_1;

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
     * The code changes that took place between the generation of {@link #TEST_REPORT_BEFORE} and {@link
     * #TEST_REPORT_AFTER}.
     */
    private static final Map<String, FileChanges> CODE_CHANGES = new HashMap<>();

    /**
     * The mapping of the used paths between the generation of {@link #TEST_REPORT_BEFORE} and {@link
     * #TEST_REPORT_AFTER}.
     */
    private static final Map<String, String> OLD_PATH_MAPPING = new HashMap<>();

    /**
     * Initializes a map with the inserted {@link #CODE_CHANGES}.
     */
    @BeforeAll
    static void initFileChanges() {
        Change insert1 = new Change(ChangeEditType.INSERT, 4, 4, 5, 9);
        Change insert2 = new Change(ChangeEditType.INSERT, 8, 8, 14, 18);
        Change insert3 = new Change(ChangeEditType.INSERT, 25, 25, 33, 36);
        Change replace = new Change(ChangeEditType.REPLACE, 10, 11, 20, 22);
        Change delete = new Change(ChangeEditType.DELETE, 16, 19, 26, 26);
        FileChanges fileChanges = new FileChanges(TEST_FILE_1_PATH, TEST_FILE_1_PATH_OLD,
                "test", FileEditType.RENAME, new HashMap<>());
        fileChanges.addChange(insert1);
        fileChanges.addChange(insert2);
        fileChanges.addChange(insert3);
        fileChanges.addChange(replace);
        fileChanges.addChange(delete);
        CODE_CHANGES.put(TEST_FILE_1_PATH, fileChanges);
        CODE_CHANGES.put(TEST_FILE_2,
                new FileChanges("empty", "empty", "", FileEditType.MODIFY, new HashMap<>()));
        OLD_PATH_MAPPING.put(TEST_FILE_1_PATH, TEST_FILE_1_PATH_OLD);
    }

    @Test
    void shouldAttachChangesCodeLines() {
        FileChangesProcessor fileChangesProcessor = createFileChangesProcessor();
        Node tree = readJacocoResult(TEST_REPORT_AFTER);
        fileChangesProcessor.attachChangedCodeLines(tree, CODE_CHANGES);

        assertThat(tree.findByHashCode(Metric.FILE, TEST_FILE_1_PATH.hashCode()))
                .isNotEmpty()
                .satisfies(node -> assertThat(node.get())
                        .isInstanceOfSatisfying(FileNode.class, f -> assertThat(f.getChangedLines())
                                        .containsExactly(
                            5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 20, 21, 22, 33, 34, 35, 36)));
        assertThat(tree.findByHashCode(Metric.FILE, TEST_FILE_2.hashCode()))
                .isNotEmpty()
                .satisfies(node -> assertThat(node.get())
                            .isInstanceOfSatisfying(FileNode.class, f -> assertThat(f.getChangedLines())
                            .isEmpty()));
    }

    @Test
    void shouldAttachFileCoverageDelta() {
        FileChangesProcessor fileChangesProcessor = createFileChangesProcessor();
        Node reference = readJacocoResult(TEST_REPORT_BEFORE);
        Node tree = readJacocoResult(TEST_REPORT_AFTER);
        fileChangesProcessor.attachFileCoverageDeltas(tree, reference, OLD_PATH_MAPPING);

        assertThat(tree.findByHashCode(Metric.FILE, TEST_FILE_1_PATH.hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileNode.class);
                    verifyFileCoverageDeltaOfTestFile1((FileNode) node.get());
                });
    }

    /**
     * Verifies the file coverage delta of {@link #TEST_FILE_1}.
     *
     * @param file
     *         The referencing coverage tree {@link FileNode node}
     */
    private void verifyFileCoverageDeltaOfTestFile1(final FileNode file) {
        assertThat(file.getName()).isEqualTo(TEST_FILE_1);
        assertThat(file.getChangeCoverage(Metric.LINE)).isEqualTo(Fraction.getFraction(3, 117));
        assertThat(file.getChangeCoverage(Metric.BRANCH)).isEqualTo(Fraction.getFraction(3, 24));
        assertThat(file.getChangeCoverage(Metric.INSTRUCTION)).isEqualTo(Fraction.getFraction(90, 999));
        assertThat(file.getChangeCoverage(Metric.METHOD)).isEqualTo(Fraction.getFraction(-4, 30));
        assertThat(file.getChangeCoverage(Metric.CLASS)).isEqualTo(Fraction.ZERO);
        assertThat(file.getChangeCoverage(Metric.FILE)).isEqualTo(Fraction.ZERO);
    }

    @Test
    void shouldAttachIndirectCoverageChanges() {
        FileChangesProcessor fileChangesProcessor = createFileChangesProcessor();
        Node reference = readJacocoResult(TEST_REPORT_BEFORE);
        Node tree = readJacocoResult(TEST_REPORT_AFTER);
        fileChangesProcessor.attachIndirectCoveragesChanges(tree, reference, CODE_CHANGES, OLD_PATH_MAPPING);

        assertThat(tree.findByHashCode(Metric.FILE, TEST_FILE_1_PATH.hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileNode.class);
                    FileNode file = (FileNode) node.get();
                    assertThat(file.getIndirectCoverageChanges()).containsExactly(
                            new SimpleEntry<>(11, -1),
                            new SimpleEntry<>(29, -1),
                            new SimpleEntry<>(31, 1)
                    );
                });
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
