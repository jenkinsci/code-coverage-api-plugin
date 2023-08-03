package io.jenkins.plugins.coverage.metrics.model;

import java.util.Objects;

public class ModifiedLinesBlock {
    private final int startLine;
    private final int endLine;
    private final LineCoverageType type;

    public ModifiedLinesBlock(final int start_line, final int end_line, final LineCoverageType type) {
        this.startLine = start_line;
        this.endLine = end_line;
        this.type = type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModifiedLinesBlock)) {
            return false;
        }
        ModifiedLinesBlock that = (ModifiedLinesBlock) o;
        return getStartLine() == that.getStartLine() && getEndLine() == that.getEndLine()
                && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartLine(), getEndLine(), getType());
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public LineCoverageType getType() {
        return type;
    }

}
