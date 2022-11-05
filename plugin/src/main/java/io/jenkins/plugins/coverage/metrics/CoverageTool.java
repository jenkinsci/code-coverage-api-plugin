package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.parser.CoberturaParser;
import edu.hm.hafner.metric.parser.JacocoParser;
import edu.hm.hafner.metric.parser.PitestParser;
import edu.hm.hafner.metric.parser.XmlParser;
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

import io.jenkins.plugins.coverage.model.ModelValidation;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.JenkinsFacade;

/**
 * A coverage tool that can produce a {@link Node coverage tree} by parsing a given report file.
 *
 * @author Ullrich Hafner
 */
public class CoverageTool extends AbstractDescribableImpl<CoverageTool> implements Serializable {
    private static final long serialVersionUID = -8612521458890553037L;

    private JenkinsFacade jenkins = new JenkinsFacade();

    private String id = StringUtils.EMPTY;
    private String name = StringUtils.EMPTY;
    private String pattern = StringUtils.EMPTY;
    private CoverageParser parser = CoverageParser.JACOCO;

    /**
     * Creates a new {@link CoverageTool}.
     */
    @DataBoundConstructor
    public CoverageTool() {
        // empty for stapler
    }

    public CoverageParser getParser() {
        return parser;
    }

    /**
     * Sets the parser to be used to read the input files.
     *
     * @param parser the parser to use
     */
    @DataBoundSetter
    public void setParser(final CoverageParser parser) {
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
     * Overrides the default ID of the results. The ID is used as URL of the results and as identifier in UI elements.
     * If no ID is given, then the default ID is used.
     *
     * @param id
     *         the ID of the results
     */
    @DataBoundSetter
    public void setId(final String id) {
        new ModelValidation().ensureValidId(id);

        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Returns the actual ID of the tool. If no user defined ID is given, then the default ID is returned.
     *
     * @return the ID
     * @see #setId(String)
     */
    public String getActualId() {
        return StringUtils.defaultIfBlank(getId(), getDescriptor().getId());
    }

    /**
     * Overrides the name of the results. The name is used for all labels in the UI. If no name is given, then the
     * default name is used.
     *
     * @param name
     *         the name of the results
     */
    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the actual name of the tool. If no user defined name is given, then the default name is returned.
     *
     * @return the name
     * @see #setName(String)
     */
    public String getActualName() {
        return StringUtils.defaultIfBlank(getName(), getParser().getDisplayName());
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
        return String.format("%s (pattern: %s)", getActualName(), getActualPattern());
    }

    @Override
    public CoverageToolDescriptor getDescriptor() {
        return (CoverageToolDescriptor) jenkins.getDescriptorOrDie(getClass());
    }

    /** Descriptor for {@link CoverageTool}. **/
    @Extension
    public static class CoverageToolDescriptor extends Descriptor<CoverageTool> {
        private final JenkinsFacade JENKINS = new JenkinsFacade();

        /**
         * Creates a new instance of {@link CoverageToolDescriptor}.
         */
        public CoverageToolDescriptor() {
            super();
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
                add(options, CoverageParser.JACOCO);
                add(options, CoverageParser.COBERTURA);
                add(options, CoverageParser.PIT);
                return options;
            }
            return new ListBoxModel();
        }

        private void add(final ListBoxModel options, final CoverageParser parser) {
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

            return new ModelValidation().validateId(id);
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
    public enum CoverageParser {
        COBERTURA(Messages._Parser_Cobertura(), "**/cobertura.xml", CoberturaParser::new),
        JACOCO(Messages._Parser_JaCoCo(), "**/jacoco.xml", JacocoParser::new),
        PIT(Messages._Parser_PIT(), "**/pit.xml", PitestParser::new);

        private final Localizable displayName;
        private final String defaultPattern;
        private final Supplier<XmlParser> parserSupplier;

        CoverageParser(final Localizable displayName, final String defaultPattern, final Supplier<XmlParser> parserSupplier) {
            this.displayName = displayName;
            this.defaultPattern = defaultPattern;
            this.parserSupplier = parserSupplier;
        }

        public String getDisplayName() {
            return displayName.toString();
        }

        public String getDefaultPattern() {
            return defaultPattern;
        }

        public XmlParser createParser() {
            return parserSupplier.get();
        }
    }
}
