/*
 * Copyright (c) 2007-2018 Stephen Connolly, Stephen Connolly and Jenkins contributors
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

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/

/**
 * Describes how {@link CoveragePaint} can be aggregated up a {@link CoverageResult} tree.
 *
 * @author Stephen Connolly
 * @since 29-Aug-2007 18:13:22
 */
public class CoveragePaintRule implements Serializable {

    private static final long serialVersionUID = 1039455096344866574L;

    private final CoverageElement element;

    private final CoverageAggregationMode mode;

    public CoveragePaintRule(CoverageElement element, CoverageAggregationMode mode) {
        this.element = element;
        this.mode = mode;
    }

    private static final CoveragePaintRule[] INITIAL_RULESET = {
            new CoveragePaintRule(CoverageElement.JAVA_METHOD, CoverageAggregationMode.NONE),
            new CoveragePaintRule(CoverageElement.JAVA_CLASS, CoverageAggregationMode.SUM),};

    public static CoveragePaint makePaint(CoverageElement element) {
        for (CoveragePaintRule rule : INITIAL_RULESET) {
            if (element == rule.element
                    || (element == rule.element.getParent() && CoverageAggregationMode.NONE != rule.mode)) {
                return new CoveragePaint(element);
            }
        }
        return null;
    }

    public static boolean propagatePaintToParent(CoverageElement element) {
        for (CoveragePaintRule rule : INITIAL_RULESET) {
            if (element == rule.element) {
                return CoverageAggregationMode.NONE != rule.mode;
            }
        }
        return false;
    }
}
