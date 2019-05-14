package io.jenkins.plugins.coverage.targets;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

/**
 *
 */
@ExportedBean()
public class CoverageRelativeResultElement implements Serializable {

    private static final long serialVersionUID = 4649039844700570525L;

    private String filePath;
    private Ratio ratio;

    public CoverageRelativeResultElement(String filePath, Ratio ratio) {
        this.filePath = filePath;
        this.ratio = ratio;
    }

    @Exported
    public String getFilePath() {
        return filePath;
    }

    public Ratio getRatioBean() {
        return ratio;
    }

    @Exported
    public float getRatio() {
        return ratio.getPercentageFloat();
    }
}
