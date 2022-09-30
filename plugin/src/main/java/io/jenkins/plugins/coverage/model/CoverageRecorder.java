package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.jenkinsci.Symbol;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

import io.jenkins.plugins.prism.CharsetValidation;
import io.jenkins.plugins.prism.SourceCodeDirectory;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.LogHandler;

/**
 * A pipeline {@code Step} or Freestyle or Maven {@link Recorder} that reads and parses coverage results in a build and
 * adds the results to the persisted build results.
 * <p>
 * Stores the created issues in a {@link CoverageNode}. This result is then attached to the {@link Run build} by
 * registering a {@link CoverageBuildAction}.
 * </p>
 *
 * @author Ullrich Hafner
 */
public class CoverageRecorder extends Recorder implements SimpleBuildStep {
    private String sourceCodeEncoding = StringUtils.EMPTY;
    private Set<SourceCodeDirectory> sourceDirectories = new HashSet<>();
    private boolean skipPublishingChecks = false;
    private SourceCodeRetention sourceCodeRetention = SourceCodeRetention.NEVER;
    private List<CoverageTool> tools = new ArrayList<>();
    private int healthy;
    private int unhealthy;

    /**
     * Creates a new instance of {@link  CoverageRecorder}.
     */
    @DataBoundConstructor
    public CoverageRecorder() {
        super();

        // empty constructor required for Stapler
    }

    /**
     * Sets the coverage tools that will scan files and create coverage reports.
     *
     * @param tools
     *         the coverage tools
     */
    @DataBoundSetter
    public void setTools(final List<CoverageTool> tools) {
        this.tools = new ArrayList<>(tools);
    }

    /**
     * Returns the static analysis tools that will scan files and create issues.
     *
     * @return the static analysis tools
     */
    public List<CoverageTool> getTools() {
        return new ArrayList<>(tools);
    }

    /**
     * Returns whether publishing checks should be skipped.
     *
     * @return {@code true} if publishing checks should be skipped, {@code false} otherwise
     */
    public boolean isSkipPublishingChecks() {
        return skipPublishingChecks;
    }

    /**
     * Sets whether publishing checks should be skipped or not.
     *
     * @param skipPublishingChecks  {@code true} if publishing checks should be skipped, {@code false} otherwise
     */
    @DataBoundSetter
    public void setSkipPublishingChecks(final boolean skipPublishingChecks) {
        this.skipPublishingChecks = skipPublishingChecks;
    }

    /**
     * Returns the  encoding to use to read source files.
     *
     * @return the source code encoding
     */
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
        sourceDirectories = new HashSet<>(sourceCodeDirectories);
    }

    private Set<String> getSourceDirectoriesPaths() {
        Set<String> paths = sourceDirectories.stream()
                .map(SourceCodeDirectory::getPath)
                .collect(Collectors.toSet());
        paths.add("src/main/java");
        return paths;
    }

    /**
     * Returns the retention strategy for source code files.
     *
     * @return the retention strategy for source code files
     */
    public SourceCodeRetention getSourceCodeRetention() {
        return sourceCodeRetention;
    }

    /**
     * Defines the retention strategy for source code files.
     *
     * @param sourceCodeRetention
     *         the retention strategy for source code files
     */
    @DataBoundSetter
    public void setSourceCodeRetention(final SourceCodeRetention sourceCodeRetention) {
        this.sourceCodeRetention = sourceCodeRetention;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@NonNull final Run<?, ?> run, @NonNull final FilePath workspace, @NonNull final EnvVars env,
            @NonNull final Launcher launcher, @NonNull final TaskListener listener) throws InterruptedException {
        LogHandler logHandler = new LogHandler(listener, "Coverage");
        FilteredLog log = new FilteredLog("Errors while recording code coverage:");
        log.logInfo("Recording coverage results");




        logHandler.log(log);
    }

    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    /**
     * Descriptor for this step: defines the context and the UI elements.
     */
    @Extension
    @Symbol("recordCoverage")
    public static class Descriptor extends BuildStepDescriptor<Publisher> {
        private static final JenkinsFacade JENKINS = new JenkinsFacade();
        private static final CharsetValidation CHARSET_VALIDATION = new CharsetValidation();

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Recorder_Name();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        /**
         * Returns a model with all {@link SourceCodeRetention} strategies.
         *
         * @param project
         *         the project that is configured
         * @return a model with all {@link SourceCodeRetention} strategies.
         */
        @POST
        public ListBoxModel doFillSourceCodeRetentionItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel options = new ListBoxModel();
                add(options, SourceCodeRetention.NEVER);
                add(options, SourceCodeRetention.LAST_BUILD);
                add(options, SourceCodeRetention.EVERY_BUILD);
                return options;
            }
            return new ListBoxModel();
        }

        private void add(final ListBoxModel options, final SourceCodeRetention sourceCodeRetention) {
            options.add(sourceCodeRetention.getDisplayName(), sourceCodeRetention.name());
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
                return CHARSET_VALIDATION.getAllCharsets();
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

            return CHARSET_VALIDATION.validateCharset(sourceCodeEncoding);
        }
    }
}
