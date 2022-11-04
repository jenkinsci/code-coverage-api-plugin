package io.jenkins.plugins.coverage.model;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.TreeMapNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;
import io.jenkins.plugins.coverage.model.visualization.tree.TreeMapNodeConverter;

import static io.jenkins.plugins.coverage.model.AbstractCoverageITest.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TreeMapNodeConverter}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverterTest extends AbstractCoverageTest {

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldConvertCodingStyleToTree() {
        Node tree = readJacocoResult(Paths.get(JACOCO_CODING_STYLE_FILE).toString());

        final double totalLines = JACOCO_CODING_STYLE_TOTAL;
        final double coveredLines = JACOCO_CODING_STYLE_COVERED;
        final double coveredPercentage = coveredLines / totalLines * 100.0;

        TreeMapNode root = new TreeMapNodeConverter().toTeeChartModel(tree, Metric.LINE, COLOR_PROVIDER);
        assertThat(root.getName()).isEqualTo("Java coding style");
        assertThat(root.getValue()).containsExactly(totalLines, coveredLines);
        assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsRGBHex(coveredPercentage));

        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner.util");
                    assertThat(node.getValue()).containsExactly(totalLines, coveredLines);
                    assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsRGBHex(coveredPercentage));
                }
        );
    }

    @Test
    void shouldConvertAnalysisModelToTree() {
        Node tree = readJacocoResult(Paths.get(JACOCO_ANALYSIS_MODEL_FILE).toString());

        TreeMapNode root = new TreeMapNodeConverter().toTeeChartModel(tree, Metric.LINE, COLOR_PROVIDER);

        double totalLines = JACOCO_ANALYSIS_MODEL_TOTAL;
        double coveredLines = JACOCO_ANALYSIS_MODEL_COVERED;
        double coveredPercentage = coveredLines / totalLines * 100.0;

        assertThat(root.getName()).isEqualTo("Static Analysis Model and Parsers");
        assertThat(root.getValue()).containsExactly(totalLines, coveredLines);
        assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsRGBHex(coveredPercentage));
        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner");
                    assertThat(node.getValue()).containsExactly(totalLines, coveredLines);
                    assertThat(node.getItemStyle().getColor()).isEqualTo(getNodeColorAsRGBHex(coveredPercentage));
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
    private String getNodeColorAsRGBHex(final Double coveredPercentage) {
        return CoverageLevel
                .getDisplayColorsOfCoverageLevel(coveredPercentage, COLOR_PROVIDER)
                .getFillColorAsRGBHex();
    }
}
