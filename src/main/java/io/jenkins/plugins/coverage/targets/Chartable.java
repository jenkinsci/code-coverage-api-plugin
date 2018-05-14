package io.jenkins.plugins.coverage;

import hudson.model.Run;
import io.jenkins.plugins.coverage.targets.CoverageMetric;


import java.util.Map;

public interface Chartable
{

	Chartable getPreviousResult();

	Map<CoverageMetric, Ratio> getResults();

	Run<?, ?> getOwner();

}
