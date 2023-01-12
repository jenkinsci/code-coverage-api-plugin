package io.jenkins.plugins.coverage.metrics.visualization.code;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.hm.hafner.metric.Node;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.FilePath;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.visualization.code.SourceCodeFacade.AgentCoveragePainter;
import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.prism.SourceCodeRetention;

/**
 * Processes the source code painting for highlighting code coverage.
 */
public class SourceCodePainter {
    private final Run<?, ?> build;
    private final FilePath workspace;
    private final String id;

    /**
     * Creates a painter for the passed build, using the passed properties.
     *
     * @param build
     *         The build which processes the source code
     * @param workspace
     *         The workspace which contains the source code files
     */
    public SourceCodePainter(@NonNull final Run<?, ?> build, @NonNull final FilePath workspace, final String id) {
        this.build = build;
        this.workspace = workspace;
        this.id = id;
    }

    /**
     * Processes the source code painting.
     *
     * @param node
     *         The files to be painted together with the information which lines has to be highlighted
     * @param sourceDirectories
     *         the source directories that have been configured in the associated job
     * @param sourceCodeEncoding
     *         the encoding of the source code files
     * @param sourceCodeRetention
     *         the source code retention strategy
     * @param log
     *         The log
     *
     * @throws InterruptedException
     *         if the painting process has been interrupted
     */
    public void processSourceCodePainting(final Node node,
            final Set<String> sourceDirectories, final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention, final FilteredLog log)
            throws InterruptedException {
        SourceCodeFacade sourceCodeFacade = new SourceCodeFacade();
        if (sourceCodeRetention != SourceCodeRetention.NEVER) {
            var files = node.getAllFileNodes().stream().map(SourceCodeFacade.PaintedNode::new).collect(Collectors.toList());
            log.logInfo("Painting %d source files on agent", files.size());

            paintFilesOnAgent(files, sourceDirectories, sourceCodeEncoding, log);
            log.logInfo("Copying painted sources from agent to build folder");

            sourceCodeFacade.copySourcesToBuildFolder(build, workspace, log);
        }
        sourceCodeRetention.cleanup(build, sourceCodeFacade.getCoverageSourcesDirectory(), log);
    }

    private void paintFilesOnAgent(final List<SourceCodeFacade.PaintedNode> paintedFiles,
            final Set<String> requestedSourceDirectories,
            final String sourceCodeEncoding, final FilteredLog log) throws InterruptedException {
        try {
            Set<String> permittedSourceDirectories = PrismConfiguration.getInstance()
                    .getSourceDirectories()
                    .stream()
                    .map(PermittedSourceCodeDirectory::getPath)
                    .collect(Collectors.toSet());

            FilteredLog agentLog = workspace.act(
                    new AgentCoveragePainter(paintedFiles, permittedSourceDirectories, requestedSourceDirectories,
                            sourceCodeEncoding, id));
            log.merge(agentLog);
        }
        catch (IOException exception) {
            log.logException(exception, "Can't paint and zip sources on the agent");
        }
    }
}
