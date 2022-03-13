package io.jenkins.plugins.coverage.model.visualization.code;

import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

import io.jenkins.plugins.prism.SourceCodeRetention;

/**
 * Wraps the properties which are required for processing source code files.
 *
 * @author Florian Orendi
 */
public class SourceCodeProperties {

    private final Set<String> requestedSourceDirectories;
    private final String sourceCodeEncoding;
    private final SourceCodeRetention sourceCodeRetention;

    /**
     * Constructor which initializes the required properties.
     *
     * @param requestedSourceDirectories
     *         the source directories that have been configured in the associated job
     * @param sourceCodeEncoding
     *         the encoding of the source code files
     * @param sourceCodeRetention
     *         the source code retention strategy
     */
    public SourceCodeProperties(@NonNull final Set<String> requestedSourceDirectories,
            @NonNull final String sourceCodeEncoding,
            @NonNull final SourceCodeRetention sourceCodeRetention) {
        this.requestedSourceDirectories = requestedSourceDirectories;
        this.sourceCodeEncoding = sourceCodeEncoding;
        this.sourceCodeRetention = sourceCodeRetention;
    }

    public Set<String> getRequestedSourceDirectories() {
        return requestedSourceDirectories;
    }

    public String getSourceCodeEncoding() {
        return sourceCodeEncoding;
    }

    public SourceCodeRetention getSourceCodeRetention() {
        return sourceCodeRetention;
    }
}
