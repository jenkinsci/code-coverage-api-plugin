package io.jenkins.plugins.coverage.model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CoverageStatistics {
    private Map<String, Coverage> sortedCoverages;

    public CoverageStatistics(SortedMap<CoverageMetric, Coverage> coverageStatistics) {
        List<String> coverages = coverageStatistics.keySet().stream()
                .map(CoverageMetric::getName)
                .collect(Collectors.toList());
        sortedCoverages = coverageStatistics.entrySet()
                .stream()
                .filter(x -> coverages.contains(x.getKey().getName()))
                .collect(Collectors.toMap(e -> e.getKey().getName(),
                        Map.Entry::getValue));
    }

    public double getFilePercentage() {
        return getPercentage(QualityGate.QualityGateType.FILE.getName());
    }

    public double getModulePercentage() {
        return getPercentage(QualityGate.QualityGateType.MODULE.getName());
    }

    public double getMethodPercentage() {
        return getPercentage(QualityGate.QualityGateType.METHOD.getName());
    }

    public double getLinePercentage() {
        return getPercentage(QualityGate.QualityGateType.LINE.getName());
    }

    public double getPackagePercentage() {
        return getPercentage(QualityGate.QualityGateType.PACKAGE.getName());
    }

    public double getBranchPercentage() {
        return getPercentage(QualityGate.QualityGateType.BRANCH.getName());
    }

    public double getClassPercentage() {
        return getPercentage(QualityGate.QualityGateType.CLASS.getName());

    }

    public double getInstructionPercentage() {
        return getPercentage(QualityGate.QualityGateType.INSTRUCTION.getName());
    }

    private double getPercentage(String key) {
        return sortedCoverages.containsKey(key) ?
                sortedCoverages.get(key).getCoveredPercentage() : 0;
    }

    public enum StatisticProperties {
        FILE(CoverageStatistics::getFilePercentage),
        MODULE(CoverageStatistics::getModulePercentage),
        METHOD(CoverageStatistics::getMethodPercentage),
        LINE(CoverageStatistics::getLinePercentage),
        PACKAGE(CoverageStatistics::getPackagePercentage),
        BRANCH(CoverageStatistics::getBranchPercentage),
        CLASS(CoverageStatistics::getClassPercentage),
        INSTRUCTION(CoverageStatistics::getInstructionPercentage);

        private final SerializableGetter sizeGetter;

        StatisticProperties(SerializableGetter sizeGetter) {
            this.sizeGetter = sizeGetter;
        }

        public Function<CoverageStatistics, Double> getSizeGetter() {
            return sizeGetter;
        }
    }

    private interface SerializableGetter extends Function<CoverageStatistics, Double>, Serializable {
        // nothing to add
    }
}
