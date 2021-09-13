package io.jenkins.plugins.coverage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.jenkinsci.Symbol;
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
import jenkins.tasks.SimpleBuildStep;

import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.detector.ReportDetector;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageChecksPublisher;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.threshold.Threshold;

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

    @Override
    public void perform(@NonNull final Run<?, ?> run, @NonNull final FilePath workspace, @NonNull final Launcher launcher, @NonNull final TaskListener listener) throws InterruptedException, IOException {
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
            CoverageBuildAction coverageAction = run.getAction(CoverageBuildAction.class);
            if (coverageAction != null) {
                CoverageChecksPublisher checksPublisher = new CoverageChecksPublisher(coverageAction);
                checksPublisher.publishChecks(listener);
            }
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public List<CoverageAdapter> getAdapters() {
        return adapters;
    }

    @DataBoundSetter
    public void setAdapters(final List<CoverageAdapter> adapters) {
        this.adapters = adapters;
    }

    public List<Threshold> getGlobalThresholds() {
        return globalThresholds;
    }

    @DataBoundSetter
    public void setGlobalThresholds(final List<Threshold> globalThresholds) {
        this.globalThresholds = globalThresholds;
    }

    public boolean isFailUnhealthy() {
        return failUnhealthy;
    }

    @DataBoundSetter
    public void setFailUnhealthy(final boolean failUnhealthy) {
        this.failUnhealthy = failUnhealthy;
    }

    public boolean isFailUnstable() {
        return failUnstable;
    }

    @DataBoundSetter
    public void setFailUnstable(final boolean failUnstable) {
        this.failUnstable = failUnstable;
    }

    public boolean isFailNoReports() {
        return failNoReports;
    }

    @DataBoundSetter
    public void setFailNoReports(final boolean failNoReports) {
        this.failNoReports = failNoReports;
    }

    public DefaultSourceFileResolver getSourceFileResolver() {
        return sourceFileResolver;
    }

    @DataBoundSetter
    public void setSourceFileResolver(final DefaultSourceFileResolver sourceFileResolver) {
        this.sourceFileResolver = sourceFileResolver;
    }

    public String getTag() {
        return tag;
    }

    @DataBoundSetter
    public void setTag(final String tag) {
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
    public void setCalculateDiffForChangeRequests(final boolean calculateDiffForChangeRequests) {
        this.calculateDiffForChangeRequests = calculateDiffForChangeRequests;
    }

    @DataBoundSetter
    public void setSkipPublishingChecks(final boolean skipPublishingChecks) {
        this.skipPublishingChecks = skipPublishingChecks;
    }

    public boolean isSkipPublishingChecks() {
        return skipPublishingChecks;
    }

    @DataBoundSetter
    public void setFailBuildIfCoverageDecreasedInChangeRequest(final boolean failBuildIfCoverageDecreasedInChangeRequest) {
        this.failBuildIfCoverageDecreasedInChangeRequest = failBuildIfCoverageDecreasedInChangeRequest;
    }

    public boolean isApplyThresholdRecursively() {
        return applyThresholdRecursively;
    }

    @DataBoundSetter
    public void setApplyThresholdRecursively(final boolean applyThresholdRecursively) {
        this.applyThresholdRecursively = applyThresholdRecursively;
    }

    @Symbol("publishCoverage")
    @Extension
    public static final class CoveragePublisherDescriptor extends BuildStepDescriptor<Publisher> {
        public CoveragePublisherDescriptor() {
            super(CoveragePublisher.class);
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
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
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.CoveragePublisher_displayName();
        }

        @Override
        public Publisher newInstance(@CheckForNull final StaplerRequest req, @NonNull final JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }
}
