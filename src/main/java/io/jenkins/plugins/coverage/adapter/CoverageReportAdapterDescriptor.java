package io.jenkins.plugins.coverage.adapter;


import io.jenkins.plugins.coverage.targets.CoverageElement;

import java.util.Collections;
import java.util.List;


public class CoverageReportAdapterDescriptor<T extends CoverageReportAdapter>
        extends CoverageAdapterDescriptor<CoverageReportAdapter> {

    public CoverageReportAdapterDescriptor(Class<? extends CoverageReportAdapter> clazz) {
        super(clazz);
    }

    public List<CoverageElement> getCoverageElements() {
        return Collections.emptyList();
    }

    public String getCoverageElementType() {
        return CoverageElement.COVERAGE_ELEMENT_TYPE_NONE;
    }

    public boolean defaultMergeToOneReport() {
        return false;
    }
}
