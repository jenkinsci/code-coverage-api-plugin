/*
 * Copyright (c) 2007-2018 Stephen Connolly, manolo, Michael Barrientos, Shenyu Zheng and Jenkins contributors
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

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/

/**
 * Type of program construct being covered.
 *
 * @author Stephen Connolly
 * @author manolo
 * @since 22-Aug-2007 18:36:01
 */
public enum CoverageElement {
    AGGREGATED_REPORT("Aggregated Report"),
    REPORT("Report", AGGREGATED_REPORT),
    JAVA_GROUP("Group", REPORT),
    JAVA_PACKAGE("Package", JAVA_GROUP),
    JAVA_FILE("File", JAVA_PACKAGE),
    JAVA_CLASS("Class", JAVA_FILE),
    JAVA_METHOD("Method", JAVA_CLASS);

    private final CoverageElement parent;
    private final String name;

    CoverageElement(String name) {
        this.parent = null;
        this.name = name;
    }

    CoverageElement(String name, CoverageElement parent) {
        this.parent = parent;
        this.name = name;
    }


    /**
     * Getter for property 'parent'.
     *
     * @return Value for property 'parent'.
     */
    public CoverageElement getParent() {
        return parent;
    }

    /**
     * Return displayName of this coverage element.
     * <p>
     * Note: This getter has to be evaluated each time in a non static
     * way because the user could change its language
     *
     * @return Value for property 'displayName'.
     */
    public String getDisplayName() {
        return name;
    }
}
