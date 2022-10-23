package io.jenkins.plugins.coverage.model.visualization.code;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import edu.hm.hafner.metric.FileNode;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
class PaintedNode implements Serializable {
    private static final long serialVersionUID = -6044649044983631852L;
    private final String path;
    private final SortedSet<Integer> coveredLines;
    private final Map<Integer, Integer> coveredPerLine;
    private final Map<Integer, Integer> missedPerLine;

    public PaintedNode(final FileNode file) {
        path = file.getPath();
        coveredLines = file.getCoveredLines();
        coveredPerLine = new HashMap<>();
        missedPerLine = new HashMap<>();
        for (Integer line : coveredLines) {
            var coverage = file.getBranchCoverage(line);
            if (!coverage.isSet()) {
                coverage = file.getLineCoverage(line);
            }
            coveredPerLine.put(line, coverage.getCovered());
            missedPerLine.put(line, coverage.getMissed());
        }
    }

    public String getPath() {
        return path;
    }

    public boolean isPainted(final int line) {
        return coveredLines.contains(line);
    }

    public int getCovered(final int line) {
        return coveredPerLine.getOrDefault(line, 0);
    }

    public int getMissed(final int line) {
        return missedPerLine.getOrDefault(line, 0);
    }
}
