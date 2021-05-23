package io.jenkins.plugins.coverage;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.detector.ReportDetector;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
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

    private List<CoverageAdapter> adapters = new LinkedList<>();
    private List<Threshold> globalThresholds = new LinkedList<>();

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private boolean applyThresholdRecursively;

    // TODO make sourceFileResolver more generic
    private DefaultSourceFileResolver sourceFileResolver;

    private String tag;

    private boolean calculateDiffForChangeRequests = false;
    private boolean failBuildIfCoverageDecreasedInChangeRequest = false;

    private boolean skipPublishingChecks = false;

    @DataBoundConstructor
    public CoveragePublisher() {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Publishing Coverage report....");

        CoverageProcessor processor = new CoverageProcessor(run, workspace, listener);

        List<CoverageReportAdapter> reportAdapters = new LinkedList<>();
        List<ReportDetector> reportDetectors = new LinkedList<>();

        for (CoverageAdapter adapter : getAdapters()) {
            if (adapter instanceof CoverageReportAdapter) {
                reportAdapters.add((CoverageReportAdapter) adapter);
            } else if (adapter instanceof ReportDetector) {
                reportDetectors.add((ReportDetector) adapter);
            }
        }

        if (sourceFileResolver != null) {
            processor.setSourceFileResolver(sourceFileResolver);
        }


        processor.setGlobalTag(tag);
        processor.setFailBuildIfCoverageDecreasedInChangeRequest(failBuildIfCoverageDecreasedInChangeRequest);
        processor.setFailUnhealthy(failUnhealthy);
        processor.setFailUnstable(failUnstable);
        processor.setFailNoReports(failNoReports);
        processor.setApplyThresholdRecursively(applyThresholdRecursively);

        try {
            processor.performCoverageReport(reportAdapters, reportDetectors, globalThresholds);
        } catch (CoverageException e) {
            listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
            run.setResult(Result.FAILURE);
        }

        if (!skipPublishingChecks) {
            CoverageAction coverageAction = run.getAction(CoverageAction.class);
            if (coverageAction != null) {
                CoverageChecksPublisher checksPublisher = new CoverageChecksPublisher(coverageAction);
                checksPublisher.publishChecks(listener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public List<CoverageAdapter> getAdapters() {
        return adapters;
    }

    @DataBoundSetter
    public void setAdapters(List<CoverageAdapter> adapters) {
        this.adapters = adapters;
    }

    public List<Threshold> getGlobalThresholds() {
        return globalThresholds;
    }

    @DataBoundSetter
    public void setGlobalThresholds(List<Threshold> globalThresholds) {
        this.globalThresholds = globalThresholds;
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

    public DefaultSourceFileResolver getSourceFileResolver() {
        return sourceFileResolver;
    }

    @DataBoundSetter
    public void setSourceFileResolver(DefaultSourceFileResolver sourceFileResolver) {
        this.sourceFileResolver = sourceFileResolver;
    }

    public String getTag() {
        return tag;
    }

    @DataBoundSetter
    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean getFailBuildIfCoverageDecreasedInChangeRequest() {
        return failBuildIfCoverageDecreasedInChangeRequest;
    }

    public boolean getCalculateDiffForChangeRequests() {
        return calculateDiffForChangeRequests;
    }

    /**
     * @deprecated not needed anymore. Diff is calculated automatically if reference build found.
     */
    @Deprecated
    @DataBoundSetter
    public void setCalculateDiffForChangeRequests(boolean calculateDiffForChangeRequests) {
        this.calculateDiffForChangeRequests = calculateDiffForChangeRequests;
    }

    @DataBoundSetter
    public void setSkipPublishingChecks(boolean skipPublishingChecks) {
        this.skipPublishingChecks = skipPublishingChecks;
    }

    public boolean isSkipPublishingChecks() {
        return skipPublishingChecks;
    }

    @DataBoundSetter
    public void setFailBuildIfCoverageDecreasedInChangeRequest(boolean failBuildIfCoverageDecreasedInChangeRequest) {
        this.failBuildIfCoverageDecreasedInChangeRequest = failBuildIfCoverageDecreasedInChangeRequest;
    }

    public boolean isApplyThresholdRecursively() {
        return applyThresholdRecursively;
    }

    @DataBoundSetter
    public void setApplyThresholdRecursively(boolean applyThresholdRecursively) {
        this.applyThresholdRecursively = applyThresholdRecursively;
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

        public DescriptorExtensionList<CoverageAdapter, CoverageAdapterDescriptor<?>> getListCoverageReportAdapterDescriptors() {
            return CoverageAdapterDescriptor.all();
        }

        @SuppressWarnings("unchecked")
        public Descriptor<SourceFileResolver> getSourceFileResolverDescriptor() {
            return new DefaultSourceFileResolver.DefaultSourceFileResolverDescriptor();
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
