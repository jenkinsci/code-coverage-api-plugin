package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
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
import hudson.model.Result;
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

import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.prism.SourceCodeDirectory;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.AgentFileVisitor.FileVisitorResult;
import io.jenkins.plugins.util.CharsetValidation;
import io.jenkins.plugins.util.EnvironmentResolver;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.LogHandler;

/**
 * A pipeline {@code Step} or Freestyle or Maven {@link Recorder} that reads and parses coverage results in a build and
 * adds the results to the persisted build results.
 * <p>
 * Stores the created issues in a {@link Node}. This result is then attached to the {@link Run build} by registering a
 * {@link CoverageBuildAction}.
 * </p>
 *
 * @author Ullrich Hafner
 */
public class CoverageRecorder extends Recorder implements SimpleBuildStep {
    private String sourceCodeEncoding = StringUtils.EMPTY;
    private Set<SourceCodeDirectory> sourceDirectories = new HashSet<>();
    private boolean skipPublishingChecks = false;
    private SourceCodeRetention sourceCodeRetention = SourceCodeRetention.LAST_BUILD;
    private List<CoverageTool> tools = new ArrayList<>();

    private boolean failOnError;
    private boolean enabledForFailure = true;
    private List<QualityGate> qualityGates = new ArrayList<>();
    private String scm = StringUtils.EMPTY;

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
        this.tools = List.copyOf(tools);
    }

    public List<CoverageTool> getTools() {
        return tools;
    }

    /**
     * Defines the optional list of quality gates.
     *
     * @param qualityGates
     *         the quality gates
     */
    @SuppressWarnings("unused") // used by Stapler view data binding
    @DataBoundSetter
    public void setQualityGates(final List<QualityGate> qualityGates) {
        this.qualityGates = List.copyOf(qualityGates);
    }

    @SuppressWarnings("unused") // used by Stapler view data binding
    public List<QualityGate> getQualityGates() {
        return qualityGates;
    }

    /**
     * Sets whether publishing checks should be skipped or not.
     *
     * @param skipPublishingChecks
     *         {@code true} if publishing checks should be skipped, {@code false} otherwise
     */
    @DataBoundSetter
    public void setSkipPublishingChecks(final boolean skipPublishingChecks) {
        this.skipPublishingChecks = skipPublishingChecks;
    }

    public boolean isSkipPublishingChecks() {
        return skipPublishingChecks;
    }

    /**
     * Determines whether to fail the build on errors during the step of recording coverage reports.
     *
     * @param failOnError
     *         if {@code true} then the build will be failed on errors, {@code false} then errors are only reported in
     *         the UI
     */
    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler
    public void setFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    /**
     * Returns whether recording should be enabled for failed builds as well.
     *
     * @param enabledForFailure
     *         {@code true} if recording should be enabled for failed builds as well, {@code false} if recording is
     *         enabled for successful or unstable builds only
     */
    @DataBoundSetter
    public void setEnabledForFailure(final boolean enabledForFailure) {
        this.enabledForFailure = enabledForFailure;
    }

    public boolean isEnabledForFailure() {
        return enabledForFailure;
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

    @CheckForNull
    public String getSourceCodeEncoding() {
        return sourceCodeEncoding;
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
        sourceDirectories = Set.copyOf(sourceCodeDirectories);
    }

    public Set<SourceCodeDirectory> getSourceDirectories() {
        return sourceDirectories;
    }

    private Set<String> getSourceDirectoriesPaths() {
        Set<String> paths = sourceDirectories.stream()
                .map(SourceCodeDirectory::getPath)
                .collect(Collectors.toSet());
        paths.add("src/main/java");
        return paths;
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

    public SourceCodeRetention getSourceCodeRetention() {
        return sourceCodeRetention;
    }

    /**
     * Sets the SCM that should be used to find the reference build for. The reference recorder will select the SCM
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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@NonNull final Run<?, ?> run, @NonNull final FilePath workspace, @NonNull final EnvVars env,
            @NonNull final Launcher launcher, @NonNull final TaskListener listener) throws InterruptedException {
        Result overallResult = run.getResult();
        LogHandler logHandler = new LogHandler(listener, "Coverage");
        if (enabledForFailure || overallResult == null || overallResult.isBetterOrEqualTo(Result.UNSTABLE)) {
            FilteredLog log = new FilteredLog("Errors while recording code coverage:");
            log.logInfo("Recording coverage results");

            if (tools.isEmpty()) {
                log.logError("No tools defined that will record the coverage files");
                run.setResult(Result.FAILURE); // use StageResultHandler of warnings plugin
                logHandler.log(log);
            }
            else {
                List<Node> results = new ArrayList<>();
                for (CoverageTool tool : tools) {
                    LogHandler toolHandler = new LogHandler(listener, tool.getActualName());
                    CoverageParser parser = tool.getParser();
                    if (StringUtils.isBlank(tool.getPattern())) {
                        toolHandler.log("Using default pattern '%s' since user defined pattern is not set",
                                parser.getDefaultPattern());
                    }

                    String expandedPattern = expandPattern(run, tool.getActualPattern());
                    if (!expandedPattern.equals(tool.getActualPattern())) {
                        log.logInfo("Expanding pattern '%s' to '%s'", tool.getActualPattern(), expandedPattern);
                    }

                    try {
                        FileVisitorResult<ModuleNode> result = workspace.act(
                                new CoverageReportScanner(expandedPattern, "UTF-8", false, parser));
                        log.merge(result.getLog());
                        var rootNode = Node.merge(result.getResults());
                        results.add(rootNode);
                        if (rootNode.isEmpty() && result.hasErrors()) {
                            if (failOnError) {
                                var errorMessage = "Failing build due to some errors during recording of the coverage";
                                log.logInfo(errorMessage);
                                var resultHandler = new RunResultHandler(run);
                                resultHandler.setResult(Result.FAILURE, errorMessage);
                            }
                            else {
                                log.logInfo("Ignore errors and continue processing");
                            }
                        }
                    }
                    catch (IOException exception) {
                        log.logException(exception, "Exception while parsing with tool " + tool);
                    }

                    toolHandler.log(log);
                }

                CoverageReporter reporter = new CoverageReporter();
                reporter.run(Node.merge(results), run, workspace, listener,
                        getQualityGates(),
                        getScm(), getSourceDirectoriesPaths(),
                        getSourceCodeEncoding(), getSourceCodeRetention());
            }
        }
        else {
            logHandler.log("Skipping execution of coverage recorder since overall result is '%s'", overallResult);
        }
    }

    private String expandPattern(final Run<?, ?> run, final String actualPattern) {
        try {
            EnvironmentResolver environmentResolver = new EnvironmentResolver();

            return environmentResolver.expandEnvironmentVariables(
                    run.getEnvironment(TaskListener.NULL), actualPattern);
        }
        catch (IOException | InterruptedException ignore) {
            return actualPattern; // fallback, no expansion
        }
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
         *
         * @return a model with all {@link SourceCodeRetention} strategies.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
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
         *
         * @return a model with all available charsets
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
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
        @SuppressWarnings("unused") // used by Stapler view data binding
        public FormValidation doCheckSourceCodeEncoding(@AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String sourceCodeEncoding) {
            if (!JENKINS.hasPermission(Item.CONFIGURE, project)) {
                return FormValidation.ok();
            }

            return CHARSET_VALIDATION.validateCharset(sourceCodeEncoding);
        }
    }
}
