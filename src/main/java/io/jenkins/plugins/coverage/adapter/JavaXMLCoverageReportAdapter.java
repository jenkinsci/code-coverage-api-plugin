package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.w3c.dom.Document;

public abstract class JavaXMLCoverageReportAdapter extends XMLCoverageReportAdapter {


    public JavaXMLCoverageReportAdapter(String path) {
        super(path);
    }

    @Override
    public CoverageResult parseToResult(Document document, String reportName) throws CoverageException {
        return new JavaCoverageParser(reportName).parse(document);
    }
}
