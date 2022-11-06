package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;

import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.FilteredLog;

/**
 * Combines the root node of a coverage tree with the log that contains useful information gathered during parsing on an
 * agent.
 *
 * @author Ullrich Hafner
 */
class AggregatedResult implements Serializable {
    private static final long serialVersionUID = 2122230867938547733L;

    private final FilteredLog log;
    private final Node root;

    AggregatedResult(final FilteredLog log, final Node root) {
        this.log = log;
        this.root = root;
    }

    public FilteredLog getLog() {
        return log;
    }

    public Node getRoot() {
        return root;
    }

    public boolean hasErrors() {
        return !getLog().getErrorMessages().isEmpty();
    }
}
