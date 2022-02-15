package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.TreeMapNode;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageColorizationLevel;
import io.jenkins.plugins.coverage.targets.CoverageResult;

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

        final double totalLines = 323.0;
        final double coveredLines = 294.0;
        final double coveredPercentage = coveredLines / totalLines * 100.0;

        TreeMapNode root = new TreeMapNodeConverter().toTeeChartModel(tree);
        assertThat(root.getName()).isEqualTo("Java coding style: jacoco-codingstyle.xml");
        assertThat(root.getValue()).containsExactly(totalLines, coveredLines);
        assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsHex(coveredPercentage));

        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner.util");
                    assertThat(node.getValue()).containsExactly(totalLines, coveredLines);
                    assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsHex(coveredPercentage));
                }
        );
    }

    @Test
    void shouldConvertAnalysisModelToTree() {
        CoverageNode tree = readNode("jacoco-analysis-model.xml");
        tree.splitPackages();

        TreeMapNode root = new TreeMapNodeConverter().toTeeChartModel(tree);

        final double totalLines = 6368.0;
        final double coveredLines = 6083.0;
        final double coveredPercentage = coveredLines / totalLines * 100.0;

        assertThat(root.getName()).isEqualTo("Static Analysis Model and Parsers: jacoco-analysis-model.xml");
        assertThat(root.getValue()).containsExactly(totalLines, coveredLines);
        assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsHex(coveredPercentage));
        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner");
                    assertThat(node.getValue()).containsExactly(totalLines, coveredLines);
                    assertThat(node.getItemStyle().getColor()).isEqualTo(getNodeColorAsHex(coveredPercentage));
                }
        );
    }

    /**
     * Gets the matching fill color for the coverage percentage.
     *
     * @param coveredPercentage
     *         The coverage percentage
     *
     * @return the fill color as a hex string
     */
    private String getNodeColorAsHex(final Double coveredPercentage) {
        return ColorUtils.colorAsHex(CoverageColorizationLevel
                .getDisplayColorsOfCoveragePercentage(coveredPercentage)
                .getFillColor());
    }
}
