package io.jenkins.plugins.coverage.targets;

import io.jenkins.plugins.coverage.Ratio;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

@ExportedBean
public class CoverageTree implements Serializable {

    /**
     * Generated
     */
    private static final long serialVersionUID = 5112467356061418891L;

    private Map<CoverageMetric, Ratio> aggregateResults;

    private Map<String, CoverageResult> children;

    private String name;

    public CoverageTree(String name, Map<CoverageMetric, Ratio> aggregateResults,
                        Map<String, CoverageResult> children) {
        this.name = name;
        this.aggregateResults = aggregateResults;
        this.children = children;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public CoverageTreeElement[] getElements() {
        CoverageTreeElement[] cte = new CoverageTreeElement[aggregateResults.size()];
        int current = 0;
        for (Entry<CoverageMetric, Ratio> e : aggregateResults.entrySet()) {
            cte[current] = new CoverageTreeElement(e.getKey(), e.getValue());
            current++;
        }
        return cte;
    }

    @Exported
    public CoverageTree[] getChildren() {
        CoverageTree[] ct = new CoverageTree[children.size()];
        int current = 0;
        for (Entry<String, CoverageResult> e : children.entrySet()) {
            ct[current] = new CoverageTree(e.getKey(), e.getValue().getResults(), e.getValue().getChildrenReal());
            current++;
        }
        return ct;
    }
}
