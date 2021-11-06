
package io.jenkins.plugins.coverage;

import org.springframework.jmx.support.MetricType;

import io.jenkins.plugins.coverage.model.CoverageMetric;

public enum QualityGateType {
    MODULE,
    REPORT,
    PACKAGE,
    FILE,
    CLASS,
    METHOD,
    INSTRUCTION,
    LINE,
    BRANCH,
}
