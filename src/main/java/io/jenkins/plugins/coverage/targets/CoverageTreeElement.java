/*
 * Copyright (c) 2007-2018 Seiji Sogabe, podarsmarty, Michael Barrientos and Jenkins contributors
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

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/
@ExportedBean(defaultVisibility = 2)
public class CoverageTreeElement implements Serializable {

    /**
     * Generated
     */
    private static final long serialVersionUID = 498666415572813346L;

    private Ratio ratio;

    private CoverageElement element;

    public CoverageTreeElement(CoverageElement element, Ratio ratio) {
        this.element = element;
        this.ratio = ratio;
    }

    @Exported
    public String getName() {
        return element.getName();
    }

    @Exported
    public float getRatio() {
        return ratio.getPercentageFloat();
    }

    @Exported
    public float getNumerator() {
        return ratio.numerator;
    }

    @Exported
    public float getDenominator() {
        return ratio.denominator;
    }
}
