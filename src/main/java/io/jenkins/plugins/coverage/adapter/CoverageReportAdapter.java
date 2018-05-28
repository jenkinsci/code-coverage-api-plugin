package io.jenkins.plugins.coverage.adapter;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.LinkedList;
import java.util.List;


public abstract class CoverageReportAdapter implements ExtensionPoint, Describable<CoverageReportAdapter> {

    // path of report file
    private final String path;
    private List<Threshold> thresholds = new LinkedList<>();

    private String name;

    public CoverageReportAdapter(String path) {
        this.path = path;
    }


    public CoverageResult getResult(File source) throws CoverageException {
        Document document = convert(source);
        return parseToResult(document, source.getName());
    }

    /**
     * convert report to standard format report, and return the DOM document representation.
     *
     * @param source Report file
     * @return DOM document representation of standard format report
     */
    protected abstract Document convert(File source) throws CoverageException;

    @CheckForNull
    protected abstract CoverageResult parseToResult(Document document, String reportName);


    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<CoverageReportAdapter> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public List<Threshold> getThresholds() {
        return thresholds;
    }

    @DataBoundSetter
    public void setThresholds(List<Threshold> thresholds) {
        this.thresholds = thresholds;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
