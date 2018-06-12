package io.jenkins.plugins.coverage.detector;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;

public class DetectorDescriptor<T extends Detector> extends Descriptor<Detector> {

    private String detectorName;

    public DetectorDescriptor(Class<? extends Detector> clazz, String detectorName) {
        super(clazz);
        this.detectorName = detectorName;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        return detectorName;
    }

    public static DescriptorExtensionList<Detector, DetectorDescriptor<?>> all() {
        return Jenkins.getInstance().getDescriptorList(Detector.class);
    }
}
