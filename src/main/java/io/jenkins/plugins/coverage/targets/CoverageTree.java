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
import java.util.Map;
import java.util.Map.Entry;

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/
@ExportedBean
public class CoverageTree implements Serializable {

    /**
     * Generated
     */
    private static final long serialVersionUID = 5112467356061418891L;

    private Map<CoverageElement, Ratio> aggregateResults;

    private Map<String, CoverageResult> children;


    private String name;

    public CoverageTree(String name, Map<CoverageElement, Ratio> aggregateResults,
                        Map<String, CoverageResult> children) {
        this.name = name;
        this.aggregateResults = aggregateResults;
        this.children = children;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public CoverageTreeElement[] getElements() {
        CoverageTreeElement[] cte = new CoverageTreeElement[aggregateResults.size()];
        int current = 0;
        for (Entry<CoverageElement, Ratio> e : aggregateResults.entrySet()) {
            cte[current] = new CoverageTreeElement(e.getKey(), e.getValue());
            current++;
        }
        return cte;
    }

    @Exported
    public CoverageTree[] getChildren() {
        CoverageTree[] ct = new CoverageTree[children.size()];
        int current = 0;
        for (Entry<String, CoverageResult> e : children.entrySet()) {
            ct[current] = new CoverageTree(e.getKey(), e.getValue().getResults(), e.getValue().getChildrenReal());
            current++;
        }
        return ct;
    }
}
