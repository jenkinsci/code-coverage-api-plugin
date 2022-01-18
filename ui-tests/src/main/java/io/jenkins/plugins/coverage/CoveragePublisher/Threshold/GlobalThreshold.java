package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;

/**
 * Global Threshold used in {@link CoveragePublisher}.
 */
public class GlobalThreshold extends AbstractThreshold {
    private CoveragePublisher coveragePublisher;

    /**
     * Constructor of a GlobalThreshold in {@link CoveragePublisher}.
     *
     * @param coveragePublisher
     *         of threshold
     * @param path
     *         to threshold
     */
    public GlobalThreshold(CoveragePublisher coveragePublisher, String path) {
        super(coveragePublisher, path);
        this.coveragePublisher = coveragePublisher;
    }

    /**
     * Setter for target of Threshold using {@link GlobalThresholdTarget}.
     *
     * @param globalThresholdTarget
     *         of threshold
     */
    public void setThresholdTarget(GlobalThresholdTarget globalThresholdTarget) {
        ensureAdvancedOptionsIsActivated();
        this.getThresholdTarget().select(globalThresholdTarget.getValue());

    }

    /**
     * Ensures advanced options are activated so that values can be set.
     */
    @Override
    public void ensureAdvancedOptionsIsActivated() {
        this.coveragePublisher.ensureAdvancedOptionsIsActivated();
    }

    /**
     * Enum for Options of {@link GlobalThreshold}.
     */
    public enum GlobalThresholdTarget {
        AGGREGATED_REPORT("Aggregated Report"),
        REPORT("Report"),
        GROUP("Group"),
        PACKAGE("Package"),
        DIRECTORY("Directory"),
        FILE("File"),
        CLASS("Class"),
        METHOD("Method"),
        FUNCTION("Function"),
        INSTRUCTION("Instruction"),
        LINE("Line"),
        CONDITIONAL("Conditional");

        private final String value;

        /**
         * Constructor of enum.
         * @param value is value-attribute of option-tag.
         */
        GlobalThresholdTarget(String value) {
            this.value = value;
        }

        /**
         * Get value of option-tag which should be selected.
         * @return value of option-tag to select.
         */
        public String getValue() {
            return value;
        }
    }
}

