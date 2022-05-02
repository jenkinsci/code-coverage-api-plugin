package io.jenkins.plugins.coverage.model;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link FileCoverageNode}.
 *
 * @author Florian Orendi
 */
class FileCoverageNodeTest {

    private static final String PATH = "path";
    private static final int LINE = 5;
    private static final int HIT_DELTA = 10;
    private static final Coverage COVERAGE = new Coverage(2, 3);
    private static final CoveragePercentage COVERAGE_DELTA = CoveragePercentage.getCoveragePercentage(0.5);
    private static final CoverageMetric COVERAGE_METRIC = CoverageMetric.LINE;

    @Test
    void shouldHaveWorkingGetters() {
        FileCoverageNode node = createFileCoverageNode();

        assertThat(node.getPath()).isEqualTo(PATH);

        assertThat(node.hasFileCoverageDelta(COVERAGE_METRIC)).isTrue();
        assertThat(node.getFileCoverageDeltaForMetric(COVERAGE_METRIC)).isEqualTo(COVERAGE_DELTA);

        assertThat(node.getChangedCodeLines()).containsExactly(LINE);
        assertThat(node.getCoveragePerLine()).containsExactly(new AbstractMap.SimpleEntry<>(LINE, COVERAGE));
        assertThat(node.getIndirectCoverageChanges()).containsExactly(new AbstractMap.SimpleEntry<>(LINE, HIT_DELTA));
    }

    @Test
    void shouldCopyTree() {
        FileCoverageNode node = createFileCoverageNode();
        CoverageNode root = node.getParent().copyTree();
        assertThat(root.getChildren()).containsExactly(node);
        assertThat(root.getChildren().get(0))
                .isInstanceOf(FileCoverageNode.class)
                .satisfies(n -> {
                    FileCoverageNode fileNode = (FileCoverageNode) n;
                    assertThat(fileNode.getPath()).isEqualTo(PATH);
                    assertThat(fileNode.getCoveragePerLine()).containsExactly(new SimpleEntry<>(LINE, COVERAGE));
                    assertThat(fileNode.getFileCoverageDeltaForMetric(COVERAGE_METRIC)).isEqualTo(COVERAGE_DELTA);
                    assertThat(fileNode.getChangedCodeLines()).containsExactly(LINE);
                    assertThat(fileNode.getIndirectCoverageChanges()).containsExactly(
                            new SimpleEntry<>(LINE, HIT_DELTA));
                });
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(FileCoverageNode.class)
                .withPrefabValues(CoverageNode.class,
                        new FileCoverageNode("", "file.txt"),
                        new CoverageNode(CoverageMetric.LINE, "line"))
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withIgnoredFields("parent")
                .verify();
    }

    /**
     * Creates an instance of {@link FileCoverageNode}.
     *
     * @return the created instance
     */
    private FileCoverageNode createFileCoverageNode() {
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, "");
        CoverageLeaf leaf = new CoverageLeaf(COVERAGE_METRIC, COVERAGE);

        FileCoverageNode fileCoverageNode = new FileCoverageNode("", PATH);
        fileCoverageNode.setParent(parent);
        fileCoverageNode.add(leaf);
        parent.add(fileCoverageNode);

        fileCoverageNode.putCoveragePerLine(LINE, COVERAGE);
        fileCoverageNode.putFileCoverageDelta(COVERAGE_METRIC, COVERAGE_DELTA);
        fileCoverageNode.putIndirectCoverageChange(LINE, HIT_DELTA);
        fileCoverageNode.addChangedCodeLine(LINE);

        return fileCoverageNode;
    }
}
