package io.jenkins.plugins.coverage.targets;

import hudson.model.Api;

public class RestfulCoverageResultWrapper {

    private CoverageResult result;

    public RestfulCoverageResultWrapper(CoverageResult result) {
        this.result = result;
    }


    public Api getResult() {
        return new Api(result);
    }

    public Api getTrend() {
        return new Api(new CoverageTrendTree(result.getName(), result.getCoverageTrends(), result.getChildrenReal()));
    }

    public Object getLast() {
        CoverageResult previousResult = result.getPreviousResult();

        if (previousResult != null)
            return new RestfulCoverageResultWrapper(previousResult);
        else
            return null;
    }
}
