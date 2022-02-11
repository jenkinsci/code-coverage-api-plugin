package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import io.jenkins.plugins.coverage.model.SourceCodeFacade.AgentCoveragePainter;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.reference.ReferenceFinder;
import io.jenkins.plugins.prism.PermittedSourceCodeDirectory;
import io.jenkins.plugins.prism.PrismConfiguration;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.LogHandler;

/**
 * Transforms the old model to the new model and invokes all steps that work on the new model. Currently, only the
 * source code painting and copying has been moved to this new reporter class.
 *
 * @author Ullrich Hafner
 */
public class CoverageReporter {
    /**
     * Transforms the old model to the new model and invokes all steps that work on the new model. In the final step, a
     * new {@link CoverageBuildAction} will be attached to the build.
     *
     * @param rootResult
     *         the root result obtained from the old coverage API
     * @param build
     *         the build that owns these results
     * @param workspace
     *         the workspace on the agent that provides access to the source code files
     * @param listener
     *         logger
     * @param requestedSourceDirectories
     *         the source directories that have been configured in the associated job
     * @param sourceCodeEncoding
     *         the encoding of the source code files
     * @param sourceCodeRetention
     *         the source code retention strategy
     * @param healthReport
     *         health report
     *
     * @throws InterruptedException
     *         if the build has been aborted
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public void run(final CoverageResult rootResult, final Run<?, ?> build, final FilePath workspace,
            final TaskListener listener, final Set<String> requestedSourceDirectories, final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention, final HealthReport healthReport)
            throws InterruptedException {
        LogHandler logHandler = new LogHandler(listener, "Coverage");
        FilteredLog log = new FilteredLog("Errors while reporting code coverage results:");

        rootResult.stripGroup();

        CoverageNodeConverter converter = new CoverageNodeConverter();
        CoverageNode rootNode = converter.convert(rootResult);
        rootNode.splitPackages();

        SourceCodeFacade sourceCodeFacade = new SourceCodeFacade();
        if (sourceCodeRetention != SourceCodeRetention.NEVER) {
            Set<Entry<CoverageNode, CoveragePaint>> paintedFiles = converter.getPaintedFiles();
            log.logInfo("Painting %d source files on agent", paintedFiles.size());
            logHandler.log(log);

            paintFilesOnAgent(workspace, paintedFiles, requestedSourceDirectories, sourceCodeEncoding, log);
            log.logInfo("Copying painted sources from agent to build folder");
            logHandler.log(log);

            sourceCodeFacade.copySourcesToBuildFolder(build, workspace, log);
            logHandler.log(log);
        }
        sourceCodeRetention.cleanup(build, sourceCodeFacade.getCoverageSourcesDirectory(), log);

        logHandler.log(log);

        Optional<CoverageBuildAction> possibleReferenceResult = getReferenceBuildAction(build, log);

        logHandler.log(log);

        CoverageBuildAction action;
        if (possibleReferenceResult.isPresent()) {
            CoverageBuildAction referenceAction = possibleReferenceResult.get();
            SortedMap<CoverageMetric, Fraction> delta = rootNode.computeDelta(referenceAction.getResult());

            action = new CoverageBuildAction(build, rootNode, healthReport, referenceAction.getOwner().getExternalizableId(), delta);
        }
        else {
            action = new CoverageBuildAction(build, rootNode, healthReport);
        }
        build.addOrReplaceAction(action);
    }

    private void paintFilesOnAgent(final FilePath workspace, final Set<Entry<CoverageNode, CoveragePaint>> paintedFiles,
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
                            sourceCodeEncoding, "coverage"));
            log.merge(agentLog);
        }
        catch (IOException exception) {
            log.logException(exception, "Can't paint and zip sources on the agent");
        }
    }

    private Optional<CoverageBuildAction> getReferenceBuildAction(final Run<?, ?> build, final FilteredLog log) {
        log.logInfo("Obtaining action of reference build");

        ReferenceFinder referenceFinder = new ReferenceFinder();
        Optional<Run<?, ?>> reference = referenceFinder.findReference(build, log);

        Optional<CoverageBuildAction> previousResult;
        if (reference.isPresent()) {
            Run<?, ?> referenceBuild = reference.get();
            log.logInfo("-> Using reference build '%s'", referenceBuild);
            previousResult = getPreviousResult(reference.get());
            if (previousResult.isPresent()) {
                Run<?, ?> fallbackBuild = previousResult.get().getOwner();
                if (!fallbackBuild.equals(referenceBuild)) {
                    log.logInfo("-> Reference build has no action, falling back to last build with action: '%s'",
                            fallbackBuild.getDisplayName());
                }
            }
        }
        else {
            previousResult = getPreviousResult(build.getPreviousBuild());
            previousResult.ifPresent(coverageBuildAction ->
                    log.logInfo("-> No reference build defined, falling back to previous build: '%s'",
                            coverageBuildAction.getOwner().getDisplayName()));
        }

        if (!previousResult.isPresent()) {
            log.logInfo("-> Found no reference result in reference build");

            return Optional.empty();
        }

        CoverageBuildAction referenceAction = previousResult.get();
        log.logInfo("-> Found reference result in build '%s'", referenceAction.getOwner().getDisplayName());

        return Optional.of(referenceAction);
    }

    private Optional<CoverageBuildAction> getPreviousResult(@CheckForNull final Run<?, ?> startSearch) {
        for (Run<?, ?> build = startSearch; build != null; build = build.getPreviousBuild()) {
            CoverageBuildAction action = build.getAction(CoverageBuildAction.class);
            if (action != null) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }
}
