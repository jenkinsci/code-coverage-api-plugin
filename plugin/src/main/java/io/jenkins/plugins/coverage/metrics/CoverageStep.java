package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tools.ToolDescriptor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import io.jenkins.plugins.prism.SourceCodeDirectory;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.AbstractExecution;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.ValidationUtilities;

/**
 * A pipeline {@code Step} that reads and parses coverage results in a build and adds the results to the persisted build
 * results. This step only provides the entry point for pipelines, the actual computation is delegated to an associated
 * Freestyle {@link CoverageRecorder} instance.
 *
 * @author Ullrich Hafner
 */
public class CoverageStep extends Step implements Serializable {
    private static final long serialVersionUID = 34386077204781270L;
    private static final ValidationUtilities VALIDATION_UTILITIES = new ValidationUtilities();

    private List<CoverageTool> tools = new ArrayList<>();
    private List<CoverageQualityGate> qualityGates = new ArrayList<>();
    private String id = StringUtils.EMPTY;
    private String name = StringUtils.EMPTY;
    private boolean skipPublishingChecks = false;
    private boolean failOnError = false;
    private boolean enabledForFailure = false;
    private boolean skipSymbolicLinks = false;
    private String scm = StringUtils.EMPTY;
    private String sourceCodeEncoding = StringUtils.EMPTY;
    private Set<SourceCodeDirectory> sourceDirectories = new HashSet<>();
    private SourceCodeRetention sourceCodeRetention = SourceCodeRetention.LAST_BUILD;

    /**
     * Creates a new instance of {@link  CoverageStep}.
     */
    @DataBoundConstructor
    public CoverageStep() {
        super();

        // empty constructor required for Stapler
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(context, this);
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
    public void setQualityGates(final List<CoverageQualityGate> qualityGates) {
        this.qualityGates = List.copyOf(qualityGates);
    }

    public List<CoverageQualityGate> getQualityGates() {
        return qualityGates;
    }

    /**
     * Overrides the default ID of the results. The ID is used as URL of the results and as identifier in UI elements.
     * If no ID is given, then the default ID "coverage".
     *
     * @param id
     *         the ID of the results
     *
     * @see ToolDescriptor#getId()
     */
    @DataBoundSetter
    public void setId(final String id) {
        VALIDATION_UTILITIES.ensureValidId(id);

        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Overrides the name of the results. The name is used for all labels in the UI. If no name is given, then the
     * default name is used.
     *
     * @param name
     *         the name of the results
     *
     * @see #getName()
     */
    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
     * Specify if traversal of symbolic links will be skipped during directory scanning for coverage reports.
     *
     * @param skipSymbolicLinks
     *         if symbolic links should be skipped during directory scanning
     */
    @DataBoundSetter
    public void setSkipSymbolicLinks(final boolean skipSymbolicLinks) {
        this.skipSymbolicLinks = skipSymbolicLinks;
    }

    public boolean isSkipSymbolicLinks() {
        return skipSymbolicLinks;
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

    /**
     * Actually performs the execution of the associated step.
     */
    @SuppressFBWarnings(value = "THROWS", justification = "false positive")
    static class Execution extends AbstractExecution<Void> {
        private static final long serialVersionUID = -2840020502160375407L;
        private static final Void UNUSED = null;

        private final CoverageStep step;

        Execution(@NonNull final StepContext context, final CoverageStep step) {
            super(context);

            this.step = step;
        }

        @Override
        @CheckForNull
        protected Void run() throws IOException, InterruptedException {
            var recorder = new CoverageRecorder();
            recorder.setTools(step.getTools());
            recorder.setQualityGates(step.getQualityGates());
            recorder.setId(step.getId());
            recorder.setName(step.getName());
            recorder.setSkipPublishingChecks(step.isSkipPublishingChecks());
            recorder.setFailOnError(step.isFailOnError());
            recorder.setEnabledForFailure(step.isEnabledForFailure());
            recorder.setScm(step.getScm());
            recorder.setSourceCodeEncoding(step.getSourceCodeEncoding());
            recorder.setSourceDirectories(List.copyOf(step.getSourceDirectories()));
            recorder.setSourceCodeRetention(step.getSourceCodeRetention());

            recorder.perform(getRun(), getWorkspace(), getTaskListener(), createStageResultHandler());

            return UNUSED;
        }
    }

    /**
     * Descriptor for this step: defines the context and the UI labels.
     */
    @Extension
    @SuppressWarnings("unused") // most methods are used by the corresponding jelly view
    public static class Descriptor extends StepDescriptor {
        private static final JenkinsFacade JENKINS = new JenkinsFacade();

        @Override
        public String getFunctionName() {
            return "recordCoverage";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Recorder_Name();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(FilePath.class, FlowNode.class, Run.class, TaskListener.class);
        }

        @Override
        public String argumentsToString(@NonNull final Map<String, Object> namedArgs) {
            String formatted = super.argumentsToString(namedArgs);
            if (formatted != null) {
                return formatted;
            }
            return namedArgs.toString();
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
                return SourceCodeRetention.fillItems();
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
                return VALIDATION_UTILITIES.getAllCharsets();
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

            return VALIDATION_UTILITIES.validateCharset(sourceCodeEncoding);
        }

        /**
         * Performs on-the-fly validation of the ID.
         *
         * @param project
         *         the project that is configured
         * @param id
         *         the ID of the tool
         *
         * @return the validation result
         */
        @POST
        public FormValidation doCheckId(@AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String id) {
            if (!JENKINS.hasPermission(Item.CONFIGURE, project)) {
                return FormValidation.ok();
            }

            return VALIDATION_UTILITIES.validateId(id);
        }
    }
}
