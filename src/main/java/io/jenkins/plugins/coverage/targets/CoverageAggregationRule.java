package io.jenkins.plugins.coverage.targets;


import java.util.Map;
import java.util.TreeMap;


public class CoverageAggregationRule {

    public static Map<CoverageElement, Ratio> aggregate(CoverageElement source,
                                                        CoverageElement input,
                                                        Ratio inputResult,
                                                        Map<CoverageElement, Ratio> runningTotal) {
        Map<CoverageElement, Ratio> result = new TreeMap<>(runningTotal);

        Ratio prevTotal = result.get(input);
        if (prevTotal == null) {
            prevTotal = Ratio.create(0, 0);
        }

        Ratio r = Ratio.create(inputResult.numerator + prevTotal.numerator, inputResult.denominator + prevTotal.denominator);
        result.put(input, r);

        if (input.equals(CoverageElement.LINE)) {
            prevTotal = result.get(source);
            if (prevTotal == null) {
                prevTotal = Ratio.create(0, 0);
            }

            r = Ratio.create(prevTotal.numerator + (Math.abs(inputResult.numerator) > 1e-7 ? 1 : 0), prevTotal.denominator + 1);
            result.put(source, r);

        }

        return result;
    }


    public static Ratio combine(CoverageElement element, Ratio existingResult, Ratio additionalResult) {
        return Ratio.create(existingResult.numerator + additionalResult.numerator, existingResult.denominator + additionalResult.denominator);
    }
}
