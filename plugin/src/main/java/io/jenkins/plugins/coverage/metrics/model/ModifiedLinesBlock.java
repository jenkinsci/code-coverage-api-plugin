package io.jenkins.plugins.coverage.metrics.model;

import java.util.Objects;

public class ModifiedLinesBlock {
    private int startLine;
    private int endLine;
    private LineCoverageType type;

    public ModifiedLinesBlock(final int start_line, final int end_line, final LineCoverageType type) {
        this.setStartLine(start_line);
        this.setEndLine(end_line);
        this.setType(type);
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

    public void setStartLine(final int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(final int endLine) {
        this.endLine = endLine;
    }

    public LineCoverageType getType() {
        return type;
    }

    public void setType(final LineCoverageType type) {
        this.type = type;
    }
}
