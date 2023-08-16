package io.jenkins.plugins.coverage.metrics.model;

import java.util.Objects;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Model class containing data pertained to consecutive lines of modified code. Each object possesses a starting and ending
 * line number and the type of coverage of the block. Each object is associated with a FileWithModifiedLines object.
 */
@ExportedBean
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

    @Exported
    public int getStartLine() {
        return startLine;
    }

    @Exported
    public int getEndLine() {
        return endLine;
    }

    @Exported
    public LineCoverageType getType() {
        return type;
    }

}
