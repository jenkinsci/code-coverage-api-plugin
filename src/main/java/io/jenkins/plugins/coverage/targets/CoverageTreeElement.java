package io.jenkins.plugins.coverage.targets;

import io.jenkins.plugins.coverage.Ratio;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

@ExportedBean
public class CoverageTreeElement implements Serializable {

    /**
     * Generated
     */
    private static final long serialVersionUID = 498666415572813346L;

    private Ratio ratio;

    private CoverageMetric metric;

    public CoverageTreeElement(CoverageMetric metric, Ratio ratio) {
        this.metric = metric;
        this.ratio = ratio;
    }

    @Exported
    public String getName() {
        return metric.getName();
    }

    @Exported
    public float getRatio() {
        return ratio.getPercentageFloat();
    }

    @Exported
    public float getNumerator() {
        return ratio.numerator;
    }

    @Exported
    public float getDenominator() {
        return ratio.denominator;
    }
}
