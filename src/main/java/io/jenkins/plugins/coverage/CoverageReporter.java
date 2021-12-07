package io.jenkins.plugins.coverage;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

import edu.hm.hafner.util.FilteredLog;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.source.SourcePainter;
import io.jenkins.plugins.coverage.source.SourcePainter.AgentPainter;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.reference.ReferenceFinder;
import io.jenkins.plugins.util.LogHandler;

/**
 * FIXME: preliminary place for the reporter code.
 *
 * @author Ullrich Hafner
 */
public class CoverageReporter {
    void run(final CoverageResult rootResult, final Run<?, ?> build, final FilePath workspace,
            final TaskListener listener) throws InterruptedException {
        rootResult.stripGroup();

        LogHandler logHandler = new LogHandler(listener, "Coverage");
        FilteredLog log = new FilteredLog("Errors while reporting code coverage results:");

        CoverageNodeConverter converter = new CoverageNodeConverter();

        CoverageNode rootNode = converter.convert(rootResult);
        rootNode.splitPackages();

        Set<Entry<CoverageNode, CoveragePaint>> paintedFiles = converter.getPaintedFiles();
        log.logInfo("Painting %d source files on agent", paintedFiles.size());

        try {
            FilteredLog agentLog = workspace.act(new AgentPainter(paintedFiles));
            log.merge(agentLog);
        }
        catch (IOException exception) {
            log.logException(exception, "Can't paint and zip sources on the agent");
        }

        log.logInfo("Copying painted sources from agent to build folder");
        logHandler.log(log);

        copySourcesToBuildFolder(build, workspace, log);

        logHandler.log(log);

        Optional<CoverageBuildAction> possibleReferenceResult = getReferenceBuildAction(build, log);

        logHandler.log(log);

        CoverageBuildAction action;
        if (possibleReferenceResult.isPresent()) {
            CoverageBuildAction referenceAction = possibleReferenceResult.get();
            SortedMap<CoverageMetric, Double> delta = rootNode.computeDelta(referenceAction.getResult());

            action = new CoverageBuildAction(build, rootNode, referenceAction.getOwner().getExternalizableId(), delta);
        }
        else {
            action = new CoverageBuildAction(build, rootNode);
        }
        build.addOrReplaceAction(action);
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
                if (fallbackBuild != referenceBuild) {
                    log.logInfo("-> Reference build has no action, falling back to last build with action: '%s'",
                            fallbackBuild);
                }
            }
        }
        else {
            previousResult = getPreviousResult(build.getPreviousBuild());
            previousResult.ifPresent(coverageBuildAction ->
                    log.logInfo("-> No reference build defined, falling back to previous build: '%s'",
                            coverageBuildAction.getOwner()));
        }

        if (!previousResult.isPresent()) {
            log.logInfo("-> Found no reference result in reference build");

            return Optional.empty();
        }

        CoverageBuildAction referenceAction = previousResult.get();
        log.logInfo("-> Found reference result '%s'", referenceAction);

        return Optional.of(referenceAction);
    }

    private Optional<CoverageBuildAction> getPreviousResult(final Run<?, ?> startSearch) {
        for (Run<?, ?> build = startSearch; build != null; build = build.getPreviousBuild()) {
            CoverageBuildAction action = build.getAction(CoverageBuildAction.class);
            if (action != null) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    private void copySourcesToBuildFolder(final Run<?, ?> build, final FilePath workspace, final FilteredLog log)
            throws InterruptedException {
        try {
            FilePath buildFolder = new FilePath(build.getRootDir());
            FilePath buildZip = buildFolder.child(SourcePainter.COVERAGE_SOURCES_ZIP);
            workspace.child(SourcePainter.COVERAGE_SOURCES_ZIP).copyTo(buildZip);
            log.logInfo("-> extracting...");
            buildZip.unzip(buildFolder);
            buildZip.delete();
            log.logInfo("-> done");
        }
        catch (IOException exception) {
            log.logException(exception, "Can't copy zipped sources from agent to controller");
        }
    }
}
