package io.jenkins.plugins.coverage.adapter;

import org.w3c.dom.Document;

import io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;

public abstract class JavaXMLCoverageReportAdapter extends XMLCoverageReportAdapter {


    public JavaXMLCoverageReportAdapter(final String path) {
        super(path);
    }

    @Override
    public CoverageResult parseToResult(final Document document, final String reportName) throws CoverageException {
        return new JavaCoverageParser(reportName).parse(document);
    }
}
