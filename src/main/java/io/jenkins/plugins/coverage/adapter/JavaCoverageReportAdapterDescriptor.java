package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.targets.CoverageElement;

import java.util.Arrays;
import java.util.List;

public class JavaCoverageReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

    public JavaCoverageReportAdapterDescriptor(Class<? extends CoverageReportAdapter> clazz) {
        super(clazz);
    }

    @Override
    public List<CoverageElement> getCoverageElements() {
        return Arrays.asList(new CoverageElement("Group", 0),
                new CoverageElement("Package", 1),
                new CoverageElement("File", 2),
                new CoverageElement("Class", 3),
                new CoverageElement("Method", 4));
    }
}
