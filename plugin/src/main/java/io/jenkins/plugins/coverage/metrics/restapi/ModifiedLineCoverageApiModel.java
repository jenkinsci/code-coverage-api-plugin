package io.jenkins.plugins.coverage.metrics.restapi;

import edu.hm.hafner.coverage.Node;

import hudson.model.Api;
import hudson.model.ModelObject;

import io.jenkins.plugins.coverage.metrics.source.Messages;

/**
 * Server side model that provides the data for modified lines coverage results.
 */
public class ModifiedLineCoverageApiModel implements ModelObject {
    private final Node node;

    /**
     * Creates a new instance of {@link ModifiedLineCoverageApiModel}
     *
     * @param node
     *         {@link Node} object
     */
    public ModifiedLineCoverageApiModel(final Node node) {
        this.node = node;
    }

    /**
     * Gets the remote API for  details of modified line coverage results.
     *
     * @return the remote API
     */
    public Api getApi() {
        return new Api(new ModifiedLineCoverageApi(getNode()));
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String getDisplayName() {
        return Messages.Coverage_Title(getNode().getName());
    }
}
