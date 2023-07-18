package io.jenkins.plugins.coverage.metrics.steps;

import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link FileChangesProcessor}.
 *
 * @author Florian Orendi
 */
class FileChangesProcessorTest extends AbstractModifiedFilesCoverageTest {

    @Test
    void shouldAttachChangesCodeLines() {
        var tree = createCoverageTree();

        assertThat(tree.findByHashCode(Metric.FILE, getPathOfFileWithModifiedLines().hashCode()))
                .isNotEmpty()
                .satisfies(node -> assertThat(node.get())
                        .isInstanceOfSatisfying(FileNode.class, f -> assertThat(f.getModifiedLines())
                                .containsExactly(
                                        5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 20, 21, 22, 33, 34, 35, 36)));
        assertThat(tree.findByHashCode(Metric.FILE, getNameOfFileWithoutModifiedLines().hashCode()))
                .isNotEmpty()
                .satisfies(node -> assertThat(node.get())
                        .isInstanceOfSatisfying(FileNode.class, f -> assertThat(f.getModifiedLines())
                                .isEmpty()));
    }

    @Test
    void shouldAttachFileCoverageDelta() {
        var tree = createCoverageTree();

        assertThat(tree.findByHashCode(Metric.FILE, getPathOfFileWithModifiedLines().hashCode()))
                .isNotEmpty()
                .satisfies(node -> {
                    assertThat(node.get()).isInstanceOf(FileNode.class);
                    verifyFileCoverageDeltaOfTestFile1((FileNode) node.get());
                });
    }

    @Test
    void shouldAttachIndirectCoverageChanges() {
        var tree = createCoverageTree();

        assertThat(tree.findByHashCode(Metric.FILE, getPathOfFileWithModifiedLines().hashCode()))
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
     * Verifies the file coverage delta of {@link #getPathOfFileWithModifiedLines() the modified file}.
     *
     * @param file
     *         The referencing coverage tree {@link FileNode node}
     */
    private void verifyFileCoverageDeltaOfTestFile1(final FileNode file) {
        assertThat(file.getName()).isEqualTo(getNameOfFileWithModifiedLines());
        assertThat(file.getDelta(Metric.LINE)).isEqualTo(Fraction.getFraction(3, 117));
        assertThat(file.getDelta(Metric.BRANCH)).isEqualTo(Fraction.getFraction(3, 24));
        assertThat(file.getDelta(Metric.INSTRUCTION)).isEqualTo(Fraction.getFraction(90, 999));
        assertThat(file.getDelta(Metric.METHOD)).isEqualTo(Fraction.getFraction(-4, 30));
        assertThat(file.getDelta(Metric.CLASS)).isEqualTo(Fraction.ZERO);
        assertThat(file.getDelta(Metric.FILE)).isEqualTo(Fraction.ZERO);
    }
}
