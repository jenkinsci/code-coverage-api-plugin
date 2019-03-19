package io.jenkins.plugins.coverage;

import hudson.DescriptorExtensionList;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;

public class CoverageElementInitializer {

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    @SuppressWarnings("unused")
    public static void registerAllCoverageElement() {
        DescriptorExtensionList<CoverageAdapter, CoverageAdapterDescriptor<?>> reportDescriptors = CoverageAdapterDescriptor.all();

        reportDescriptors.stream()
                .filter(d -> d instanceof CoverageReportAdapterDescriptor)
                .map(d -> (CoverageReportAdapterDescriptor) d)
                .forEach(d -> CoverageElementRegister.addCoverageElements(d.getCoverageElementType(), d.getCoverageElements()));

    }
}
