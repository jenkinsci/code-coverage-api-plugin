package io.jenkins.plugins.coverage.detector;

import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

public abstract class Detector implements ExtensionPoint, Describable<Detector> {

    public abstract List<FilePath> findFiles(FilePath workspace) throws IOException, InterruptedException;

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<Detector> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
}
