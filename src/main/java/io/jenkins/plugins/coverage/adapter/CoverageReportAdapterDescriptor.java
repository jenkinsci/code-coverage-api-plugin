package io.jenkins.plugins.coverage.adapter;


public class CoverageReportAdapterDescriptor<T extends CoverageReportAdapter>
        extends CoverageAdapterDescriptor<CoverageReportAdapter> {


    public CoverageReportAdapterDescriptor(Class<? extends CoverageReportAdapter> clazz) {
        super(clazz);
    }
}
