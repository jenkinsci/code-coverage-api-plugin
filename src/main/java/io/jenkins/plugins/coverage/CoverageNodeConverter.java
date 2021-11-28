package io.jenkins.plugins.coverage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageLeaf;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.FileCoverageNode;
import io.jenkins.plugins.coverage.model.PackageCoverageNode;
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
    private final Map<CoverageNode, CoveragePaint> paintedFiles = new HashMap<>();

    /**
     * Converts a {@link CoverageResult} instance to the corresponding {@link CoverageNode} instance.
     *
     * @param result
     *         the result that should be converted
     *
     * @return the root node of the coverage tree
     */
    public CoverageNode convert(final CoverageResult result) {
        CoverageNode node = createNode(result);
        attachLineAndBranchHits(result, node);

        return node;
    }

    private void attachLineAndBranchHits(final CoverageResult result, final CoverageNode node) {
        CoveragePaint paint = result.getPaint();
        if (paint != null) {
            int[] uncoveredLines = paint.getUncoveredLines();
            if (uncoveredLines.length > 0) {
                node.setUncoveredLines(uncoveredLines);
            }
            if (node.getMetric().equals(CoverageMetric.FILE)) {
                paintedFiles.put(node, paint);
            }
        }
    }

    private CoverageNode createNode(final CoverageResult result) {
        CoverageElement element = result.getElement();
        CoverageMetric metric = CoverageMetric.valueOf(element.getName());
        if (result.getChildren().isEmpty()) {
            CoverageNode coverageNode = createNode(metric, result);
            for (Map.Entry<CoverageElement, Ratio> coverage : result.getLocalResults().entrySet()) {
                Ratio ratio = coverage.getValue();
                CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.valueOf(coverage.getKey().getName()),
                        new Coverage((int) ratio.numerator, (int) (ratio.denominator - ratio.numerator)));
                coverageNode.add(leaf);
            }
            return coverageNode;
        }
        else {
            CoverageNode coverageNode = createNode(metric, result);
            for (String childKey : result.getChildren()) {
                CoverageResult childResult = result.getChild(childKey);
                coverageNode.add(convert(childResult));
            }
            return coverageNode;
        }
    }

    private CoverageNode createNode(final CoverageMetric metric, final CoverageResult result) {
        if (metric.equals(CoverageMetric.FILE)) {
            return new FileCoverageNode(result.getName(), result.getRelativeSourcePath());
        }
        if (metric.equals(CoverageMetric.PACKAGE)) {
            return new PackageCoverageNode(result.getName());
        }
        return new CoverageNode(metric, result.getName());
    }

    public Set<Entry<CoverageNode, CoveragePaint>> getPaintedFiles() {
        return paintedFiles.entrySet();
    }
}
