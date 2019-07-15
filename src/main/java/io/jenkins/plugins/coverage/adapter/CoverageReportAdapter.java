package io.jenkins.plugins.coverage.adapter;

import com.google.common.collect.Lists;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
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
    private boolean mergeToOneReport = false;

    private List<Threshold> thresholds = new LinkedList<>();

    /**
     * @param path Ant-style path of report files.
     */
    public CoverageReportAdapter(String path) {
        this.path = path;
    }


    @DataBoundSetter
    public void setMergeToOneReport(boolean mergeToOneReport) {
        this.mergeToOneReport = mergeToOneReport;
    }

    public boolean isMergeToOneReport() {
        return mergeToOneReport;
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
    protected abstract CoverageResult parseToResult(Document document, String reportName) throws CoverageException;


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

    /**
     * Perform publish coverage reports step with the adapter called this method.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong; use {@link AbortException} for a polite error
     */
    @SuppressWarnings("unused")
    public final void performCoveragePlugin(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        performCoveragePlugin(run, workspace, launcher, listener, null);
    }

    /**
     * Perform publish coverage reports step with the adapter called this method.
     *
     * @param run                a build this is running as a part of
     * @param workspace          a workspace to use for any file operations
     * @param launcher           a way to start processes
     * @param listener           a place to send output
     * @param sourceFileResolver source file resolver
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong; use {@link AbortException} for a polite error
     */
    public final void performCoveragePlugin(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, DefaultSourceFileResolver sourceFileResolver) throws IOException, InterruptedException {
        CoveragePublisher publisher = new CoveragePublisher();
        publisher.setAdapters(Lists.newArrayList(this));
        publisher.setSourceFileResolver(sourceFileResolver);

        publisher.perform(run, workspace, launcher, listener);

    }
}
