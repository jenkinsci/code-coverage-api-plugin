package io.jenkins.plugins.coverage.model.coverage;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.FileCoverageNode;
import io.jenkins.plugins.coverage.targets.CoveragePaint;

public class FileCoverageProcessor {

    public void attachUnexpectedCoveragesChanges(final CoverageNode node, final CoverageNode referenceNode,
            final FilteredLog log) {
        log.logInfo("Obtaining unexpected coverage changes...");
        Map<String, FileCoverageNode> fileNodes = node.getAllFileCoverageNodes().stream()
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        Map<String, FileCoverageNode> referenceFileNodes = referenceNode.getAllFileCoverageNodes().stream()
                .filter(reference -> fileNodes.containsKey(reference.getPath()))
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        fileNodes.forEach((path, fileNode) -> fileNode.getCoveragePerLine().forEach((line, coverage) -> {
            if (fileNode.getChangedCodeLines().isEmpty()
                    && referenceFileNodes.get(path) != null
                    && referenceFileNodes.get(path).getCoveragePerLine().get(line) != null) {
                // TODO: a mapping between reference lines and current file lines is required in order to find unexpected coverage changes in changed files
                //if (!fileNode.getChangedCodeLines().contains(line)) {
                Coverage referenceCoverage = referenceFileNodes.get(path).getCoveragePerLine().get(line);
                int covered = coverage.getCovered();
                int referenceCovered = referenceCoverage.getCovered();
                if (covered != referenceCovered) {
                    fileNode.putUnexpectedCoverageChange(line, covered - referenceCovered);
                }
            }
        }));
    }

    public void attachCoveragePerLine(final FileCoverageNode node, final CoveragePaint paint) {
        int[] lines = paint.getAllLines();
        SortedMap<Integer, Coverage> coverageDetails = new TreeMap<>();
        for (int line : lines) {
            if (paint.getBranchTotal(line) > 0) {
                int covered = paint.getBranchCoverage(line);
                int missed = paint.getBranchTotal(line) - covered;
                coverageDetails.put(line, new Coverage(paint.getBranchCoverage(line), missed));
            }
            else {
                int covered = paint.getHits(line) > 0 ? 1 : 0;
                coverageDetails.put(line, new Coverage(covered, 1 - covered));
            }
        }
        node.setCoveragePerLine(coverageDetails);
    }
}
