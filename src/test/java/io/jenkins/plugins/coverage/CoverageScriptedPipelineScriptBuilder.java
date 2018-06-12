package io.jenkins.plugins.coverage;

import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.detector.AntPathDetector;
import io.jenkins.plugins.coverage.detector.Detector;
import io.jenkins.plugins.coverage.threshold.Threshold;
import org.jenkinsci.Symbol;

import java.util.LinkedList;
import java.util.List;

public class CoverageScriptedPipelineScriptBuilder {
    private Detector reportDetector = new AntPathDetector("*.xml");

    private List<CoverageReportAdapter> adapters = new LinkedList<>();

    private List<Threshold> globalThresholds = new LinkedList<>();

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private CoverageScriptedPipelineScriptBuilder() {
    }


    public static CoverageScriptedPipelineScriptBuilder builder() {
        return new CoverageScriptedPipelineScriptBuilder();
    }

    public Detector getReportDetector() {
        return reportDetector;
    }

    public CoverageScriptedPipelineScriptBuilder setReportDetector(Detector reportDetector) {
        this.reportDetector = reportDetector;
        return this;
    }

    public CoverageScriptedPipelineScriptBuilder addAdapter(CoverageReportAdapter adapter) {
        adapters.add(adapter);
        return this;
    }

    public CoverageScriptedPipelineScriptBuilder addGlobalThreshold(Threshold threshold) {
        globalThresholds.add(threshold);
        return this;
    }


    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("node {")
                .append("publishCoverage(");

        sb.append("reportDetector:").append(generateSnippetForDetector(reportDetector));

                sb.append(",failUnhealthy:").append(failUnhealthy);
        sb.append(",failUnstable:").append(failUnstable);
        sb.append(",failNoReports:").append(failNoReports);

        if (adapters.size() > 0) {
            sb.append(",adapters:[");
            for (int i = 0; i < adapters.size(); i++) {
                if (i > 0) sb.append(",");

                CoverageReportAdapter adapter = adapters.get(i);
                sb.append(generateSnippetForAdapter(adapter));
            }
            sb.append(']');
        }

        sb.append(",globalThresholds: ").append(generateSnippetForThresholds(globalThresholds));

        sb.append(")").append("}");

        return sb.toString();
    }

    private String generateSnippetForDetector(Detector detector) {
        StringBuilder sb = new StringBuilder();

        Descriptor<Detector> d = detector.getDescriptor();
        Class c = d.getClass();

        if (c.isAnnotationPresent(Symbol.class)) {
            Symbol s = (Symbol) c.getAnnotation(Symbol.class);
            String[] symbolValues = s.value();

            if (symbolValues.length > 0) {
                String symbol = symbolValues[0];
                sb.append(symbol).append("(");

                if (detector instanceof AntPathDetector) {
                    return sb.append("path:'").append(((AntPathDetector) detector).getPath()).append("')").toString();
                }

            }
        }

        return "";
    }

    private String generateSnippetForAdapter(CoverageReportAdapter adapter) {
        Descriptor<CoverageReportAdapter> d = adapter.getDescriptor();
        Class c = d.getClass();

        if (c.isAnnotationPresent(Symbol.class)) {
            Symbol s = (Symbol) c.getAnnotation(Symbol.class);
            String[] symbolValues = s.value();

            if (symbolValues.length > 0) {
                String symbol = symbolValues[0];

                return symbol + "(path:'" + adapter.getPath()
                        + "', thresholds: " + generateSnippetForThresholds(adapter.getThresholds()) + ")";
            }
        }

        //TODO condition when adapter do not have @Symbol annotation.
        return "";
    }

    private String generateSnippetForThresholds(List<Threshold> thresholds) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (int i = 0; i < thresholds.size(); i++) {
            if (i > 0) sb.append(",");

            Threshold threshold = thresholds.get(i);
            sb.append("[thresholdTarget: '").append(threshold.getThresholdTarget())
                    .append("', unhealthyThreshold: ").append(threshold.getUnhealthyThreshold())
                    .append(", unstableThreshold: ").append(threshold.getUnstableThreshold())
                    .append(", failUnhealthy: ").append(threshold.isFailUnhealthy())
                    .append("]");
        }
        sb.append("]");

        return sb.toString();
    }


    public CoverageScriptedPipelineScriptBuilder setFailUnhealthy(boolean failUnhealthy) {
        this.failUnhealthy = failUnhealthy;
        return this;
    }

    public CoverageScriptedPipelineScriptBuilder setFailUnstable(boolean failUnstable) {
        this.failUnstable = failUnstable;
        return this;
    }

    public CoverageScriptedPipelineScriptBuilder setFailNoReports(boolean failNoReports) {
        this.failNoReports = failNoReports;
        return this;
    }

    @Override
    public String toString() {
        return build();
    }
}
