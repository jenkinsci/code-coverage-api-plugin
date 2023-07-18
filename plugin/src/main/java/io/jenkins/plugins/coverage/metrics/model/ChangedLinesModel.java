package io.jenkins.plugins.coverage.metrics.model;

public class ChangedLinesModel {
    private int start_line;
    private int end_line;
    private Type type;


    public ChangedLinesModel(final int start_line, final int end_line, final Type type) {
        this.setStart_line(start_line);
        this.setEnd_line(end_line);
        this.setType(type);
    }

    public int getStart_line() {
        return start_line;
    }

    public void setStart_line(final int start_line) {
        this.start_line = start_line;
    }

    public int getEnd_line() {
        return end_line;
    }

    public void setEnd_line(final int end_line) {
        this.end_line = end_line;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }
}
