package io.jenkins.plugins.coverage.adapter;


import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;

import javax.annotation.Nonnull;

public class CoverageReportAdapterDescriptor<T extends CoverageReportAdapter>
        extends Descriptor<CoverageReportAdapter> {

    private String toolName;

    public CoverageReportAdapterDescriptor(Class<? extends CoverageReportAdapter> clazz, String toolName) {
        super(clazz);
        this.toolName = toolName;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        return toolName;
    }

    public static DescriptorExtensionList<CoverageReportAdapter, CoverageReportAdapterDescriptor<?>> all() {
        return Jenkins.getInstance().getDescriptorList(CoverageReportAdapter.class);
    }
}
