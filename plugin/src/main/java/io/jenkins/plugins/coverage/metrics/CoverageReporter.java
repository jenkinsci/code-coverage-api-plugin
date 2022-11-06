package io.jenkins.plugins.coverage.metrics;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.metrics.visualization.code.SourceCodePainter;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.reference.ReferenceFinder;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.LogHandler;

import static io.jenkins.plugins.coverage.metrics.FilePathValidator.*;

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
     * @param scm
     *         the SCM which is used for calculating the code delta to a reference build
     * @param sourceDirectories
     *         the source directories that have been configured in the associated job
     * @param sourceCodeEncoding
     *         the encoding of the source code files
     * @param sourceCodeRetention
     *         the source code retention strategy
     *
     * @throws InterruptedException
     *         if the build has been aborted
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public void run(final Node rootResult, final Run<?, ?> build, final FilePath workspace,
            final TaskListener listener, final String scm,
            final Set<String> sourceDirectories, final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention)
            throws InterruptedException {
        LogHandler logHandler = new LogHandler(listener, "Coverage");
        FilteredLog log = new FilteredLog("Errors while reporting code coverage results:");

        verifyPathUniqueness(rootResult, log);

        runWithModel(build, workspace, listener, scm, sourceDirectories, sourceCodeEncoding,
                sourceCodeRetention,
                logHandler, log, rootResult, new HashSet<>());
    }

    private void runWithModel(final Run<?, ?> build, final FilePath workspace, final TaskListener listener,
            final String scm, final Set<String> sourceDirectories,
            final String sourceCodeEncoding, final SourceCodeRetention sourceCodeRetention, final LogHandler logHandler,
            final FilteredLog log, final Node rootNode,
            final Set<Entry<Node, CoveragePaint>> paintedFiles)
            throws InterruptedException {
        Optional<CoverageBuildAction> possibleReferenceResult = getReferenceBuildAction(build, log);

        HealthReport healthReport = new HealthReport(); // FIXME: currently empty

        CoverageBuildAction action;
        if (possibleReferenceResult.isPresent()) {
            CoverageBuildAction referenceAction = possibleReferenceResult.get();
            Node referenceRoot = referenceAction.getResult();

            // calculate code delta
            log.logInfo("Calculating the code delta...");
            CodeDeltaCalculator codeDeltaCalculator = new CodeDeltaCalculator(build, workspace, listener, scm);
            Optional<Delta> delta = codeDeltaCalculator.calculateCodeDeltaToReference(referenceAction.getOwner(), log);

            if (delta.isPresent()) {
                FileChangesProcessor fileChangesProcessor = new FileChangesProcessor();

                try {
                    log.logInfo("Verify uniqueness of reference file paths...");
                    verifyPathUniqueness(referenceRoot, log);

                    log.logInfo("Preprocessing code changes...");
                    Set<FileChanges> changes = codeDeltaCalculator.getCoverageRelevantChanges(delta.get());
                    Map<String, FileChanges> mappedChanges =
                            codeDeltaCalculator.mapScmChangesToReportPaths(changes, rootNode, log);
                    Map<String, String> oldPathMapping = codeDeltaCalculator.createOldPathMapping(
                            rootNode, referenceRoot, mappedChanges, log);

                    // calculate code changes
                    log.logInfo("Obtaining code changes for files...");
                    fileChangesProcessor.attachChangedCodeLines(rootNode, mappedChanges);

                    // indirect coverage changes
                    log.logInfo("Obtaining indirect coverage changes...");
                    fileChangesProcessor.attachIndirectCoveragesChanges(rootNode, referenceRoot,
                            mappedChanges, oldPathMapping);

                    // file coverage deltas
                    log.logInfo("Obtaining coverage delta for files...");
                    fileChangesProcessor.attachFileCoverageDeltas(rootNode, referenceRoot, oldPathMapping);
                }
                catch (CodeDeltaException e) {
                    log.logError("An error occurred while processing code and coverage changes:");
                    log.logError("-> Message: " + e.getMessage());
                    log.logError("-> Skipping calculating change coverage and indirect coverage changes");
                }
            }

            log.logInfo("Calculating coverage deltas...");

            // filtered coverage trees
            CoverageTreeCreator coverageTreeCreator = new CoverageTreeCreator();
            Node changeCoverageRoot = coverageTreeCreator.createChangeCoverageTree(rootNode);
            Node indirectCoverageChangesTree = coverageTreeCreator.createIndirectCoverageChangesTree(rootNode);

            // coverage delta
            NavigableMap<Metric, Fraction> coverageDelta = rootNode.computeDelta(referenceRoot);

            NavigableMap<Metric, Fraction> changeCoverageDelta;
            if (hasChangeCoverage(changeCoverageRoot)) {
                changeCoverageDelta = changeCoverageRoot.computeDelta(rootNode);
            }
            else {
                changeCoverageDelta = new TreeMap<>();
                if (rootNode.hasCodeChanges()) {
                    log.logInfo("No detected code changes affect the code coverage");
                }
            }

            action = new CoverageBuildAction(build, rootNode, healthReport,
                    referenceAction.getOwner().getExternalizableId(),
                    coverageDelta,
                    changeCoverageRoot.getMetricsDistribution(),
                    changeCoverageDelta,
                    indirectCoverageChangesTree.getMetricsDistribution());
        }
        else {
            action = new CoverageBuildAction(build, rootNode, healthReport);
        }

        log.logInfo("Executing source code painting...");
        SourceCodePainter sourceCodePainter = new SourceCodePainter(build, workspace);
        sourceCodePainter.processSourceCodePainting(rootNode, sourceDirectories,
                sourceCodeEncoding, sourceCodeRetention, log);

        log.logInfo("Finished coverage processing - adding the action to the build...");

        logHandler.log(log);

        build.addOrReplaceAction(action);
    }

    private boolean hasChangeCoverage(final Node changeCoverageRoot) {
        Optional<Value> lineCoverage = changeCoverageRoot.getValue(Metric.LINE);
        if (lineCoverage.isPresent()) {
            if (((edu.hm.hafner.metric.Coverage) lineCoverage.get()).isSet()) {
                return true;
            }
        }
        Optional<Value> branchCoverage = changeCoverageRoot.getValue(Metric.BRANCH);
        return branchCoverage.filter(value -> ((edu.hm.hafner.metric.Coverage) value).isSet()).isPresent();
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
