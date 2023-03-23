package io.jenkins.plugins.coverage.metrics.charts;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.echarts.LabeledTreeMapNode;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.coverage.metrics.color.ColorProviderFactory;
import io.jenkins.plugins.coverage.metrics.color.CoverageLevel;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TreeMapNodeConverter}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverterTest extends AbstractCoverageTest {
    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();
    private static final String PREFIX = "../steps/";

    @Test
    void shouldConvertCodingStyleToTree() {
        Node tree = readJacocoResult(PREFIX + JACOCO_CODING_STYLE_FILE);

        LabeledTreeMapNode root = new TreeMapNodeConverter().toTreeChartModel(tree, Metric.LINE, COLOR_PROVIDER);
        assertThat(root.getName()).isEqualTo("Java coding style");

        var overallCoverage = String.valueOf(JACOCO_CODING_STYLE_TOTAL);
        assertThat(root.getValue()).contains(overallCoverage);

        var overallCoveragePercentage = 100.0 * JACOCO_CODING_STYLE_COVERED / JACOCO_CODING_STYLE_TOTAL;
        assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsRGBHex(overallCoveragePercentage));

        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> {
                    assertThat(node.getName()).isEqualTo("edu.hm.hafner.util");
                    assertThat(node.getValue()).contains(overallCoverage);
                    assertThat(root.getItemStyle().getColor()).isEqualTo(getNodeColorAsRGBHex(overallCoveragePercentage));
                }
        );
    }

    @Test
    void shouldReadBranchCoverage() {
        Node tree = readJacocoResult(PREFIX + JACOCO_ANALYSIS_MODEL_FILE);

        LabeledTreeMapNode root = new TreeMapNodeConverter().toTreeChartModel(tree, Metric.BRANCH, COLOR_PROVIDER);

        var nodes = aggregateChildren(root);
        nodes.stream().filter(node -> node.getName().endsWith(".java")).forEach(node -> {
            assertThat(node.getValue()).hasSize(2);
        });
    }

    private List<LabeledTreeMapNode> aggregateChildren(final LabeledTreeMapNode root) {
        var children = root.getChildren();
        var subChildren = children.stream()
                .map(this::aggregateChildren)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        subChildren.addAll(children);
        return subChildren;
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
