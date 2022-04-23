package io.jenkins.plugins.coverage.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.forensics.delta.model.Change;
import io.jenkins.plugins.forensics.delta.model.ChangeEditType;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.delta.model.FileEditType;

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
     * Initializes a map with the inserted {@link #CODE_CHANGES}.
     */
    @BeforeAll
    static void initFileChanges() {
        Change insert1 = new Change(ChangeEditType.INSERT, 4, 4, 5, 9);
        Change insert2 = new Change(ChangeEditType.INSERT, 8, 8, 14, 18);
        Change insert3 = new Change(ChangeEditType.INSERT, 25, 25, 33, 36);
        Change replace = new Change(ChangeEditType.REPLACE, 10, 11, 20, 22);
        Change delete = new Change(ChangeEditType.DELETE, 16, 19, 26, 26);
        FileChanges fileChanges = new FileChanges("", "", FileEditType.MODIFY, new HashMap<>());
        fileChanges.addChange(insert1);
        fileChanges.addChange(insert2);
        fileChanges.addChange(insert3);
        fileChanges.addChange(replace);
        fileChanges.addChange(delete);
        CODE_CHANGES.put(TEST_FILE_1_PATH, fileChanges);
        CODE_CHANGES.put(TEST_FILE_2, new FileChanges("", "", FileEditType.MODIFY, new HashMap<>()));
    }

    @Test
    void shouldAttachChangesCodeLines() {
        FileChangesProcessor fileChangesProcessor = createFileChangesProcessor();
        CoverageNode tree = readCoverageTree(TEST_REPORT_AFTER);
        fileChangesProcessor.attachChangedCodeLines(tree, CODE_CHANGES);

        assertThat(tree.findByHashCode(FILE, TEST_FILE_1.hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileCoverageNode.class);
                    assertThat(((FileCoverageNode) node.get()).getChangedCodeLines()).containsExactly(
                            5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 20, 21, 22, 33, 34, 35, 36
                    );
                });
        assertThat(tree.findByHashCode(FILE, TEST_FILE_2.hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileCoverageNode.class);
                    assertThat(((FileCoverageNode) node.get()).getChangedCodeLines()).isEmpty();
                });
    }

    @Test
    void shouldAttachFileCoverageDelta() {
        FileChangesProcessor fileChangesProcessor = createFileChangesProcessor();
        CoverageNode reference = readCoverageTree(TEST_REPORT_BEFORE);
        CoverageNode tree = readCoverageTree(TEST_REPORT_AFTER);
        fileChangesProcessor.attachFileCoverageDeltas(tree, reference);

        assertThat(tree.findByHashCode(FILE, TEST_FILE_1.hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileCoverageNode.class);
                    verifyFileCoverageDeltaOfTestFile1((FileCoverageNode) node.get());
                });
    }

    /**
     * Verifies the file coverage delta of {@link #TEST_FILE_1}.
     *
     * @param file
     *         The referencing coverage tree {@link FileCoverageNode node}
     */
    private void verifyFileCoverageDeltaOfTestFile1(final FileCoverageNode file) {
        assertThat(file.getName()).isEqualTo(TEST_FILE_1);
        assertThat(file.getFileCoverageDeltaForMetric(LINE)
                .compareTo(Fraction.getFraction(3, 117))).isZero();
        assertThat(file.getFileCoverageDeltaForMetric(BRANCH)
                .compareTo(Fraction.getFraction(3, 24))).isZero();
        assertThat(file.getFileCoverageDeltaForMetric(INSTRUCTION)
                .compareTo(Fraction.getFraction(90, 999))).isZero();
        assertThat(file.getFileCoverageDeltaForMetric(METHOD)
                .compareTo(Fraction.getFraction(-4, 30))).isZero();
        assertThat(file.getFileCoverageDeltaForMetric(CLASS)
                .compareTo(Fraction.ZERO)).isZero();
        assertThat(file.getFileCoverageDeltaForMetric(FILE)
                .compareTo(Fraction.ZERO)).isZero();
    }

    @Test
    void shouldAttachIndirectCoverageChanges() {
        FileChangesProcessor fileChangesProcessor = createFileChangesProcessor();
        CoverageNode reference = readCoverageTree(TEST_REPORT_BEFORE);
        CoverageNode tree = readCoverageTree(TEST_REPORT_AFTER);
        fileChangesProcessor.attachIndirectCoveragesChanges(tree, reference, CODE_CHANGES);

        assertThat(tree.findByHashCode(FILE, TEST_FILE_1.hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileCoverageNode.class);
                    FileCoverageNode file = (FileCoverageNode) node.get();
                    assertThat(file.getIndirectCoverageChanges()).containsExactly(
                            new SimpleEntry<>(11, -1),
                            new SimpleEntry<>(29, -1),
                            new SimpleEntry<>(31, 1)
                    );
                });
    }

    /**
     * Reads the coverage tree from a report.
     *
     * @param file
     *         The name of the report
     *
     * @return the {@link CoverageNode} root of the tree
     */
    private CoverageNode readCoverageTree(final String file) {
        CoverageNode root = readNode(file);
        root.splitPackages();
        return root;
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