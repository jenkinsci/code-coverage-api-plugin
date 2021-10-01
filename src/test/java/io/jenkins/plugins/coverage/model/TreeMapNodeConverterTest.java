package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TreeMapNodeConverter}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverterTest extends AbstractCoverageTest {
    @Test
    void shouldConvertCodingStyleToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-codingstyle.xml");

        CoverageNode tree = CoverageNodeConverter.convert(report);
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
    void shouldConvertAnalysisModelToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-analysis-model.xml");

        CoverageNode tree = CoverageNodeConverter.convert(report);
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
