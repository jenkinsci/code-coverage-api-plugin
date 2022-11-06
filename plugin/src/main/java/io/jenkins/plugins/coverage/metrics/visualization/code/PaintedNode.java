package io.jenkins.plugins.coverage.metrics.visualization.code;

import java.io.Serializable;
import java.util.Arrays;

import edu.hm.hafner.metric.FileNode;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
class PaintedNode implements Serializable {
    private static final long serialVersionUID = -6044649044983631852L;
    private final String path;
    private final int[] linesToPaint;
    private final int[] coveredPerLine;
    private final int[] missedPerLine;

    public PaintedNode(final FileNode file) {
        path = file.getPath();
        linesToPaint = file.getCoveredLines().stream().mapToInt(i -> i).toArray();
        coveredPerLine = file.getCoveredCounters();
        missedPerLine = file.getMissedCounters();
    }

    public String getPath() {
        return path;
    }

    public boolean isPainted(final int line) {
        var index = findLine(line);
        if (index >= 0) {
            return coveredPerLine[index] > 0;
        }
        return false;
    }

    private int findLine(final int line) {
        return Arrays.binarySearch(linesToPaint, line);
    }

    public int getCovered(final int line) {
        return getCounter(line, coveredPerLine);
    }

    public int getMissed(final int line) {
        return getCounter(line, missedPerLine);
    }

    private int getCounter(final int line, final int[] counters) {
        var index = findLine(line);
        if (index >= 0) {
            return counters[index];
        }
        return 0;
    }
}
