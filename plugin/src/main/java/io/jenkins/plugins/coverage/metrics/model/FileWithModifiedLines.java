package io.jenkins.plugins.coverage.metrics.model;

import java.util.List;
import java.util.Objects;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class FileWithModifiedLines {
    private final String relativePath;
    private final List<ModifiedLinesBlock> listOfModifiedLines;

    public FileWithModifiedLines(final String relativePath, final List<ModifiedLinesBlock> listOfModifiedLines) {
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
}
