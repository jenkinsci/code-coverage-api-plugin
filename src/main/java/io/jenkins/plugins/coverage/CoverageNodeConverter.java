package io.jenkins.plugins.coverage;

import java.util.Map;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageLeaf;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * Converts {@link CoverageResult} instances to corresponding {@link CoverageNode} instances.
 *
 * @author Ullrich Hafner
 */
public class CoverageNodeConverter {
    /**
     * Converts a {@link CoverageResult} instance to the corresponding {@link CoverageNode} instance.
     *
     * @param result
     *         the result that should be converted
     */
    public static CoverageNode convert(final CoverageResult result) {
        CoverageNode node = createNode(result);
        attachLineAndBranchHits(result, node);

        return node;
    }

    private static void attachLineAndBranchHits(final CoverageResult result, final CoverageNode node) {
        CoveragePaint paint = result.getPaint();
        if (paint != null) {
            int[] uncoveredLines = paint.getUncoveredLines();
            if (uncoveredLines.length > 0) {
                node.setUncoveredLines(uncoveredLines);
            }
        }
    }

    private static CoverageNode createNode(final CoverageResult result) {
        CoverageElement element = result.getElement();
        CoverageMetric metric = CoverageMetric.valueOf(element.getName());
        if (result.getChildren().isEmpty()) {
            CoverageNode coverageNode = new CoverageNode(metric, result.getName());
            for (Map.Entry<CoverageElement, Ratio> coverage : result.getLocalResults().entrySet()) {
                Ratio ratio = coverage.getValue();
                CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.valueOf(coverage.getKey().getName()),
                        new Coverage((int) ratio.numerator, (int) (ratio.denominator - ratio.numerator)));
                coverageNode.add(leaf);
            }
            return coverageNode;
        }
        else {
            CoverageNode coverageNode = new CoverageNode(metric, result.getName());
            for (String childKey : result.getChildren()) {
                CoverageResult childResult = result.getChild(childKey);
                coverageNode.add(convert(childResult));
            }
            return coverageNode;
        }
    }

}
