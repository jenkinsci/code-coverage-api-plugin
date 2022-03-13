package io.jenkins.plugins.coverage.model;

import java.io.ObjectStreamException;
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
    private SortedMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>();
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

    /**
     * Called after de-serialization to retain backward compatibility.
     *
     * @return this
     * @throws ObjectStreamException
     *         if the operation failed
     */
    protected Object readResolve() throws ObjectStreamException {
        if (indirectCoverageChanges == null) {
            indirectCoverageChanges = new TreeMap<>();
        }
        if (changedCodeLines == null) {
            changedCodeLines = new TreeSet<>();
        }
        if (coveragePerLine == null) {
            coveragePerLine = new TreeMap<>();
        }
        return this;
    }

    /**
     * Adds a code line that has been changed.
     *
     * @param line
     *         The changed code line
     */
    public void addChangedCodeLine(final int line) {
        changedCodeLines.add(line);
    }

    /**
     * Adds the {@link Coverage} for a specific line of code.
     *
     * @param line
     *         The line
     * @param coverage
     *         The coverage
     */
    public void putCoveragePerLine(final Integer line, final Coverage coverage) {
        coveragePerLine.put(line, coverage);
    }

    /**
     * Adds an indirect coverage change for a specific line.
     *
     * @param line
     *         The line with the coverage change
     * @param hitsDelta
     *         The delta of the coverage hits before and after the code changes
     */
    public void putIndirectCoverageChange(final Integer line, final Integer hitsDelta) {
        indirectCoverageChanges.put(line, hitsDelta);
    }

    public void setChangedCodeLines(final SortedSet<Integer> changes) {
        changedCodeLines = changes;
    }

    public SortedSet<Integer> getChangedCodeLines() {
        return changedCodeLines;
    }

    public void setCoveragePerLine(final SortedMap<Integer, Coverage> coverage) {
        coveragePerLine = coverage;
    }

    public SortedMap<Integer, Coverage> getCoveragePerLine() {
        return coveragePerLine;
    }

    public SortedMap<Integer, Integer> getIndirectCoverageChanges() {
        return indirectCoverageChanges;
    }

    public void setIndirectCoverageChanges(final SortedMap<Integer, Integer> changes) {
        indirectCoverageChanges = changes;
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
        copy.setIndirectCoverageChanges(new TreeMap<>(indirectCoverageChanges));

        return copy;
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
                && Objects.equals(indirectCoverageChanges, that.indirectCoverageChanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourcePath, coveragePerLine, changedCodeLines, indirectCoverageChanges);
    }
}
