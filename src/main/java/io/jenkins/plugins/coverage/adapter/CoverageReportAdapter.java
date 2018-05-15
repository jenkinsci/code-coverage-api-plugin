package io.jenkins.plugins.coverage.adapter;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import jenkins.model.Jenkins;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import java.io.File;


public abstract class CoverageReportAdapter implements ExtensionPoint, Describable<CoverageReportAdapter> {

    // report file path
    private String path;

    public CoverageReportAdapter(String path) {
        this.path = path;
    }

    /**
     * convert report to standard format report, and return the DOM document representation.
     *
     * @param source Report file
     * @return DOM document representation of standard format report
     */
    protected abstract Document convert(File source);

    @CheckForNull
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
