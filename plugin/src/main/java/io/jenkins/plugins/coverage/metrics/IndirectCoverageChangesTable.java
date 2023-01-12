package io.jenkins.plugins.coverage.metrics;

import java.util.Locale;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Node;

import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;

/**
 * A coverage table model that handles the lines of code that have been indirectly changed with respect to a result of a
 * reference build.
 */
class IndirectCoverageChangesTable extends ChangesTableModel {
    IndirectCoverageChangesTable(final String id, final Node root, final Node changeRoot,
            final RowRenderer renderer, final ColorProvider colorProvider) {
        super(id, root, changeRoot, renderer, colorProvider);
    }

    @Override
    IndirectCoverageChangesRow createRow(final FileNode file, final Locale browserLocale) {
        return new IndirectCoverageChangesRow(
                getOriginalNode(file), file, browserLocale, getRenderer(), getColorProvider());
    }

    /**
     *  UI row model for the coverage details table of the indirect coverage changes.
     */
    private static class IndirectCoverageChangesRow extends ChangesRow {
        IndirectCoverageChangesRow(final FileNode originalFile, final FileNode changedFileNode,
                final Locale browserLocale, final RowRenderer renderer, final ColorProvider colorProvider) {
            super(originalFile, changedFileNode, browserLocale, renderer, colorProvider);
        }

        @Override
        public int getLoc() {
            return getOriginalFile().getIndirectCoverageChanges().size();
        }
    }
}
