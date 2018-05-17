package io.jenkins.plugins.coverage.adapter;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.exception.ConversionException;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import jenkins.model.Jenkins;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import java.io.File;


public abstract class CoverageReportAdapter implements ExtensionPoint, Describable<CoverageReportAdapter> {

    // path of report file
    private final String path;

    public CoverageReportAdapter(String path) {
        this.path = path;
    }


    public CoverageResult getResult(File source) throws ConversionException {
        Document document = convert(source);
        return parseToResult(document);
    }

    /**
     * convert report to standard format report, and return the DOM document representation.
     *
     * @param source Report file
     * @return DOM document representation of standard format report
     */
    protected abstract Document convert(File source) throws ConversionException;

    @CheckForNull
    protected abstract CoverageResult parseToResult(Document document);


    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<CoverageReportAdapter> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public String getPath() {
        return path;
    }
}
