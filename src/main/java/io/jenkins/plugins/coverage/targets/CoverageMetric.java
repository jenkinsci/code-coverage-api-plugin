/*
 * Copyright (c) 2007-2018 connollys, manolo, Seiji Sogabe, Michael Barrientos, Shenyu Zheng and Jenkins contributors
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
 * @author connollys
 * @author manolo
 * @since 10-Jul-2007 14:59:50
 */
public enum CoverageMetric {
    REPORTS("Reports"),

    JAVA_GROUPS("JAVA Groups"),
    JAVA_PACKAGES("JAVA Packages"),
    JAVA_FILES("JAVA Files"),
    JAVA_CLASSES("JAVA Classes"),
    JAVA_METHODS("JAVA Methods"),

    LLVM_DATALIST("LLVM Data"),
    LLVM_DIRECTORIES("LLVM Directories"),
    LLVM_FILES("LLVM Files"),
    LLVM_FUNCTIONS("LLVM Functions"),

    LINE("Lines"),
    CONDITIONAL("Conditionals");

    private final String name;

    CoverageMetric(String name) {
        this.name = name;
    }

    /**
     * Return the name of this metric element.
     * <p>
     * Note: This getter has to be evaluated each time in a non static
     * way because the user could change its language
     *
     * @return Value for property 'displayName'.
     */
    public String getName() {
        return name;
    }

    public static CoverageMetric[] all() {
        return new CoverageMetric[]{
                CoverageMetric.JAVA_GROUPS,
                CoverageMetric.JAVA_PACKAGES,
                CoverageMetric.JAVA_FILES,
                CoverageMetric.JAVA_CLASSES,
                CoverageMetric.JAVA_METHODS,
                CoverageMetric.LLVM_DATALIST,
                CoverageMetric.LLVM_DIRECTORIES,
                CoverageMetric.LLVM_FILES,
                CoverageMetric.LLVM_FUNCTIONS,
                CoverageMetric.LINE,
                CoverageMetric.CONDITIONAL};
    }
}

