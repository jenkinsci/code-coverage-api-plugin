package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.TreeMapNode;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TreeMapNodeConverter}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverterTest extends AbstractCoverageTest {
    @Test
    void shouldConvertCodingStyleToTree() {
        CoverageNode tree = readNode("jacoco-codingstyle.xml");
        tree.splitPackages();

        TreeMapNode root = new TreeMapNodeConverter().toTeeChartModel(tree);
        assertThat(root.getName()).isEqualTo("Java coding style: jacoco-codingstyle.xml");
        assertThat(root.getValue()).containsExactly(323.0, 294.0);

        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner.util");
                    assertThat(node.getValue()).containsExactly(323.0, 294.0);
                }
        );
    }

    @Test
    void shouldConvertAnalysisModelToTree() {
        CoverageNode tree = readNode("jacoco-analysis-model.xml");
        tree.splitPackages();

        TreeMapNode root = new TreeMapNodeConverter().toTeeChartModel(tree);

        assertThat(root.getName()).isEqualTo("Static Analysis Model and Parsers: jacoco-analysis-model.xml");
        assertThat(root.getValue()).containsExactly(6368.0, 6083.0);
        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner");
                    assertThat(node.getValue()).containsExactly(6368.0, 6083.0);
                }
        );
    }
}
