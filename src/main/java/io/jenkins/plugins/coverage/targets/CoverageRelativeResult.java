package io.jenkins.plugins.coverage.targets;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.util.List;

/**
 * Compare with previous not failed jenkins build's report.
 * output the relative code coverage result.
 */
@ExportedBean()
public class CoverageRelativeResult implements Serializable {

    private static final long serialVersionUID = 4649039844700570525L;

    private String name;
    private List<CoverageRelativeResultElement> elements;

    public CoverageRelativeResult(String name, List<CoverageRelativeResultElement> elements) {
        this.name = name;
        this.elements = elements;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public List<CoverageRelativeResultElement> getElements() {
        return elements;
    }
}
