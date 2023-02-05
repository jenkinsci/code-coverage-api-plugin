package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import hudson.Functions;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.datatables.DetailedCell;
import io.jenkins.plugins.datatables.TableConfiguration;
import io.jenkins.plugins.datatables.TableConfiguration.SelectStyle;

/**
 * {@link CoverageTableModel} implementation for visualizing the changed files coverage.
 *
 * @since 4.0.0
 */
class ChangedFilesCoverageTable extends CoverageTableModel {
    private final Node changedFilesCoverageRoot;

    /**
     * Creates a changed files coverage table model.
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
    ChangedFilesCoverageTable(final String id, final Node root, final Node changeRoot,
            final RowRenderer renderer, final ColorProvider colorProvider) {
        super(id, root, renderer, colorProvider);

        this.changedFilesCoverageRoot = changeRoot;
    }

    @Override
    public TableConfiguration getTableConfiguration() {
        return super.getTableConfiguration().select(SelectStyle.SINGLE);
    }

    @Override
    public List<Object> getRows() {
        Locale browserLocale = Functions.getCurrentLocale();
        return changedFilesCoverageRoot.getAllFileNodes().stream()
                .map(file -> new ChangedFilesCoverageRow(
                        getOriginalNode(file), file, browserLocale, getRenderer(), getColorProvider()))
                .collect(Collectors.toList());
    }

    private FileNode getOriginalNode(final FileNode fileNode) {
        return getRoot().getAllFileNodes().stream()
                .filter(node -> node.getPath().equals(fileNode.getPath())
                        && node.getName().equals(fileNode.getName()))
                .findFirst()
                .orElse(fileNode); // return this as fallback to prevent exceptions
    }

    /**
     * UI row model for the changed files coverage details table.
     *
     * @since 4.0.0
     */
    private static class ChangedFilesCoverageRow extends CoverageRow {
        private final FileNode originalFile;

        ChangedFilesCoverageRow(final FileNode originalFile, final FileNode changedFileNode,
                final Locale browserLocale, final RowRenderer renderer, final ColorProvider colorProvider) {
            super(changedFileNode, browserLocale, renderer, colorProvider);

            this.originalFile = originalFile;
        }

        @Override
        public DetailedCell<?> getLineCoverageDelta() {
            return createColoredChangeCoverageDeltaColumn(Metric.LINE);
        }

        @Override
        public DetailedCell<?> getBranchCoverageDelta() {
            return createColoredChangeCoverageDeltaColumn(Metric.BRANCH);
        }

        @Override
        public int getLoc() {
            return getFile().getCoveredLines().size();
        }

        private DetailedCell<?> createColoredChangeCoverageDeltaColumn(final Metric metric) {
            Coverage fileCoverage = getFile().getTypedValue(metric, Coverage.nullObject(metric));
            if (fileCoverage.isSet()) {
                return createColoredCoverageDeltaColumn(metric,
                        fileCoverage.delta(originalFile.getTypedValue(metric, Coverage.nullObject(metric))));
            }
            return NO_COVERAGE;
        }
    }
}
