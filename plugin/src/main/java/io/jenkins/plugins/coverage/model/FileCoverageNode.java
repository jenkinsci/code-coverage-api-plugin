package io.jenkins.plugins.coverage.model;

import java.io.ObjectStreamException;
import java.util.Arrays;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A {@link CoverageNode} for a specific file. It stores the actual file name along the coverage information.
 *
 * @author Ullrich Hafner
 */
public class FileCoverageNode extends CoverageNode {
    private static final long serialVersionUID = -3795695377267542624L;

    private final String sourcePath;

    // new since 3.0.0
    private SortedMap<Integer, Coverage> coveragePerLine = new TreeMap<>();
    private SortedMap<Integer, Integer> unexpectedCoverageChanges = new TreeMap<>();
    private SortedSet<Integer> changedCodeLines = new TreeSet<>();

    /**
     * Creates a new {@link FileCoverageNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     * @param sourcePath
     *         optional path to the source code of this node
     */
    public FileCoverageNode(final String name, @CheckForNull final String sourcePath) {
        super(CoverageMetric.FILE, name);
        this.sourcePath = StringUtils.defaultString(sourcePath);
    }

    @Override
    public String getPath() {
        return mergePath(sourcePath);
    }

    public void setChangedCodeLines(final SortedSet<Integer> changes) {
        changedCodeLines = changes;
    }

    public void addChangedCodeLines(final Integer... changes) {
        changedCodeLines.addAll(Arrays.asList(changes));
    }

    public SortedSet<Integer> getChangedCodeLines() {
        return changedCodeLines;
    }

    public void setCoveragePerLine(final SortedMap<Integer, Coverage> coverage) {
        coveragePerLine = coverage;
    }

    public void putCoveragePerLine(final Integer line, final Coverage coverage) {
        coveragePerLine.put(line, coverage);
    }

    public SortedMap<Integer, Coverage> getCoveragePerLine() {
        return coveragePerLine;
    }

    public SortedMap<Integer, Integer> getUnexpectedCoverageChanges() {
        return unexpectedCoverageChanges;
    }

    public void putUnexpectedCoverageChange(final Integer line, final Integer hitsDelta) {
        unexpectedCoverageChanges.put(line, hitsDelta);
    }

    public void setUnexpectedCoverageChanges(final SortedMap<Integer, Integer> changes) {
        unexpectedCoverageChanges = changes;
    }

    @Override
    protected FileCoverageNode copyTree(final CoverageNode copiedParent) {
        FileCoverageNode copy = new FileCoverageNode(getName(), sourcePath);
        if (copiedParent != null) {
            copy.setParent(copiedParent);
        }

        copyChildrenAndLeaves(this, copy);

        SortedMap<Integer, Coverage> copiedCoverageDetails = new TreeMap<>();
        coveragePerLine.forEach((line, coverage) -> copiedCoverageDetails.put(line, coverage.copy()));
        copy.setCoveragePerLine(new TreeMap<>(copiedCoverageDetails));

        copy.setChangedCodeLines(new TreeSet<>(changedCodeLines));
        copy.setUnexpectedCoverageChanges(new TreeMap<>(unexpectedCoverageChanges));

        return copy;
    }

    protected Object readResolve() throws ObjectStreamException {
        if (unexpectedCoverageChanges == null) {
            unexpectedCoverageChanges = new TreeMap<>();
        }
        if (changedCodeLines == null) {
            changedCodeLines = new TreeSet<>();
        }
        if (coveragePerLine == null) {
            coveragePerLine = new TreeMap<>();
        }
        return this;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FileCoverageNode that = (FileCoverageNode) o;
        return Objects.equals(sourcePath, that.sourcePath)
                && Objects.equals(coveragePerLine, that.coveragePerLine)
                && Objects.equals(changedCodeLines, that.changedCodeLines)
                && Objects.equals(unexpectedCoverageChanges, that.unexpectedCoverageChanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourcePath, coveragePerLine, changedCodeLines, unexpectedCoverageChanges);
    }
}
