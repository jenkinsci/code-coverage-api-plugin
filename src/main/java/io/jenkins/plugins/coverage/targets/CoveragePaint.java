/*
 * Copyright (c) 2007-2018 Stephen Connolly, Jesse Glick, Kohsuke Kawaguchi, Seiji Sogabe and Jenkins contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.coverage.targets;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/

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
    public Map<CoverageElement, Ratio> getResults() {
        Map<CoverageElement, Ratio> result = new HashMap<>();
        result.put(CoverageElement.LINE, getLineCoverage());
        result.put(CoverageElement.CONDITIONAL, getConditionalCoverage());
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
