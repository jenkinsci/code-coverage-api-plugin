package io.jenkins.plugins.coverage.metrics.model;

import java.util.List;
import java.util.Objects;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class FileWithChangedLinesCoverageModel {
    private String relativePath;
    List<ChangedLinesModel> listOfChangedLines;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileWithChangedLinesCoverageModel)) {
            return false;
        }
        FileWithChangedLinesCoverageModel file = (FileWithChangedLinesCoverageModel) o;
        return Objects.equals(getRelativePath(), file.getRelativePath())
                //&& Objects.equals(getListOfChangedLines(), file.getListOfChangedLines());
                && getListOfChangedLines().containsAll(file.getListOfChangedLines());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRelativePath(), getListOfChangedLines());
    }

    public FileWithChangedLinesCoverageModel(final String relativePath) {
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
    public List<ChangedLinesModel> getListOfChangedLines() {
        return listOfChangedLines;
    }

    public void setListOfChangedLines(
            final List<ChangedLinesModel> listOfChangedLines) {
        this.listOfChangedLines = listOfChangedLines;
    }
}
