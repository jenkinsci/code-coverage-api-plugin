package io.jenkins.plugins.coverage.targets;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.util.List;

@ExportedBean
public class CoverageTrend implements Serializable {

    private String buildName;
    private List<CoverageTreeElement> elements;


    public CoverageTrend(String buildName, List<CoverageTreeElement> elements) {
        this.buildName = buildName;
        this.elements = elements;
    }

    @Exported
    public String getBuildName() {
        return buildName;
    }

    @Exported
    public List<CoverageTreeElement> getElements() {
        return elements;
    }

}
