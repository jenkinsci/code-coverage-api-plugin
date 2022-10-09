package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;

import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.FilteredLog;

/**
 * FIXME: comment class.
 *
 * @author Ullrich Hafner
 */
public class AggregatedResult implements Serializable {
    private final FilteredLog log;
    private final Node root;

    public AggregatedResult(final FilteredLog log, final Node root) {
        this.log = log;
        this.root = root;
    }

    public FilteredLog getLog() {
        return log;
    }

    public Node getRoot() {
        return root;
    }
}
