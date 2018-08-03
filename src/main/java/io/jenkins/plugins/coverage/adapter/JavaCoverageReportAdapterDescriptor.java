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
        return Arrays.asList(new CoverageElement("Java Group", 0),
                new CoverageElement("Java Package", 1),
                new CoverageElement("Java File", 2),
                new CoverageElement("Java Class", 3),
                new CoverageElement("Java Method", 4));
    }
}
