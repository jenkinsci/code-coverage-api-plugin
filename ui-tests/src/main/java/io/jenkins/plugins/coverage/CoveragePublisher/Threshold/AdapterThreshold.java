package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;

/**
 * Threshold used in {@link Adapter} in {@CoveragePublisher}.
 */
public class AdapterThreshold extends AbstractThreshold {

    /**
     * Constructor of a Threshold used in {@link Adapter} in {@CoveragePublisher}.
     *
     * @param adapter
     *         of threshold
     * @param path
     *         to threshold
     */
    public AdapterThreshold(Adapter adapter, String path) {
        super(adapter, path);
    }

    /**
     * Setter for target of Threshold using {@link AdapterThresholdTarget}.
     *
     * @param adapterThresholdTarget
     *         of threshold
     */
    public void setThresholdTarget(AdapterThresholdTarget adapterThresholdTarget) {
        this.thresholdTarget.select(adapterThresholdTarget.getValue());
    }


    /**
     * Enum for Options of {@link AdapterThreshold}.
     */
    public enum AdapterThresholdTarget {
        AGGREGATED_REPORT("Aggregated Report"),
        REPORT("Report"),
        GROUP("Group"),
        PACKAGE("Package"),
        FILE("File"),
        CLASS("Class"),
        METHOD("Method"),
        INSTRUCTION("Instruction"),
        LINE("Line"),
        CONDITIONAL("Conditional"),
        ;

        private final String value;

        /**
         * Constructor of enum.
         *
         * @param value
         *         is value-attribute of option-tag.
         */
        AdapterThresholdTarget(String value) {
            this.value = value;
        }

        /**
         * Get value of option-tag which should be selected.
         *
         * @return value of option-tag to select.
         */
        public String getValue() {
            return value;
        }
    }
}


