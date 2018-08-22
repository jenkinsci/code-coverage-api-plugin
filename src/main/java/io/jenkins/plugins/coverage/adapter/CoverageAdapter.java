package io.jenkins.plugins.coverage.adapter;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.Serializable;

public abstract class CoverageAdapter implements ExtensionPoint, Describable<CoverageAdapter>, Serializable {


    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<CoverageAdapter> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

}
