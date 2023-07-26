package io.jenkins.plugins.coverage.metrics.model;

public class ChangedLinesModel {
    private int startLine;
    private int endLine;
    private Type type;


    public ChangedLinesModel(final int start_line, final int end_line, final Type type) {
        this.setStartLine(start_line);
        this.setEndLine(end_line);
        this.setType(type);
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

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }
}
