package io.jenkins.plugins.coverage;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CoveragePublisher extends Recorder implements SimpleBuildStep {

    private List<CoverageReportAdapter> adapters = new LinkedList<>();
    private List<Threshold> globalThresholds = new LinkedList<>();

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private String autoDetectPath;
    private SourceFileResolver sourceFileResolver;

    @DataBoundConstructor
    public CoveragePublisher(String autoDetectPath) {
        this.autoDetectPath = autoDetectPath;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Publishing Coverage report....");

        CoverageProcessor processor = new CoverageProcessor(run, workspace, listener);

        processor.setAutoDetectPath(autoDetectPath);

        if (sourceFileResolver != null) {
            sourceFileResolver.setOwnerBuild(run, workspace, listener);
            processor.setSourceFileResolver(sourceFileResolver);
        }

        processor.setFailUnhealthy(failUnhealthy);
        processor.setFailUnstable(failUnstable);
        processor.setFailNoReports(failNoReports);

        try {
            processor.performCoverageReport(getAdapters(), globalThresholds);
        } catch (CoverageException e) {
            listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
            run.setResult(Result.FAILURE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public List<CoverageReportAdapter> getAdapters() {
        return adapters;
    }

    @DataBoundSetter
    public void setAdapters(List<CoverageReportAdapter> adapters) {
        this.adapters = adapters;
    }

    public List<Threshold> getGlobalThresholds() {
        return globalThresholds;
    }

    @DataBoundSetter
    public void setGlobalThresholds(List<Threshold> globalThresholds) {
        this.globalThresholds = globalThresholds;
    }

    public String getAutoDetectPath() {
        return autoDetectPath;
    }

    @DataBoundSetter
    public void setAutoDetectPath(@Nonnull String autoDetectPath) {
        this.autoDetectPath = autoDetectPath;
    }

    public boolean isFailUnhealthy() {
        return failUnhealthy;
    }

    @DataBoundSetter
    public void setFailUnhealthy(boolean failUnhealthy) {
        this.failUnhealthy = failUnhealthy;
    }

    public boolean isFailUnstable() {
        return failUnstable;
    }

    @DataBoundSetter
    public void setFailUnstable(boolean failUnstable) {
        this.failUnstable = failUnstable;
    }

    public boolean isFailNoReports() {
        return failNoReports;
    }

    @DataBoundSetter
    public void setFailNoReports(boolean failNoReports) {
        this.failNoReports = failNoReports;
    }

    public SourceFileResolver getSourceFileResolver() {
        return sourceFileResolver;
    }

    @DataBoundSetter
    public void setSourceFileResolver(SourceFileResolver sourceFileResolver) {
        this.sourceFileResolver = sourceFileResolver;
    }

    @Symbol("publishCoverage")
    @Extension
    public static final class CoveragePublisherDescriptor extends BuildStepDescriptor<Publisher> {

        public CoveragePublisherDescriptor() {
            super(CoveragePublisher.class);
            load();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            super.configure(req, json);
            save();
            return true;
        }

        public DescriptorExtensionList<CoverageReportAdapter, CoverageReportAdapterDescriptor<?>> getListCoverageReportAdapterDescriptors() {
            return CoverageReportAdapterDescriptor.all();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.CoveragePublisher_displayName();
        }

        @Override
        public Publisher newInstance(@CheckForNull StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }
}
