package io.jenkins.plugins.coverage.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.saxon.trans.SymbolicName.F;

import io.jenkins.plugins.coverage.model.coverage.FileCoverageProcessor;
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

    private static final FileCoverageProcessor FILE_COVERAGE_PROCESSOR = new FileCoverageProcessor();

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
        return createNode(result);
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
            FileCoverageNode fileCoverageNode = new FileCoverageNode(result.getName(), result.getRelativeSourcePath());
            attachCoverageLineMapping(result, fileCoverageNode);
            return fileCoverageNode;
        }
        if (metric.equals(CoverageMetric.PACKAGE)) {
            return new PackageCoverageNode(result.getName());
        }
        return new CoverageNode(metric, result.getName());
    }

    private void attachCoverageLineMapping(final CoverageResult result, final FileCoverageNode node) {
        CoveragePaint paint = result.getPaint();
        if (paint != null) {
            node.setUncoveredLines(paint.getUncoveredLines());
            paintedFiles.put(node, paint);
            FILE_COVERAGE_PROCESSOR.attachCoveragePerLine(node, paint);
        }
    }

    public Set<Entry<CoverageNode, CoveragePaint>> getPaintedFiles() {
        return paintedFiles.entrySet();
    }
}
