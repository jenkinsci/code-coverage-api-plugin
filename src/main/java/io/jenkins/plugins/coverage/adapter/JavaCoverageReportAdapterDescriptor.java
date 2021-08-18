package io.jenkins.plugins.coverage.adapter;

import java.util.List;

import com.google.common.collect.Lists;

import io.jenkins.plugins.coverage.targets.CoverageElement;

public class JavaCoverageReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {
    /** A Java package. */
    protected static final CoverageElement PACKAGE = new CoverageElement("Package", 1);

    public JavaCoverageReportAdapterDescriptor(final Class<? extends CoverageReportAdapter> clazz) {
        super(clazz);
    }

    @Override
    public List<CoverageElement> getCoverageElements() {
        return Lists.newArrayList(new CoverageElement("Group", 0),
                PACKAGE,
                CoverageElement.FILE,
                new CoverageElement("Class", 3),
                new CoverageElement("Method", 4));
    }

    @Override
    public String getCoverageElementType() {
        return CoverageElement.COVERAGE_ELEMENT_TYPE_JAVA;
    }
}
