package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.Objects;
import java.util.SortedSet;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Model class that describes the {@link LineCoverageType coverage} for modified code lines of a file. The file is
 * described by its fully qualified name.
 */
@ExportedBean
class FileWithModifiedLines {
    private final String fullyQualifiedFileName;
    private final SortedSet<ModifiedLinesBlock> modifiedLinesBlocks;

    FileWithModifiedLines(final String fullyQualifiedFileName,
            final SortedSet<ModifiedLinesBlock> modifiedLinesBlocks) {
        this.fullyQualifiedFileName = fullyQualifiedFileName;
        this.modifiedLinesBlocks = modifiedLinesBlocks;
    }

    @Exported(inline = true)
    public String getFullyQualifiedFileName() {
        return fullyQualifiedFileName;
    }

    @Exported(inline = true)
    public SortedSet<ModifiedLinesBlock> getModifiedLinesBlocks() {
        return modifiedLinesBlocks;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileWithModifiedLines file = (FileWithModifiedLines) o;
        return Objects.equals(this.getFullyQualifiedFileName(), file.getFullyQualifiedFileName())
                && Objects.equals(this.getModifiedLinesBlocks(), file.getModifiedLinesBlocks());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullyQualifiedFileName(), getModifiedLinesBlocks());
    }
}
