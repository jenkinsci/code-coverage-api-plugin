package io.jenkins.plugins.coverage;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class CoveragePublisher extends Recorder implements SimpleBuildStep {

    private List<CoverageReportAdapter> adapters;
    private List<Threshold> globalThresholds;

    @Nonnull
    private String autoDetectPath = CoveragePublisherDescriptor.AUTO_DETACT_PATH;

    @DataBoundConstructor
    public CoveragePublisher(@Nonnull String autoDetectPath) {
        this.autoDetectPath = autoDetectPath;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        CoverageProcessor processor = new CoverageProcessor(run, workspace, listener, adapters, globalThresholds);

        if (!StringUtils.isEmpty(autoDetectPath)) {
            processor.enableAutoDetect(autoDetectPath);
        }

        CoverageResult result = processor.processCoverageReport();

        CoverageAction action = new CoverageAction(result);
        run.addAction(action);
    }

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

    @Nonnull
    public String getAutoDetectPath() {
        return autoDetectPath;
    }

    @DataBoundSetter
    public void setAutoDetectPath(@Nonnull String autoDetectPath) {
        this.autoDetectPath = autoDetectPath;
    }

    @Symbol("coverage")
    @Extension
    public static final class CoveragePublisherDescriptor extends BuildStepDescriptor<Publisher> {

        public static final String AUTO_DETACT_PATH = "*.xml";

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

        @Nonnull
        public CoverageMetric[] getAllCoverageMetrics() {
            return CoverageMetric.all();
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
