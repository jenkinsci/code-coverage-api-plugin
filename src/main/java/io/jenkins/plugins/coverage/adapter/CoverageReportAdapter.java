package io.jenkins.plugins.coverage.adapter;

import com.google.common.collect.Lists;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.threshold.Threshold;
import org.kohsuke.stapler.DataBoundSetter;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public abstract class CoverageReportAdapter extends CoverageAdapter {

    // ant path of report files
    private final String path;
    private List<Threshold> thresholds = new LinkedList<>();

    /**
     * @param path Ant-style path of report files.
     */
    public CoverageReportAdapter(String path) {
        this.path = path;
    }


    /**
     * Get {@link CoverageResult} from report file.
     *
     * @param report report file
     * @return CoverageResult
     * @throws CoverageException cannot convert report file to CoverageResult
     */
    public CoverageResult getResult(File report) throws CoverageException {
        Document document = convert(report);
        return parseToResult(document, report.getName());
    }

    /**
     * convert report to standard format report, and return the DOM document representation.
     *
     * @param source report file
     * @return {@link Document} representation of standard format report
     */
    protected abstract Document convert(File source) throws CoverageException;

    /**
     * parse report document to {@link CoverageResult}.
     *
     * @param document   document be parse
     * @param reportName report name
     * @return CoverageResult
     */
    @CheckForNull
    protected abstract CoverageResult parseToResult(Document document, String reportName);


    /**
     * Getter for thresholds.
     *
     * @return thresholds
     * @see Threshold
     */
    public List<Threshold> getThresholds() {
        return thresholds;
    }

    /**
     * Setter for thresholds.
     *
     * @param thresholds value to set for thresholds
     */
    @DataBoundSetter
    public void setThresholds(List<Threshold> thresholds) {
        this.thresholds = thresholds;
    }

    /**
     * Getter for property 'path'.
     *
     * @return value for property 'path'
     */
    public String getPath() {
        return path;
    }

    public void performCoveragePlugin(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        CoveragePublisher publisher = new CoveragePublisher();
        publisher.setAdapters(Lists.newArrayList(this));

        publisher.perform(run, workspace, launcher, listener);
    }
}
