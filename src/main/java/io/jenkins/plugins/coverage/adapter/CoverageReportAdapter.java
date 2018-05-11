package io.jenkins.plugins.coverage.adapter;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import jenkins.model.Jenkins;
import org.w3c.dom.Document;

import java.io.File;


public abstract class CoverageReportAdapter implements ExtensionPoint, Describable<CoverageReportAdapter> {

    private String path;

    public CoverageReportAdapter(String path) {
        this.path = path;
    }

    protected abstract Document convert(File source);

    protected abstract CoverageResult parseToResult(Document document);

    public CoverageResult getResult(File source) {
        Document document = convert(source);
        return parseToResult(document);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<CoverageReportAdapter> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
}
