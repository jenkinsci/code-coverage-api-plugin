package io.jenkins.plugins.coverage.targets;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;

@ExportedBean
public class CoverageTrend implements Serializable {

    private String buildName;
    private CoverageTreeElement[] elements;


    public CoverageTrend(String buildName, CoverageTreeElement[] elements) {
        this.buildName = buildName;
        this.elements = elements;
    }

    @Exported
    public String getBuildName() {
        return buildName;
    }

    @Exported
    public CoverageTreeElement[] getElements() {
        return elements;
    }

}
