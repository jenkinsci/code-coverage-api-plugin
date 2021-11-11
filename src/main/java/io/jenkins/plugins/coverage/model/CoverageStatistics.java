package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class that represents a coverage statistic.
 */
public class CoverageStatistics {
    private Map<String, Coverage> sortedCoverages;

    /**
     * Ctor for coverage statistics.
     *
     * @param coverageStatistics The coverage statistics
     */
    public CoverageStatistics(final SortedMap<CoverageMetric, Coverage> coverageStatistics) {
        List<String> coverages = coverageStatistics.keySet().stream()
                .map(CoverageMetric::getName)
                .collect(Collectors.toList());
        sortedCoverages = coverageStatistics.entrySet()
                .stream()
                .filter(x -> coverages.contains(x.getKey().getName()))
                .collect(Collectors.toMap(e -> e.getKey().getName(),
                        Map.Entry::getValue));
    }

    /**
     * Returns the percentage the file coverage.
     *
     * @return the percentage of the file coverage
     */
    public double getFilePercentage() {
        return getPercentage(QualityGate.QualityGateType.FILE.getName());
    }

    /**
     * Returns the percentage the module coverage.
     *
     * @return the percentage of the module coverage
     */
    public double getModulePercentage() {
        return getPercentage(QualityGate.QualityGateType.MODULE.getName());
    }

    /**
     * Returns the percentage the method coverage.
     *
     * @return the percentage of the method coverage
     */
    public double getMethodPercentage() {
        return getPercentage(QualityGate.QualityGateType.METHOD.getName());
    }

    /**
     * Returns the percentage the line coverage.
     *
     * @return the percentage of the line coverage
     */
    public double getLinePercentage() {
        return getPercentage(QualityGate.QualityGateType.LINE.getName());
    }

    /**
     * Returns the percentage the package coverage.
     *
     * @return the percentage of the package coverage
     */
    public double getPackagePercentage() {
        return getPercentage(QualityGate.QualityGateType.PACKAGE.getName());
    }

    /**
     * Returns the percentage the branch coverage.
     *
     * @return the percentage of the branch coverage
     */
    public double getBranchPercentage() {
        return getPercentage(QualityGate.QualityGateType.BRANCH.getName());
    }

    /**
     * Returns the percentage the class coverage.
     *
     * @return the percentage of the class coverage
     */
    public double getClassPercentage() {
        return getPercentage(QualityGate.QualityGateType.CLASS.getName());

    }

    /**
     * Returns the percentage the instruction coverage.
     *
     * @return the percentage of the instruction coverage
     */
    public double getInstructionPercentage() {
        return getPercentage(QualityGate.QualityGateType.INSTRUCTION.getName());
    }

    /**
     * Returns the covered percentage by key.
     *
     * @param key the key
     * @return the covered percentage
     */
    private double getPercentage(final String key) {
        return sortedCoverages.containsKey(key) ?
                sortedCoverages.get(key).getCoveredPercentage() : 0;
    }

    /**
     * Reference for the method to get the covered percentage.
     */
    public enum StatisticProperties {
        FILE(CoverageStatistics::getFilePercentage),
        MODULE(CoverageStatistics::getModulePercentage),
        METHOD(CoverageStatistics::getMethodPercentage),
        LINE(CoverageStatistics::getLinePercentage),
        PACKAGE(CoverageStatistics::getPackagePercentage),
        BRANCH(CoverageStatistics::getBranchPercentage),
        CLASS(CoverageStatistics::getClassPercentage),
        INSTRUCTION(CoverageStatistics::getInstructionPercentage);

        private final SerializableGetter coverageGetter;

        /**
         * Ctor for StatisticProperties.
         *
         * @param coverageGetter The coverage getter
         */
        StatisticProperties(final SerializableGetter coverageGetter) {
            this.coverageGetter = coverageGetter;
        }

        public Function<CoverageStatistics, Double> getCoverageGetter() {
            return coverageGetter;
        }
    }

    /**
     * Interface for SerializableGetter.
     */
    private interface SerializableGetter extends Function<CoverageStatistics, Double>, Serializable {
        // nothing to add
    }
}
