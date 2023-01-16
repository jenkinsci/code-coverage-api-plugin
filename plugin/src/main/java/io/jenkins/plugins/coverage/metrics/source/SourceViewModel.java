package io.jenkins.plugins.coverage.metrics.source;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Node;

import hudson.model.ModelObject;
import hudson.model.Run;

/**
 * Server side model that provides the data for the source code view of the coverage results. The layout of the
 * associated view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 */
public class SourceViewModel implements ModelObject {
    private static final SourceCodeFacade SOURCE_CODE_FACADE = new SourceCodeFacade();

    private final Run<?, ?> owner;
    private final String id;
    private final FileNode fileNode;

    /**
     * Creates a new source view model instance.
     *
     * @param owner
     *         the owner of this view
     * @param id
     *         the ID that is used to store the coverage sources
     * @param fileNode
     *         the selected file node of the coverage tree
     */
    public SourceViewModel(final Run<?, ?> owner, final String id, final FileNode fileNode) {
        this.owner = owner;
        this.id = id;
        this.fileNode = fileNode;
    }

    public Run<?, ?> getOwner() {
        return owner;
    }

    public Node getNode() {
        return fileNode;
    }


    /**
     * Returns the source file rendered in HTML.
     *
     * @return the colored source code as HTML document
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getSourceFileContent() {
        try {
            return SOURCE_CODE_FACADE.read(getOwner().getRootDir(), id, getNode().getPath());
        }
        catch (IOException | InterruptedException exception) {
            return ExceptionUtils.getStackTrace(exception);
        }
    }

    /**
     * Returns whether the source file is available in Jenkins build folder.
     *
     * @param coverageNode
     *         The {@link Node} which is checked if there is a source file available
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    @SuppressWarnings("unused") // Called by jelly view
    public boolean isSourceFileAvailable(final Node coverageNode) {
        return SOURCE_CODE_FACADE.createFileInBuildFolder(getOwner().getRootDir(), id, coverageNode.getPath()).canRead();
    }

    @Override
    public String getDisplayName() {
        return Messages.Coverage_Title(getNode().getName());
    }
}
