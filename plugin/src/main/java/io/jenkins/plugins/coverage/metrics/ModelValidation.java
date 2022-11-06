package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

import io.jenkins.plugins.util.JenkinsFacade;

/**
 * Validates all properties of a configuration of a static analysis tool in a job.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
public class ModelValidation {
    private static final Set<String> ALL_CHARSETS = Charset.availableCharsets().keySet();
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("\\p{Alnum}[\\p{Alnum}-_.]*");

    @VisibleForTesting
    static final String NO_REFERENCE_JOB = "-";

    private final JenkinsFacade jenkins;

    /** Creates a new descriptor. */
    public ModelValidation() {
        this(new JenkinsFacade());
    }

    @VisibleForTesting
    ModelValidation(final JenkinsFacade jenkins) {
        super();

        this.jenkins = jenkins;
    }

    /**
     * Returns all available character set names.
     *
     * @return all available character set names
     * @deprecated moved to Prism API Plugin
     */
    @Deprecated
    public ComboBoxModel getAllCharsets() {
        return new ComboBoxModel(ALL_CHARSETS);
    }

    /**
     * Returns the default charset for the specified encoding string. If the default encoding is empty or {@code null},
     * or if the charset is not valid then the default encoding of the platform is returned.
     *
     * @param charset
     *         identifier of the character set
     *
     * @return the default charset for the specified encoding string
     * @deprecated moved to Prism API Plugin
     */
    @Deprecated
    public Charset getCharset(@CheckForNull final String charset) {
        try {
            if (StringUtils.isNotBlank(charset)) {
                return Charset.forName(charset);
            }
        }
        catch (UnsupportedCharsetException | IllegalCharsetNameException exception) {
            // ignore and return default
        }
        return Charset.defaultCharset();
    }

    /**
     * Ensures that the specified ID is valid.
     *
     * @param id
     *         the custom ID of the tool
     *
     * @throws IllegalArgumentException
     *         if the ID is not valid
     */
    public void ensureValidId(final String id) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("INVALID ID TODO");
        }
    }

    /**
     * Performs on-the-fly validation of the ID.
     *
     * @param id
     *         the custom ID of the tool
     *
     * @return the validation result
     */
    public FormValidation validateId(final String id) {
        if (isValidId(id)) {
            return FormValidation.ok();
        }
        return FormValidation.error("FIXME ERROR");
    }

    private boolean isValidId(final String id) {
        return StringUtils.isEmpty(id) || VALID_ID_PATTERN.matcher(id).matches();
    }

    /**
     * Performs on-the-fly validation of the character encoding.
     *
     * @param reportEncoding
     *         the character encoding
     *
     * @return the validation result
     * @deprecated moved to Prism API Plugin
     */
    @Deprecated
    public FormValidation validateCharset(final String reportEncoding) {
        try {
            if (StringUtils.isBlank(reportEncoding) || Charset.isSupported(reportEncoding)) {
                return FormValidation.ok();
            }
        }
        catch (IllegalCharsetNameException | UnsupportedCharsetException ignore) {
            // throw a FormValidation error
        }
        return FormValidation.errorWithMarkup(createWrongEncodingErrorMessage());
    }

    @VisibleForTesting
    static String createWrongEncodingErrorMessage() {
        return "FIXME";
    }

    /**
     * Performs on-the-fly validation on the ant pattern for input files.
     *
     * @param project
     *         the project that is configured
     * @param pattern
     *         the file pattern
     *
     * @return the validation result
     */
    public FormValidation doCheckPattern(final AbstractProject<?, ?> project, final String pattern) {
        if (project != null) { // there is no workspace in pipelines
            try {
                FilePath workspace = project.getSomeWorkspace();
                if (workspace != null && workspace.exists()) {
                    return validatePatternInWorkspace(pattern, workspace);
                }
            }
            catch (InterruptedException | IOException ignore) {
                // ignore and return ok
            }
        }

        return FormValidation.ok();
    }

    private FormValidation validatePatternInWorkspace(final String pattern, final FilePath workspace)
            throws IOException, InterruptedException {
        String result = workspace.validateAntFileMask(pattern, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
        if (result != null) {
            return FormValidation.error(result);
        }
        return FormValidation.ok();
    }

    /**
     * Performs on-the-fly validation on the source code directory.
     *
     * @param project
     *         the project that is configured
     * @param sourceDirectory
     *         the file pattern
     *
     * @return the validation result
     * @deprecated moved to Prism API Plugin
     */
    @Deprecated
    public FormValidation doCheckSourceDirectory(@AncestorInPath final AbstractProject<?, ?> project,
            @QueryParameter final String sourceDirectory) {
        if (project != null) { // there is no workspace in pipelines
            try {
                FilePath workspace = project.getSomeWorkspace();
                if (workspace != null && workspace.exists()) {
                    return validateRelativePath(sourceDirectory, workspace);
                }
            }
            catch (InterruptedException | IOException ignore) {
                // ignore and return ok
            }
        }

        return FormValidation.ok();
    }

    private FormValidation validateRelativePath(
            @QueryParameter final String sourceDirectory, final FilePath workspace) throws IOException {
        PathUtil pathUtil = new PathUtil();
        if (pathUtil.isAbsolute(sourceDirectory)) {
            return FormValidation.ok();
        }
        return workspace.validateRelativeDirectory(sourceDirectory);
    }
}
