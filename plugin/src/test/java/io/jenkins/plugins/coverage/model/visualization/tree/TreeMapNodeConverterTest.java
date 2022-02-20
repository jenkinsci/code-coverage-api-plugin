package io.jenkins.plugins.coverage.model.visualization.tree;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.model.AbstractCoverageTest;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.visualization.tree.TreeMapNodeConverter;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TreeMapNodeConverter}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverterTest extends AbstractCoverageTest {

    @Test
    void shouldConvertCodingStyleToTree() {
        CoverageNode tree = readNode(Paths.get("..", "..", "jacoco-codingstyle.xml").toString());
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
        CoverageNode tree = readNode(Paths.get("..", "..", "jacoco-analysis-model.xml").toString());
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
