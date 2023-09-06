package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.List;
import java.util.Objects;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Model class containing data pertaining to files with modified lines of code. Each object contains a relative file
 * path and a list of {@link ModifiedLinesBlock} objects.
 */
@ExportedBean
public class FileWithModifiedLines {
    private final String relativePath;
    private final List<ModifiedLinesBlock> listOfModifiedLines;

    FileWithModifiedLines(final String relativePath, final List<ModifiedLinesBlock> listOfModifiedLines) {
        this.relativePath = relativePath;
        this.listOfModifiedLines = listOfModifiedLines;
    }

    @Exported(inline = true)
    public String getRelativePath() {
        return relativePath;
    }

    @Exported(inline = true)
    public List<ModifiedLinesBlock> getListOfModifiedLines() {
        return listOfModifiedLines;
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
        return Objects.equals(this.getRelativePath(), file.getRelativePath())
                && Objects.equals(this.getListOfModifiedLines(), file.getListOfModifiedLines());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRelativePath(), getListOfModifiedLines());
    }
}
