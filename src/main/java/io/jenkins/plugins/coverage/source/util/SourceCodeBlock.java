package io.jenkins.plugins.coverage.source.util;

import java.io.Serializable;

public class SourceCodeBlock implements Serializable {
    private static final long serialVersionUID = -6747494923590803118L;

    /**
     * the changed code block's start line number.
     */
    private final long startLine;

    /**
     * the changed code block's end line number.
     */
    private final long endLine;

    private SourceCodeBlock(long startLine, long endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public static SourceCodeBlock of(int start, int end) {
        return new SourceCodeBlock(start, end);
    }

    public long getStartLine() {
        return startLine;
    }

    public long getEndLine() {
        return endLine;
    }
}
