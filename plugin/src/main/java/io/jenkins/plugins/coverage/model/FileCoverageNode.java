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

    /**
     * The {@link Coverage} represents both line and branch coverage per line since it can be differentiated by the
     * total number of covered and missed cases and saves disk space.
     */
    private SortedMap<Integer, Coverage> coveragePerLine = new TreeMap<>(); // since 3.0.0
    private SortedMap<CoverageMetric, CoveragePercentage> fileCoverageDelta = new TreeMap<>(); // since 3.0.0
    private SortedMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>(); // since 3.0.0
    private SortedSet<Integer> changedCodeLines = new TreeSet<>(); // since 3.0.0

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
    @Override
    protected Object readResolve() throws ObjectStreamException {
        super.readResolve();
        if (fileCoverageDelta == null) {
            fileCoverageDelta = new TreeMap<>();
        }
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
     * Checks whether the file coverage delta exists for the passed {@link CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return {@code true} whether the coverage delta exists, else {@code false}
     */
    public boolean hasFileCoverageDelta(final CoverageMetric coverageMetric) {
        return fileCoverageDelta.containsKey(coverageMetric);
    }

    /**
     * Gets the file coverage delta for the passed {@link CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     *
     * @return the file coverage delta as percentage
     */
    public CoveragePercentage getFileCoverageDeltaForMetric(final CoverageMetric coverageMetric) {
        return fileCoverageDelta.get(coverageMetric);
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
     * Adds a {@link CoveragePercentage file coverage delta} of this file against a reference for the passed {@link
     * CoverageMetric}.
     *
     * @param coverageMetric
     *         The coverage metric
     * @param delta
     *         The coverage delta as percentage
     */
    public void putFileCoverageDelta(final CoverageMetric coverageMetric, final CoveragePercentage delta) {
        fileCoverageDelta.put(coverageMetric, delta);
    }

    /**
     * Adds the {@link Coverage} for a specific line of code.
     *
     * @param line
     *         The line
     * @param coverage
     *         The coverage
     */
    public void putCoveragePerLine(final int line, final Coverage coverage) {
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
    public void putIndirectCoverageChange(final int line, final int hitsDelta) {
        indirectCoverageChanges.put(line, hitsDelta);
    }

    public void setFileCoverageDelta(final SortedMap<CoverageMetric, CoveragePercentage> fileCoverageDelta) {
        this.fileCoverageDelta = fileCoverageDelta;
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
    protected FileCoverageNode copyTree(@CheckForNull final CoverageNode copiedParent) {
        CoverageNode copy = super.copyTree(copiedParent);

        FileCoverageNode fileCoverageNode = (FileCoverageNode) copy;
        fileCoverageNode.setCoveragePerLine(new TreeMap<>(coveragePerLine));
        fileCoverageNode.setChangedCodeLines(new TreeSet<>(changedCodeLines));
        fileCoverageNode.setIndirectCoverageChanges(new TreeMap<>(indirectCoverageChanges));
        fileCoverageNode.setFileCoverageDelta(new TreeMap<>(fileCoverageDelta));

        return fileCoverageNode;
    }

    @Override
    protected CoverageNode copyEmpty() {
        return new FileCoverageNode(getName(), sourcePath);
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
                && Objects.equals(fileCoverageDelta, that.fileCoverageDelta)
                && Objects.equals(coveragePerLine, that.coveragePerLine)
                && Objects.equals(changedCodeLines, that.changedCodeLines)
                && Objects.equals(indirectCoverageChanges, that.indirectCoverageChanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourcePath, fileCoverageDelta,
                coveragePerLine, changedCodeLines, indirectCoverageChanges);
    }
}
