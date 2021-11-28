package io.jenkins.plugins.coverage.model;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A {@link CoverageNode} for a specific file. It stores the actual file name along the coverage information.
 *
 * @author Ullrich Hafner
 */
public class FileCoverageNode extends CoverageNode {
    private static final long serialVersionUID = -3795695377267542624L;

    private final String sourcePath;

    /**
     * Creates a new {@link FileCoverageNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     * @param sourcePath
     *         optional path to the source code of this node
     */
    public FileCoverageNode(final String name, @CheckForNull final String sourcePath) {
        super(CoverageMetric.FILE, name);

        this.sourcePath = StringUtils.defaultString(sourcePath);
    }

    @Override
    public String getPath() {
        return mergePath(sourcePath);
    }
}
