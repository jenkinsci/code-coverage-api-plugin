package io.jenkins.plugins.coverage.metrics.steps;

import java.util.Locale;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Node;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider;

/**
 * A coverage table model that handles the modified lines of a change with respect to a result of a reference build.
 */
class ChangeCoverageTableModel extends ChangesTableModel {
    ChangeCoverageTableModel(final String id, final Node root, final Node changeRoot,
            final RowRenderer renderer, final ColorProvider colorProvider) {
        super(id, root, changeRoot, renderer, colorProvider);
    }

    @Override
    ChangeCoverageRow createRow(final FileNode file, final Locale browserLocale) {
        return new ChangeCoverageRow(getOriginalNode(file), file,
                browserLocale, getRenderer(), getColorProvider());
    }

    /**
     * UI row model for the coverage details table of modified lines.
     */
    private static class ChangeCoverageRow extends ChangesRow {
        ChangeCoverageRow(final FileNode originalFile, final FileNode changedFileNode,
                final Locale browserLocale, final RowRenderer renderer, final ColorProvider colorProvider) {
            super(originalFile, changedFileNode, browserLocale, renderer, colorProvider);
        }

        @Override
        public int getLoc() {
            return getFile().getCoveredLinesOfChangeSet().size();
        }
    }
}
