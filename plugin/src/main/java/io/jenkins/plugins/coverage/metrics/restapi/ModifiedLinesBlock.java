package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.Objects;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Model class containing data pertaining to consecutive lines of modified code. Each object possesses a starting and
 * ending line number and the type of coverage of the block. Each object is associated with a
 * {@link FileWithModifiedLines} object. The class implements {@link Comparable} and is ordered by the start line.
 */
@ExportedBean
class ModifiedLinesBlock implements Comparable<ModifiedLinesBlock> {
    private final int startLine;
    private final int endLine;
    private final LineCoverageType type;

    ModifiedLinesBlock(final int startLine, final int endLine, final LineCoverageType type) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.type = type;
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

    @Override
    public int compareTo(final ModifiedLinesBlock other) {
        return this.startLine - other.startLine;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
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
}
