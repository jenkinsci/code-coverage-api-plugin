package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link MethodCoverageNode}.
 *
 * @author Florian Orendi
 */
class MethodCoverageNodeTest {

    private static final String NAME = "METHOD";
    private static final int LINE = 5;

    @Test
    void shouldGetValidLineNumberOfMethod() {
        MethodCoverageNode node = createMethodCoverageNode();
        assertThat(node.hasValidLineNumber()).isTrue();
        assertThat(node.getLineNumber()).isEqualTo(5);
    }

    @Test
    void shouldRecognizeInvalidLineNumberOfMethod() {
        MethodCoverageNode node = new MethodCoverageNode(NAME, 0);
        assertThat(node.hasValidLineNumber()).isFalse();
    }

    @Test
    void shouldCopyTree() {
        MethodCoverageNode node = createMethodCoverageNode();
        CoverageNode root = node.getParent().copyTree();
        assertThat(root.getChildren()).containsExactly(node);
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(MethodCoverageNode.class)
                .withPrefabValues(CoverageNode.class,
                        new MethodCoverageNode("", LINE),
                        new CoverageNode(CoverageMetric.LINE, "line"))
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withIgnoredFields("parent")
                .verify();
    }

    /**
     * Creates an instance of {@link MethodCoverageNode}.
     *
     * @return the created instance
     */
    private MethodCoverageNode createMethodCoverageNode() {
        CoverageNode parent = new CoverageNode(CoverageMetric.LINE, "");
        CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.LINE, Coverage.NO_COVERAGE);
        MethodCoverageNode node = new MethodCoverageNode(NAME, LINE);
        node.setParent(parent);
        parent.add(node);
        node.add(leaf);
        return node;
    }
}
