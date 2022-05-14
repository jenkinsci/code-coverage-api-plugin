package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link PackageCoverageNode}.
 *
 * @author Florian Orendi
 */
class PackageCoverageNodeTest {

    private static final String PACKAGE = "test.path";
    private static final String PATH = "test/path";

    @Test
    void shouldGetPath() {
        PackageCoverageNode node = createPackageCoverageNode();
        assertThat(node.getPath()).isEqualTo(PATH);
    }

    @Test
    void shouldCopyTree() {
        PackageCoverageNode node = createPackageCoverageNode();
        CoverageNode root = node.getParent().copyTree();
        assertThat(root.getChildren()).containsExactly(node);
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(PackageCoverageNode.class)
                .withPrefabValues(CoverageNode.class,
                        new PackageCoverageNode(PACKAGE),
                        new CoverageNode(CoverageMetric.FILE, "file.txt"))
                .suppress(Warning.NONFINAL_FIELDS)
                .usingGetClass()
                .withIgnoredFields("parent")
                .verify();
    }

    /**
     * Creates an instance of {@link PackageCoverageNode}.
     *
     * @return the created instance
     */
    private PackageCoverageNode createPackageCoverageNode() {
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, "");
        CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.LINE, Coverage.NO_COVERAGE);
        PackageCoverageNode node = new PackageCoverageNode(PACKAGE);
        node.setParent(parent);
        parent.add(node);
        node.add(leaf);
        return node;
    }
}
