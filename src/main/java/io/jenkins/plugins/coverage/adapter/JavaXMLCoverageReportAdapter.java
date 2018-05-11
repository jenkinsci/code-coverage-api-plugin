package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.w3c.dom.Document;

public abstract class JavaXMLCoverageReportAdapter extends XMLCoverageReportAdapter {

    public JavaXMLCoverageReportAdapter(String path) {
        super(path);
    }

    @Override
    public CoverageResult parseToResult(Document document) {
        return new JavaCoverageParser().parse(document);
    }
}
