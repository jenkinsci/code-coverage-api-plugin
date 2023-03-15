package io.jenkins.plugins.coverage.metrics.steps;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.registry.ParserRegistry;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.jvnet.localizer.Localizable;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.ValidationUtilities;

/**
 * A coverage tool that can produce a {@link Node coverage tree} by parsing a given report file.
 *
 * @author Ullrich Hafner
 */
public class CoverageTool extends AbstractDescribableImpl<CoverageTool> implements Serializable {
    private static final long serialVersionUID = -8612521458890553037L;
    private static final ValidationUtilities VALIDATION_UTILITIES = new ValidationUtilities();

    private JenkinsFacade jenkins = new JenkinsFacade();

    private String pattern = StringUtils.EMPTY;
    private Parser parser = Parser.JACOCO;

    /**
     * Creates a new {@link io.jenkins.plugins.coverage.metrics.steps.CoverageTool}.
     */
    @DataBoundConstructor
    public CoverageTool() {
        super();
        // empty for stapler
    }

    CoverageTool(final Parser parser, final String pattern) {
        super();

        this.pattern = pattern;
        this.parser = parser;
    }

    public Parser getParser() {
        return parser;
    }

    /**
     * Sets the parser to be used to read the input files.
     *
     * @param parser the parser to use
     */
    @DataBoundSetter
    public void setParser(final Parser parser) {
        this.parser = parser;
    }

    @VisibleForTesting
    void setJenkinsFacade(final JenkinsFacade jenkinsFacade) {
        jenkins = jenkinsFacade;
    }

    /**
     * Called after de-serialization to retain backward compatibility.
     *
     * @return this
     */
    protected Object readResolve() {
        jenkins = new JenkinsFacade();

        return this;
    }

    /**
     * Sets the Ant file-set pattern of files to work with. If the pattern is undefined then the console log is
     * scanned.
     *
     * @param pattern
     *         the pattern to use
     */
    @DataBoundSetter
    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    @CheckForNull
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns the actual pattern to work with. If no user defined pattern is given, then the default pattern is
     * returned.
     *
     * @return the name
     * @see #setPattern(String)
     */
    public String getActualPattern() {
        return StringUtils.defaultIfBlank(pattern, parser.getDefaultPattern());
    }

    @Override
    public String toString() {
        return String.format("%s (pattern: %s)", getParser(), getActualPattern());
    }

    @Override
    public CoverageToolDescriptor getDescriptor() {
        return (CoverageToolDescriptor) jenkins.getDescriptorOrDie(getClass());
    }

    public String getDisplayName() {
        return getParser().getDisplayName();
    }

    /** Descriptor for {@link CoverageTool}. **/
    @Extension
    public static class CoverageToolDescriptor extends Descriptor<io.jenkins.plugins.coverage.metrics.steps.CoverageTool> {
        private static final JenkinsFacade JENKINS = new JenkinsFacade();

        /**
         * Creates a new instance of {@link CoverageToolDescriptor}.
         */
        public CoverageToolDescriptor() {
            super();

            // empty constructor required for stapler
        }

        /**
         * Returns a model with all {@link SourceCodeRetention} strategies.
         *
         * @param project
         *         the project that is configured
         * @return a model with all {@link SourceCodeRetention} strategies.
         */
        @POST
        public ListBoxModel doFillParserItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel options = new ListBoxModel();
                add(options, Parser.JACOCO);
                add(options, Parser.COBERTURA);
                add(options, Parser.PIT);
                return options;
            }
            return new ListBoxModel();
        }

        private void add(final ListBoxModel options, final Parser parser) {
            options.add(parser.getDisplayName(), parser.name());
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
            if (!new JenkinsFacade().hasPermission(Item.CONFIGURE, project)) {
                return FormValidation.ok();
            }

            return VALIDATION_UTILITIES.validateId(id);
        }

        /**
         * Returns an optional help text that can provide useful hints on how to configure the coverage tool so that the
         * report files could be parsed by Jenkins. This help can be a plain text message or an HTML snippet.
         *
         * @return the help
         */
        public String getHelp() {
            return StringUtils.EMPTY;
        }

        /**
         * Returns an optional URL to the homepage of the coverage tool.
         *
         * @return the help
         */
        public String getUrl() {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Supported coverage parsers.
     */
    public enum Parser {
        COBERTURA(Messages._Parser_Cobertura(), "**/cobertura.xml",
                "symbol-footsteps-outline plugin-ionicons-api"),
        JACOCO(Messages._Parser_JaCoCo(), "**/jacoco.xml",
                "symbol-footsteps-outline plugin-ionicons-api"),
        PIT(Messages._Parser_PIT(), "**/mutations.xml",
                "symbol-solid/virus-slash plugin-font-awesome-api");

        private final Localizable displayName;
        private final String defaultPattern;
        private final String icon;

        Parser(final Localizable displayName, final String defaultPattern,
                final String icon) {
            this.displayName = displayName;
            this.defaultPattern = defaultPattern;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName.toString();
        }

        public String getDefaultPattern() {
            return defaultPattern;
        }

        public String getIcon() {
            return icon;
        }

        /**
         * Creates a new parser to read the report XML files into a Java object model of {@link Node} instances.
         *
         * @return the parser
         */
        public CoverageParser createParser() {
            return new ParserRegistry().getParser(name());
        }
    }
}
