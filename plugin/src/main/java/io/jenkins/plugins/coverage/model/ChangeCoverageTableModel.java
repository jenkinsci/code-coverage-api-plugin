package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import hudson.Functions;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.datatables.DetailedCell;
import io.jenkins.plugins.datatables.TableConfiguration;
import io.jenkins.plugins.datatables.TableConfiguration.SelectStyle;

/**
 * {@link CoverageTableModel} implementation for visualizing the change coverage.
 *
 * @since 3.0.0
 */
class ChangeCoverageTableModel extends CoverageTableModel {
    private final CoverageNode changeRoot;

    /**
     * Creates a change coverage table model.
     *
     * @param id
     *         The ID of the table
     * @param root
     *         The root of the origin coverage tree
     * @param changeRoot
     *         The root of the change coverage tree
     * @param renderer
     *         the renderer to use for the file names
     * @param colorProvider
     *         The {@link ColorProvider} which provides the used colors
     */
    ChangeCoverageTableModel(final String id, final CoverageNode root, final CoverageNode changeRoot,
            final RowRenderer renderer, final ColorProvider colorProvider) {
        super(id, root, renderer, colorProvider);

        this.changeRoot = changeRoot;
    }

    @Override
    public TableConfiguration getTableConfiguration() {
        return super.getTableConfiguration().select(SelectStyle.SINGLE);
    }

    @Override
    public List<Object> getRows() {
        Locale browserLocale = Functions.getCurrentLocale();
        return changeRoot.getAllFileCoverageNodes().stream()
                .map(file -> new ChangeCoverageRow(
                        getOriginalNode(file), file, browserLocale, getRenderer(), colorProvider))
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
     * UI row model for the change coverage details table.
     *
     * @since 3.0.0
     */
    private static class ChangeCoverageRow extends CoverageRow {
        private final FileCoverageNode changedFileNode;

        ChangeCoverageRow(final FileCoverageNode root, final FileCoverageNode changedFileNode,
                final Locale browserLocale, final RowRenderer renderer, final ColorProvider colorProvider) {
            super(root, browserLocale, renderer, colorProvider);

            this.changedFileNode = changedFileNode;
        }

        @Override
        public DetailedCell<?> getLineCoverage() {
            Coverage coverage = changedFileNode.getCoverage(CoverageMetric.LINE);
            return createColoredCoverageColumn(coverage, "The line change coverage");
        }

        @Override
        public DetailedCell<?> getBranchCoverage() {
            Coverage coverage = changedFileNode.getCoverage(CoverageMetric.BRANCH);
            return createColoredCoverageColumn(coverage, "The branch change coverage");
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
            return (int) changedFileNode.getChangedCodeLines().stream()
                    .filter(line -> changedFileNode.getCoveragePerLine().containsKey(line))
                    .count();
        }

        private DetailedCell<?> createColoredChangeCoverageDeltaColumn(final CoverageMetric coverageMetric) {
            Coverage changeCoverage = changedFileNode.getCoverage(coverageMetric);
            if (changeCoverage.isSet()) {
                Fraction delta = changeCoverage.getCoveredFraction()
                        .subtract(getRoot().getCoverage(coverageMetric).getCoveredFraction());
                return createColoredCoverageDeltaColumn(CoveragePercentage.valueOf(delta),
                        "The change coverage within the file against the total file coverage");
            }
            return NO_COVERAGE;
        }
    }
}
