package io.jenkins.plugins.coverage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.jenkinsci.Symbol;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.detector.ReportDetector;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.prism.CharsetValidation;
import io.jenkins.plugins.prism.SourceCodeDirectory;
import io.jenkins.plugins.util.JenkinsFacade;

public class CoveragePublisher extends Recorder implements SimpleBuildStep {
    private List<CoverageAdapter> adapters = new LinkedList<>();
    private List<Threshold> globalThresholds = new LinkedList<>();

    private String scm = StringUtils.EMPTY; // @since 3.0.0

    private String sourceCodeEncoding = StringUtils.EMPTY; // @since 2.1.0
    private Set<SourceCodeDirectory> sourceDirectories = new HashSet<>(); // @since 2.1.0

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private boolean applyThresholdRecursively;

    private DefaultSourceFileResolver sourceFileResolver;

    private String tag;

    private boolean calculateDiffForChangeRequests = false;
    private boolean failBuildIfCoverageDecreasedInChangeRequest = false;

    private boolean skipPublishingChecks = false;

    private String checksName = "Code Coverage";

    @DataBoundConstructor
    public CoveragePublisher() {
    }

    @Override
    public void perform(@NonNull final Run<?, ?> run, @NonNull final FilePath workspace,
            @NonNull final Launcher launcher, @NonNull final TaskListener listener)
            throws InterruptedException, IOException {
        listener.getLogger().println("Publishing Coverage report....");

        CoverageProcessor processor = new CoverageProcessor(run, workspace, listener);

        List<CoverageReportAdapter> reportAdapters = new LinkedList<>();
        List<ReportDetector> reportDetectors = new LinkedList<>();

        for (CoverageAdapter adapter : getAdapters()) {
            if (adapter instanceof CoverageReportAdapter) {
                reportAdapters.add((CoverageReportAdapter) adapter);
            }
            else if (adapter instanceof ReportDetector) {
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
            processor.performCoverageReport(reportAdapters, reportDetectors, globalThresholds,
                    getSourceDirectoriesPaths(),
                    sourceCodeEncoding, scm);
        }
        catch (CoverageException e) {
            listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
            run.setResult(Result.FAILURE);
        }

        if (!skipPublishingChecks) {
            CoverageAction coverageAction = run.getAction(CoverageAction.class);
            if (coverageAction != null) {
                CoverageChecksPublisher checksPublisher = new CoverageChecksPublisher(coverageAction, checksName);
                checksPublisher.publishChecks(listener);
            }
        }
    }

    private Set<String> getSourceDirectoriesPaths() {
        Set<String> paths = sourceDirectories.stream()
                .map(SourceCodeDirectory::getPath)
                .collect(Collectors.toSet());
        paths.add("src/main/java");
        return paths;
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

    /**
     * Sets the SCM that should be used to find the reference build for. The the SCM will be selected
     * based on a substring comparison, there is no need to specify the full name.
     *
     * @param scm
     *         the ID of the SCM to use (a substring of the full ID)
     */
    @DataBoundSetter
    public void setScm(final String scm) {
        this.scm = scm;
    }

    public String getScm() {
        return scm;
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

    public String getChecksName() {
        return checksName;
    }

    @DataBoundSetter
    public void setChecksName(final String checksName) {
        this.checksName = checksName;
    }

    @DataBoundSetter
    public void setFailBuildIfCoverageDecreasedInChangeRequest(
            final boolean failBuildIfCoverageDecreasedInChangeRequest) {
        this.failBuildIfCoverageDecreasedInChangeRequest = failBuildIfCoverageDecreasedInChangeRequest;
    }

    public boolean isApplyThresholdRecursively() {
        return applyThresholdRecursively;
    }

    @DataBoundSetter
    public void setApplyThresholdRecursively(final boolean applyThresholdRecursively) {
        this.applyThresholdRecursively = applyThresholdRecursively;
    }

    @CheckForNull
    public String getSourceCodeEncoding() {
        return sourceCodeEncoding;
    }

    /**
     * Sets the encoding to use to read source files.
     *
     * @param sourceCodeEncoding
     *         the encoding, e.g. "ISO-8859-1"
     */
    @DataBoundSetter
    public void setSourceCodeEncoding(final String sourceCodeEncoding) {
        this.sourceCodeEncoding = sourceCodeEncoding;
    }

    /**
     * Gets the paths to the directories that contain the source code.
     *
     * @return directories containing the source code
     */
    public List<SourceCodeDirectory> getSourceDirectories() {
        return new ArrayList<>(sourceDirectories);
    }

    /**
     * Sets the paths to the directories that contain the source code. If not relative and thus not part of the
     * workspace then these directories need to be added in Jenkins global configuration to prevent accessing of
     * forbidden resources.
     *
     * @param sourceCodeDirectories
     *         directories containing the source code
     */
    @DataBoundSetter
    public void setSourceDirectories(final List<SourceCodeDirectory> sourceCodeDirectories) {
        this.sourceDirectories = new HashSet<>(sourceCodeDirectories);
    }

    /**
     * Called after de-serialization to retain backward compatibility or to populate new elements (that would be
     * otherwise initialized to {@code null}).
     *
     * @return this
     */
    protected Object readResolve() {
        if (sourceDirectories == null) {
            sourceDirectories = new HashSet<>();
        }
        if (sourceCodeEncoding == null) {
            sourceCodeEncoding = StringUtils.EMPTY;
        }
        if (scm == null) {
            scm = StringUtils.EMPTY;
        }
        return this;
    }

    @Extension @Symbol("publishCoverage")
    public static final class CoveragePublisherDescriptor extends BuildStepDescriptor<Publisher> {
        private static final JenkinsFacade JENKINS = new JenkinsFacade();

        private final CharsetValidation validation = new CharsetValidation();

        public CoveragePublisherDescriptor() {
            super(CoveragePublisher.class);
        }

        /**
         * Returns a model with all available charsets.
         *
         * @param project
         *         the project that is configured
         * @return a model with all available charsets
         */
        @POST
        public ComboBoxModel doFillSourceCodeEncodingItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                return validation.getAllCharsets();
            }
            return new ComboBoxModel();
        }

        /**
         * Performs on-the-fly validation on the character encoding.
         *
         * @param project
         *         the project that is configured
         * @param sourceCodeEncoding
         *         the character encoding
         *
         * @return the validation result
         */
        @POST
        public FormValidation doCheckSourceCodeEncoding(@AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String sourceCodeEncoding) {
            if (!JENKINS.hasPermission(Item.CONFIGURE, project)) {
                return FormValidation.ok();
            }

            return validation.validateCharset(sourceCodeEncoding);
        }

        @SuppressWarnings("unchecked")
        public Descriptor<SourceFileResolver> getSourceFileResolverDescriptor() {
            return new DefaultSourceFileResolver.DefaultSourceFileResolverDescriptor();
        }

        public DescriptorExtensionList<CoverageAdapter, CoverageAdapterDescriptor<?>> getListCoverageReportAdapterDescriptors() {
            return CoverageAdapterDescriptor.all();
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
    }
}
