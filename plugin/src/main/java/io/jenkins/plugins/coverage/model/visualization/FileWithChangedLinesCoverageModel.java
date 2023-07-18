package io.jenkins.plugins.coverage.model.visualization;

import java.util.List;

public class FileWithChangedLinesCoverageModel {
    private String relativePath;
    List<ChangedLinesModel> listOfChangedLines;

    public FileWithChangedLinesCoverageModel(final String relativePath) {
        this.setRelativePath(relativePath);
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(final String relativePath) {
        this.relativePath = relativePath;
    }

    public List<ChangedLinesModel> getListOfChangedLines() {
        return listOfChangedLines;
    }

    public void setListOfChangedLines(
            final List<ChangedLinesModel> listOfChangedLines) {
        this.listOfChangedLines = listOfChangedLines;
    }
}
