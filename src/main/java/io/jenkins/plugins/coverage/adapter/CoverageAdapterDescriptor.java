package io.jenkins.plugins.coverage.adapter;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public class CoverageAdapterDescriptor<T extends CoverageAdapter> extends Descriptor<CoverageAdapter> {

    public CoverageAdapterDescriptor(Class<? extends CoverageAdapter> clazz) {
        super(clazz);
    }

    public static DescriptorExtensionList<CoverageAdapter, CoverageAdapterDescriptor<?>> all() {
        return Jenkins.getInstance().getDescriptorList(CoverageAdapter.class);
    }
}
