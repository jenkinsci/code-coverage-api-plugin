package io.jenkins.plugins.coverage.metrics.model;

import java.util.List;
import java.util.Objects;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class FileWithModifiedLines {
    private String relativePath;
    List<ModifiedLinesBlock> listOfModifiedLines;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileWithModifiedLines)) {
            return false;
        }
        FileWithModifiedLines file = (FileWithModifiedLines) o;
        return Objects.equals(getRelativePath(), file.getRelativePath())
                //&& Objects.equals(getListOfChangedLines(), file.getListOfChangedLines());
                && getListOfModifiedLines().containsAll(file.getListOfModifiedLines());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRelativePath(), getListOfModifiedLines());
    }

    public FileWithModifiedLines(final String relativePath) {
        this.setRelativePath(relativePath);
    }

    @Exported(inline = true)
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(final String relativePath) {
        this.relativePath = relativePath;
    }

    @Exported(inline = true)
    public List<ModifiedLinesBlock> getListOfModifiedLines() {
        return listOfModifiedLines;
    }

    public void setListOfModifiedLines(
            final List<ModifiedLinesBlock> listOfModifiedLines) {
        this.listOfModifiedLines = listOfModifiedLines;
    }
}
