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
 * {@link CoverageTableModel} implementation for visualizing the modified lines coverage.
 *
 * @since 4.0.0
 */
class ModifiedFilesCoverageTable extends CoverageTableModel {
    private final Node modifiedFilesCoverageRoot;

    /**
     * Creates a modified lines coverage table model.
     *
     * @param id
     *         The ID of the table
     * @param root
     *         The root of the origin coverage tree
     * @param changeRoot
     *         The root of the Modified Lines Coverage tree
     * @param renderer
     *         the renderer to use for the file names
     * @param colorProvider
     *         The {@link ColorProvider} which provides the used colors
     */
    ModifiedFilesCoverageTable(final String id, final Node root, final Node changeRoot,
            final RowRenderer renderer, final ColorProvider colorProvider) {
        super(id, root, renderer, colorProvider);

        this.modifiedFilesCoverageRoot = changeRoot;
    }

    @Override
    public TableConfiguration getTableConfiguration() {
        return super.getTableConfiguration().select(SelectStyle.SINGLE);
    }

    @Override
    public List<Object> getRows() {
        Locale browserLocale = Functions.getCurrentLocale();
        return modifiedFilesCoverageRoot.getAllFileNodes().stream()
                .map(file -> new ModifiedFilesCoverageRow(
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
     * UI row model for the modified lines coverage details table.
     *
     * @since 4.0.0
     */
    private static class ModifiedFilesCoverageRow extends CoverageRow {
        private final FileNode originalFile;

        ModifiedFilesCoverageRow(final FileNode originalFile, final FileNode changedFileNode,
                final Locale browserLocale, final RowRenderer renderer, final ColorProvider colorProvider) {
            super(changedFileNode, browserLocale, renderer, colorProvider);

            this.originalFile = originalFile;
        }

        @Override
        public DetailedCell<?> getLineCoverageDelta() {
            return createColoredModifiedLinesCoverageDeltaColumn(Metric.LINE);
        }

        @Override
        public DetailedCell<?> getBranchCoverageDelta() {
            return createColoredModifiedLinesCoverageDeltaColumn(Metric.BRANCH);
        }

        @Override
        public int getLoc() {
            return getFile().getLinesWithCoverage().size();
        }

        private DetailedCell<?> createColoredModifiedLinesCoverageDeltaColumn(final Metric metric) {
            Coverage fileCoverage = getFile().getTypedValue(metric, Coverage.nullObject(metric));
            if (fileCoverage.isSet()) {
                return createColoredCoverageDeltaColumn(metric,
                        fileCoverage.delta(originalFile.getTypedValue(metric, Coverage.nullObject(metric))));
            }
            return NO_COVERAGE;
        }
    }
}
