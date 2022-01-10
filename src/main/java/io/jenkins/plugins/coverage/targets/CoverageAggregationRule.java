package io.jenkins.plugins.coverage.targets;


import java.math.BigDecimal;
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
            prevTotal = Ratio.create(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Ratio r = Ratio.create(inputResult.numerator.add(prevTotal.numerator), inputResult.denominator.add(prevTotal.denominator));
        result.put(input, r);

        return result;
    }


    public static Ratio combine(CoverageElement element, Ratio existingResult, Ratio additionalResult) {
        return Ratio.create(existingResult.numerator.add( additionalResult.numerator), existingResult.denominator.add(additionalResult.denominator));
    }
}
