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


import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import static io.jenkins.plugins.coverage.targets.CoverageAggregationMode.COUNT_NON_ZERO;
import static io.jenkins.plugins.coverage.targets.CoverageAggregationMode.SUM;
import static io.jenkins.plugins.coverage.targets.CoverageElement.*;
import static io.jenkins.plugins.coverage.targets.CoverageMetric.*;


/**
 * Rules that determines how coverage ratio of children are aggregated into that of the parent.
 *
 * @author Stephen Connolly
 * @since 22-Aug-2007 18:08:46
 */
public class CoverageAggregationRule implements Serializable {

    private static final long serialVersionUID = 3610276359557022488L;

    private final CoverageElement source;

    private final CoverageMetric input;

    private final CoverageAggregationMode mode;

    private final CoverageMetric output;

    public CoverageAggregationRule(CoverageElement source,
                                   CoverageMetric input,
                                   CoverageAggregationMode mode,
                                   CoverageMetric output) {
        this.mode = mode;
        this.input = input;
        this.source = source;
        this.output = output;
    }

    public static Map<CoverageMetric, Ratio> aggregate(CoverageElement source,
                                                       CoverageMetric input,
                                                       Ratio inputResult,
                                                       Map<CoverageMetric, Ratio> runningTotal) {
        Map<CoverageMetric, Ratio> result = new EnumMap<CoverageMetric, Ratio>(CoverageMetric.class);
        result.putAll(runningTotal);
        for (CoverageAggregationRule rule : INITIAL_RULESET) {
            if (rule.source == source && rule.input == input) {
                Ratio prevTotal = result.get(rule.output);
                if (prevTotal == null) {
                    prevTotal = rule.mode.ZERO;
                }

                result.put(rule.output, rule.mode.aggregate(prevTotal, inputResult));
            }
        }
        return result;
    }

    // read (a,b,c,d) as "b metric of a is aggregated into d metric of the parent by using method c."
    // for example, line coverage of a Java method is SUMed up to the line coverage of a Java class (its parent) (1st line),
    // the method coverage of a Java class is # of methods that have some coverage among # of methods that have any code (3rd line.)
    // and so on.
    private static final CoverageAggregationRule INITIAL_RULESET[] = {
            new CoverageAggregationRule(JAVA_METHOD, LINE, SUM, LINE),
            new CoverageAggregationRule(JAVA_METHOD, CONDITIONAL, SUM, CONDITIONAL),
            new CoverageAggregationRule(JAVA_METHOD, LINE, COUNT_NON_ZERO, METHOD),
            new CoverageAggregationRule(JAVA_CLASS, LINE, SUM, LINE),
            new CoverageAggregationRule(JAVA_CLASS, CONDITIONAL, SUM, CONDITIONAL),
            new CoverageAggregationRule(JAVA_CLASS, METHOD, SUM, METHOD),
            new CoverageAggregationRule(JAVA_CLASS, LINE, COUNT_NON_ZERO, CLASSES),
            new CoverageAggregationRule(JAVA_FILE, LINE, SUM, LINE),
            new CoverageAggregationRule(JAVA_FILE, CONDITIONAL, SUM, CONDITIONAL),
            new CoverageAggregationRule(JAVA_FILE, METHOD, SUM, METHOD),
            new CoverageAggregationRule(JAVA_FILE, CLASSES, SUM, CLASSES),
            new CoverageAggregationRule(JAVA_FILE, LINE, COUNT_NON_ZERO, FILES),
            new CoverageAggregationRule(JAVA_PACKAGE, LINE, SUM, LINE),
            new CoverageAggregationRule(JAVA_PACKAGE, CONDITIONAL, SUM, CONDITIONAL),
            new CoverageAggregationRule(JAVA_PACKAGE, METHOD, SUM, METHOD),
            new CoverageAggregationRule(JAVA_PACKAGE, CLASSES, SUM, CLASSES),
            new CoverageAggregationRule(JAVA_PACKAGE, FILES, SUM, FILES),
            new CoverageAggregationRule(JAVA_PACKAGE, LINE, COUNT_NON_ZERO, PACKAGES),
            new CoverageAggregationRule(JAVA_GROUP, LINE, SUM, LINE),
            new CoverageAggregationRule(JAVA_GROUP, METHOD, SUM, METHOD),
            new CoverageAggregationRule(JAVA_GROUP, CLASSES, SUM, CLASSES),
            new CoverageAggregationRule(JAVA_GROUP, FILES, SUM, FILES),
            new CoverageAggregationRule(JAVA_GROUP, PACKAGES, SUM, PACKAGES),
            new CoverageAggregationRule(JAVA_GROUP, LINE, COUNT_NON_ZERO, GROUPS),
            new CoverageAggregationRule(REPORT, LINE, SUM, LINE),
            new CoverageAggregationRule(REPORT, METHOD, SUM, METHOD),
            new CoverageAggregationRule(REPORT, CLASSES, SUM, CLASSES),
            new CoverageAggregationRule(REPORT, FILES, SUM, FILES),
            new CoverageAggregationRule(REPORT, PACKAGES, SUM, PACKAGES),
            new CoverageAggregationRule(REPORT, GROUPS, SUM, GROUPS),
            new CoverageAggregationRule(REPORT, LINE, COUNT_NON_ZERO, REPORTS),
    };

    public static Ratio combine(CoverageMetric metric, Ratio existingResult, Ratio additionalResult) {
        return Ratio.create(existingResult.numerator + additionalResult.numerator, existingResult.denominator + additionalResult.denominator);
    }
}
