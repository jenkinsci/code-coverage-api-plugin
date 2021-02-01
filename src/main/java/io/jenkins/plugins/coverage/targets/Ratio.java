/*
 * Copyright (c) 2007-2018 Kohsuke Kawaguchi, Michael Barrientos, Jeff Pearce, Shenyu Zheng and Jenkins contributors
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

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/

/**
 * Represents <code>x/y</code> where x={@link #numerator} and y={@link #denominator}.
 *
 * @author Kohsuke Kawaguchi
 */
final public class Ratio implements Serializable {
    public final float numerator;
    public final float denominator;

    private Ratio(float numerator, float denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Gets "x/y" representation.
     */
    public String toString() {
        return print(numerator) + "/" + print(denominator);
    }

    private String print(float f) {
        int i = (int) f;
        if (i == f)
            return String.valueOf(i);
        else
            return String.valueOf(f);
    }

    /**
     * Gets the percentage in integer.
     * If float percentage is less than 100 and larger than 95.5, then return rounded down value,
     * else return rounded off value
     *
     * @return percentage
     */
    public int getPercentage() {
        float floatPercentage = getPercentageFloat();
        int intPercentage = Math.round(floatPercentage);
        if (intPercentage == 100 && (int) floatPercentage < 100) {
            return (int) floatPercentage;
        } else {
            return intPercentage;
        }
    }

    /**
     * Gets the percentage in float.
     * For exceptional cases of 0/0, return 100% as it corresponds to expected ammout.
     * For error cases of x/0, return 0% as x is unexpected ammout.
     *
     * @return percentage
     */
    public float getPercentageFloat() {
        return denominator == 0 ? (numerator == 0 ? 100.0f : 0.0f) : (100 * numerator / denominator);
    }

    static NumberFormat dataFormat = new DecimalFormat("000.00");
    static NumberFormat roundDownDataFormat;

    static {
        roundDownDataFormat = new DecimalFormat("000.000");
        roundDownDataFormat.setRoundingMode(RoundingMode.DOWN);
    }


    /**
     * Gets the percentage as a formatted string used for sorting the html table.
     * If float percentage is less than 100 and larger than 99.995, then return rounded down value,
     * else return rounded off value.
     *
     * @return percentage
     */
    public String getPercentageString() {
        float floatPercentage = getPercentageFloat();
        if (Float.compare(floatPercentage, 99.995f) >= 0) {
            return roundDownDataFormat.format(floatPercentage);
        } else {
            return dataFormat.format(floatPercentage);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ratio ratio = (Ratio) o;

        return Float.compare(ratio.denominator, denominator) == 0
                && Float.compare(ratio.numerator, numerator) == 0;

    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int result;
        result = numerator != +0.0f ? Float.floatToIntBits(numerator) : 0;
        result = 31 * result + denominator != +0.0f ? Float.floatToIntBits(denominator) : 0;
        return result;
    }

    private static final long serialVersionUID = 1L;

    //
    // fly-weight patterns for common Ratio instances (x/y) where x<y
    // and x,y are integers.
    //
    private static final Ratio[] COMMON_INSTANCES = new Ratio[256];

    /**
     * Creates a new instance of {@link Ratio}.
     *
     * @param x numerator
     * @param y denominator
     * @return the ratio
     */
    public static Ratio create(float x, float y) {
        // TODO COMMON_INSTANCES seems broken and return the wrong cached ratio, need to be fix.
//        int xx = (int) x;
//        int yy = (int) y;
//
//        if (xx == x && yy == y) {
//            int idx = yy * (yy + 1) / 2 + xx;
//            if (0 <= idx && idx < COMMON_INSTANCES.length) {
//                Ratio r = COMMON_INSTANCES[idx];
//                if (r == null)
//                    COMMON_INSTANCES[idx] = r = new Ratio(x, y);
//                return r;
//            }
//        }

        return new Ratio(x, y);
    }
}
