package io.jenkins.plugins.coverage.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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
                        new Coverage.CoverageBuilder().setCovered((int) ratio.numerator)
                                .setMissed((int) (ratio.denominator - ratio.numerator))
                                .build());
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
        if (metric.equals(CoverageMetric.METHOD)) {
            Optional<String> line = result.getAdditionalProperty("lineNumber").stream().findAny();
            if (line.isPresent() && line.get().matches("\\d+")) {
                return new MethodCoverageNode(result.getName(), Integer.parseInt(line.get()));
            }
            // fallback if method line has not been set properly since this is a temporary workaround
            // until the adapter structure has been replaced
            return new MethodCoverageNode(result.getName(), 0);
        }
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
            attachCoveragePerLine(node, paint);
        }
    }

    private void attachCoveragePerLine(final FileCoverageNode node, final CoveragePaint paint) {
        int[] lines = paint.getAllLines();
        SortedMap<Integer, Coverage> coverageDetails = new TreeMap<>();
        for (int line : lines) {
            if (paint.getBranchTotal(line) > 0) {
                int covered = paint.getBranchCoverage(line);
                int missed = paint.getBranchTotal(line) - covered;
                coverageDetails.put(line, new Coverage.CoverageBuilder().setCovered(paint.getBranchCoverage(line))
                        .setMissed(missed)
                        .build());
            }
            else {
                int covered = paint.getHits(line) > 0 ? 1 : 0;
                coverageDetails.put(line,
                        new Coverage.CoverageBuilder().setCovered(covered).setMissed(1 - covered).build());
            }
        }
        node.setCoveragePerLine(coverageDetails);
    }

    public Set<Entry<CoverageNode, CoveragePaint>> getPaintedFiles() {
        return paintedFiles.entrySet();
    }
}
