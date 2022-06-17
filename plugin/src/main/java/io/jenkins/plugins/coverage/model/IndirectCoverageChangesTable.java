package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import hudson.Functions;

import io.jenkins.plugins.datatables.DetailedCell;

/**
 * {@link CoverageTableModel} implementation for visualizing the indirect coverage changes.
 *
 * @since 3.0.0
 */
class IndirectCoverageChangesTable extends CoverageTableModel {
    private final CoverageNode changeRoot;

    /**
     * Creates an indirect coverage changes table model.
     *
     * @param id
     *         The ID of the table
     * @param root
     *         The root of the origin coverage tree
     * @param changeRoot
     *         The root of the indirect coverage changes tree
     * @param buildFolder
     *         the build folder to store the source code files
     * @param resultsId
     *         the ID of the results as prefix for the source code files in the build folder
     */
    IndirectCoverageChangesTable(final String id, final CoverageNode root, final CoverageNode changeRoot, final File buildFolder,
            final String resultsId) {
        super(id, root, buildFolder, resultsId);

        this.changeRoot = changeRoot;
    }

    @Override
    public List<Object> getRows() {
        Locale browserLocale = Functions.getCurrentLocale();
        return changeRoot.getAllFileCoverageNodes().stream()
                .map(file -> new IndirectCoverageChangesRow(getOriginalNode(file), file, getBuildFolder(), getResultsId(),
                        browserLocale))
                .collect(Collectors.toList());
    }

    private FileCoverageNode getOriginalNode(final FileCoverageNode fileNode) {
        Optional<FileCoverageNode> reference = getRoot().getAllFileCoverageNodes().stream()
                .filter(node -> node.getPath().equals(fileNode.getPath())
                        && node.getName().equals(fileNode.getName()))
                .findFirst();
        return reference.orElse(fileNode); // return this as fallback to prevent exceptions
    }

    /**
     * UI row model for the indirect coverage changes details table.
     *
     * @since 3.0.0
     */
    private static class IndirectCoverageChangesRow extends CoverageRow {
        private final FileCoverageNode changedFileNode;

        /**
         * Creates a table row for visualizing the indirect coverage changes of a file.
         *
         * @param root
         *         The unfiltered node which represents the coverage of the whole file
         * @param changedFileNode
         *         The filtered node which represents the indirect coverage changes only
         * @param buildFolder
         *         the build folder to store the source code files
         * @param resultsId
         *         the ID of the results as prefix for the source code files in the build folder
         * @param browserLocale
         *         The locale
         */
        IndirectCoverageChangesRow(final FileCoverageNode root, final FileCoverageNode changedFileNode,
                final File buildFolder, final String resultsId, final Locale browserLocale) {
            super(root, buildFolder, resultsId, browserLocale);

            this.changedFileNode = changedFileNode;
        }

        @Override
        public DetailedCell<?> getLineCoverage() {
            Coverage coverage = changedFileNode.getCoverage(CoverageMetric.LINE);
            return createColoredCoverageColumn(coverage, "The indirect line coverage changes");
        }

        @Override
        public DetailedCell<?> getBranchCoverage() {
            Coverage coverage = changedFileNode.getCoverage(CoverageMetric.BRANCH);
            return createColoredCoverageColumn(coverage, "The indirect branch coverage changes");
        }

        @Override
        public DetailedCell<?> getLineCoverageDelta() {
            return createColoredChangeCoverageDeltaColumn(CoverageMetric.LINE);
        }

        @Override
        public DetailedCell<?> getBranchCoverageDelta() {
            return createColoredChangeCoverageDeltaColumn(CoverageMetric.BRANCH);
        }

        @Override
        public int getLoc() {
            return changedFileNode.getIndirectCoverageChanges().size();
        }

        private DetailedCell<?> createColoredChangeCoverageDeltaColumn(
                final CoverageMetric coverageMetric) {
            Coverage changeCoverage = changedFileNode.getCoverage(coverageMetric);
            if (changeCoverage.isSet()) {
                Fraction delta = changeCoverage.getCoveredFraction()
                        .subtract(getRoot().getCoverage(coverageMetric).getCoveredFraction());
                return createColoredCoverageDeltaColumn(CoveragePercentage.valueOf(delta),
                        "The indirect coverage changes within the file against the total file coverage");
            }
            return NO_COVERAGE;
        }
    }
}
