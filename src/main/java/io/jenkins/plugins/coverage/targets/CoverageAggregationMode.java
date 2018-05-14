/*
 * The MIT License
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


/**
 * Different ways of aggregating data series {x_1,x_2,x_3,...}, which can be represented as
 * <tt>f(...f(f(ZERO,x_1),x_2)...,x_n)</tt>
 *
 * @author Stephen Connolly
 * @since 22-Aug-2007 18:07:35
 */
public enum CoverageAggregationMode {
    /**
     * Aggregation that doesn't produce any value at all.
     */
    NONE(null) {
        public Ratio aggregate(Ratio a, Ratio b) {
            return null;
        }
    },

    /**
     * Adds up numerator and denominator separately.
     *
     * Say if you want to count the ratio of male among the population in a state from
     * a series of those ratios per county, this is how you add them up.
     */
    SUM(Ratio.create(0,0)) {
        public Ratio aggregate(Ratio a, Ratio b) {
            return Ratio.create(a.numerator + b.numerator, a.denominator + b.denominator);
        }
    },

    /**
     * x_1 * x_2 * x_3 + ...
     */
    PRODUCT(Ratio.create(1,1)) {
        public Ratio aggregate(Ratio a, Ratio b) {
            return Ratio.create(a.numerator * b.numerator, a.denominator * b.denominator);
        }
    },

    /**
     * Treat (0/0) as "no data", then compute "# of non-zero data/# of data."
     */
    COUNT_NON_ZERO(Ratio.create(0,0)) {
        public Ratio aggregate(Ratio a, Ratio b) {
            if (Math.abs(b.denominator) < 1e-7)
                return a;       // 0/0 is treated as "no data"
            return Ratio.create(a.numerator + (Math.abs(b.numerator) > 1e-7 ? 1:0),    a.denominator + 1);
        }};

    /**
     * Initial value of this aggregation mode, which is the output of the aggregation when
     * the data series is empty.
     */
    public final Ratio ZERO;

    CoverageAggregationMode(Ratio ZERO) {
        this.ZERO = ZERO;
    }

    /**
     * Combinator function. Note that this function is defined to be left-associative and f(x,y) isn't necessarily
     * the same as f(y,x)
     *
     * @param a the first ratio
     * @param b the second ratio
     * @return Combined ratio
     */
    public abstract Ratio aggregate(Ratio a, Ratio b);
}
