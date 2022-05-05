package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.VisibleForTesting;

import org.jenkinsci.Symbol;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Run;
import jenkins.security.MasterToSlaveCallable;

import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.LogHandler;

/**
 * A coverage tool that can produce a {@link CoverageNode coverage tree} by parsing a given report file.
 *
 * @author Ullrich Hafner
 */
public abstract class CoverageTool extends AbstractDescribableImpl<CoverageTool> implements Serializable {
    private JenkinsFacade jenkins = new JenkinsFacade();

    @VisibleForTesting
    public void setJenkinsFacade(final JenkinsFacade jenkinsFacade) {
        this.jenkins = jenkinsFacade;
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
     * Returns the {@link Symbol} name of this tool.
     *
     * @return the name of this tool, or "undefined" if no symbol has been defined
     */
    public String getSymbolName() {
        return getDescriptor().getSymbolName();
    }

    @Override
    public CoverageToolDescriptor getDescriptor() {
        return (CoverageToolDescriptor) jenkins.getDescriptorOrDie(getClass());
    }

    /**
     * Scans the results of a build for issues. This method is invoked on Jenkins master. I.e., if a tool wants to
     * process some build results it is required to run a {@link MasterToSlaveCallable}.
     *
     * @param run
     *         the build
     * @param workspace
     *         the workspace of the build
     * @param sourceCodeEncoding
     *         the encoding to use to read source files
     * @param logger
     *         the logger
     *
     * @return the created report
     * @throws ParsingException
     *         Signals that during parsing a non-recoverable error has been occurred
     * @throws ParsingCanceledException
     *         Signals that the parsing has been aborted by the user
     */
    public abstract CoverageNode parse(Run<?, ?> run, FilePath workspace, Charset sourceCodeEncoding, LogHandler logger)
            throws ParsingException, ParsingCanceledException;

    /** Descriptor for {@link CoverageTool}. **/
    public abstract static class CoverageToolDescriptor extends Descriptor<CoverageTool> {
        /**
         * Creates a new instance of {@link CoverageToolDescriptor}.
         */
        protected CoverageToolDescriptor() {
            super();
        }

        /**
         * Returns the {@link Symbol} name of this tool.
         *
         * @return the name of this tool, or "undefined" if no symbol has been defined
         */
        public String getSymbolName() {
            Symbol annotation = getClass().getAnnotation(Symbol.class);

            if (annotation != null) {
                String[] symbols = annotation.value();
                if (symbols.length > 0) {
                    return symbols[0];
                }
            }
            return "unknownSymbol";
        }

        /**
         * Returns an optional help text that can provide useful hints on how to configure the coverage tool so
         * that the report files could be parsed by Jenkins. This help can be a plain text message or an HTML snippet.
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
}
