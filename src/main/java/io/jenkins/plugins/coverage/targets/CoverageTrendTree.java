package io.jenkins.plugins.coverage.targets;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@ExportedBean
public class CoverageTrendTree {

    private String name;
    private List<CoverageTrend> trends;
    private Map<String, CoverageResult> children;

    public CoverageTrendTree(String name, List<CoverageTrend> trends, Map<String, CoverageResult> children) {
        this.name = name;
        this.trends = trends;
        this.children = children;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public List<CoverageTrend> getTrends() {
        return trends;
    }

    @Exported
    public List<CoverageTrendTree> getChildren() {
        List<CoverageTrendTree> childrenTrends = new LinkedList<>();

        for (Map.Entry<String, CoverageResult> e : children.entrySet()) {
            childrenTrends.add(new CoverageTrendTree(e.getKey(), e.getValue().getCoverageTrends(), e.getValue().getChildrenReal()));
        }
        return childrenTrends;
    }
}
