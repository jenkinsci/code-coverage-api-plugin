package io.jenkins.plugins.coverage.source.code;

import java.io.Serializable;
import java.util.List;

public class SourceCodeFile implements Serializable {
    private static final long serialVersionUID = -6747494923590803118L;

    /**
     * source code file path.
     */
    private final String path;

    /**
     * changed code block list.
     */
    private final List<SourceCodeBlock> blocks;

    public SourceCodeFile(String path, List<SourceCodeBlock> blocks) {
        this.path = path;
        this.blocks = blocks;
    }

    public String getPath() {
        return path;
    }

    public List<SourceCodeBlock> getBlocks() {
        return blocks;
    }
}
