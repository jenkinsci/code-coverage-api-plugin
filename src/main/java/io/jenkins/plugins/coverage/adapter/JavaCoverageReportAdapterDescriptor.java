package io.jenkins.plugins.coverage.adapter;

import java.util.List;

import com.google.common.collect.Lists;

import io.jenkins.plugins.coverage.targets.CoverageElement;

public class JavaCoverageReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {
    /** A Java package. */
    public static final CoverageElement PACKAGE = new CoverageElement("Package", 1);
    /** A Java class. */
    public static final CoverageElement CLASS = new CoverageElement("Class", 3);
    /** A Java method. */
    public static final CoverageElement METHOD = new CoverageElement("Method", 4);

    public JavaCoverageReportAdapterDescriptor(final Class<? extends CoverageReportAdapter> clazz) {
        super(clazz);
    }

    @Override
    public List<CoverageElement> getCoverageElements() {
        return Lists.newArrayList(new CoverageElement("Group", 0),
                PACKAGE,
                CoverageElement.FILE,
                CLASS,
                METHOD);
    }

    @Override
    public String getCoverageElementType() {
        return CoverageElement.COVERAGE_ELEMENT_TYPE_JAVA;
    }
}
