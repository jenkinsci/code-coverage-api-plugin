package io.jenkins.plugins.coverage.targets;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.jenkins.plugins.coverage.Ratio;


import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Line-by-line coverage information.
 *
 * @author Stephen Connolly
 * @since 29-Aug-2007 17:44:29
 */
public class CoveragePaint implements Serializable {

    /**
     * Generated
     */
    private static final long serialVersionUID = -6265259191856193735L;

    private static final CoveragePaintDetails[] EMPTY = new CoveragePaintDetails[0];

    private static class CoveragePaintDetails implements Serializable {

        /**
         * Generated
         */
        private static final long serialVersionUID = -9097537016381444671L;

        /**
         * Fly-weight object pool of (n,0,0) instances, which are very common.
         */
        private static final CoveragePaintDetails[] CONSTANTS = new CoveragePaintDetails[128];

        /**
         * Number of times this line is executed.
         */
        final int hitCount;

        static CoveragePaintDetails create(int hitCount, int branchCount, int branchCoverage) {
            if (branchCount == 0 && branchCoverage == 0) {
                if (0 <= hitCount && hitCount < CONSTANTS.length) {
                    CoveragePaintDetails r = CONSTANTS[hitCount];
                    if (r == null) {
                        CONSTANTS[hitCount] = r = new CoveragePaintDetails(hitCount);
                    }
                    return r;
                }
                return new CoveragePaintDetails(hitCount);
            } else {
                return new BranchingCoveragePaintDetails(hitCount, branchCount, branchCoverage);
            }
        }

        private CoveragePaintDetails(int hitCount) {
            this.hitCount = hitCount;
        }

        int branchCount() {
            return 0;
        }

        int branchCoverage() {
            return 0;
        }

        /**
         * Do 'this+that' and return the new object.
         */
        CoveragePaintDetails add(CoveragePaintDetails that) {
            return CoveragePaintDetails.create(
                    this.hitCount + that.hitCount,
                    // TODO find a better algorithm
                    Math.max(this.branchCount(), that.branchCount()),
                    Math.max(this.branchCoverage(), that.branchCoverage()));
        }
    }

    /**
     * {@link CoveragePaintDetails} that has non-zero branch coverage numbers.
     * This is relatively rare, so we use two classes to save 8 bytes of storing 0.
     */
    private static class BranchingCoveragePaintDetails extends CoveragePaintDetails {

        final int branchCount;

        final int branchCoverage;

        private BranchingCoveragePaintDetails(int hitCount, int branchCount, int branchCoverage) {
            super(hitCount);
            this.branchCount = branchCount;
            this.branchCoverage = branchCoverage;
        }

        @Override
        int branchCount() {
            return branchCount;
        }

        @Override
        int branchCoverage() {
            return branchCoverage;
        }

        private static final long serialVersionUID = 1L;
    }

    protected TIntObjectMap<CoveragePaintDetails> lines = new TIntObjectHashMap<CoveragePaintDetails>();

    private int totalLines = 0;

    public CoveragePaint(CoverageElement source) {
//		there were no getters against the source ...
//      this.source = source;
    }

    private void paint(int line, CoveragePaintDetails delta) {
        CoveragePaintDetails d = lines.get(line);
        if (d == null) {
            lines.put(line, delta);
        } else {
            lines.put(line, d.add(delta));
        }
    }

    public void paint(int line, int hits) {
        paint(line, CoveragePaintDetails.create(hits, 0, 0));
    }

    public void paint(int line, int hits, int branchCover, int branchCount) {
        paint(line, CoveragePaintDetails.create(hits, branchCount, branchCover));
    }

    public void add(CoveragePaint child) {
        TIntObjectIterator<CoveragePaintDetails> it = child.lines.iterator();
        while (it.hasNext()) {
            it.advance();
            paint(it.key(), it.value());
        }
    }

    /**
     * Setter for the property {@code totalLines}.
     *
     * @param totalLines The total number of lines in this file
     **/
    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    /**
     * Returns the total number of lines in this painted file. Unlike the
     * denominator in the ratio returned by {@link #getLineCoverage()},
     * which only indicates the number of executable lines, this includes
     * even non-executable lines, such as white space, comments, brackets,
     * and more.
     *
     * @return value for the property {@code totalLines}.
     **/
    public int getTotalLines() {
        return totalLines;
    }

    /**
     * Getter for property 'lineCoverage'.
     *
     * @return Value for property 'lineCoverage'.
     */
    public Ratio getLineCoverage() {
        int covered = 0;
        for (CoveragePaintDetails d : lines.values(EMPTY)) {
            if (d.hitCount > 0) {
                covered++;
            }
        }
        return Ratio.create(covered, lines.size());
    }

    /**
     * Getter for property 'conditionalCoverage'.
     *
     * @return Value for property 'conditionalCoverage'.
     */
    public Ratio getConditionalCoverage() {
        long maxTotal = 0;
        long total = 0;
        for (CoveragePaintDetails d : lines.values(EMPTY)) {
            maxTotal += d.branchCount();
            total += d.branchCoverage();
        }
        return Ratio.create(total, maxTotal);
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Map<CoverageMetric, Ratio> getResults() {
        Map<CoverageMetric, Ratio> result = new EnumMap<CoverageMetric, Ratio>(CoverageMetric.class);
        result.put(CoverageMetric.LINE, getLineCoverage());
        result.put(CoverageMetric.CONDITIONAL, getConditionalCoverage());
        return result;
    }

    public boolean isPainted(int line) {
        return lines.get(line) != null;
    }

    public int getHits(int line) {
        CoveragePaintDetails d = lines.get(line);
        if (d == null) {
            return 0;
        } else {
            return d.hitCount;
        }
    }

    public int getBranchTotal(int line) {
        CoveragePaintDetails d = lines.get(line);
        if (d == null) {
            return 0;
        } else {
            return d.branchCount();
        }
    }

    public int getBranchCoverage(int line) {
        CoveragePaintDetails d = lines.get(line);
        if (d == null) {
            return 0;
        } else {
            return d.branchCoverage();
        }
    }
}
