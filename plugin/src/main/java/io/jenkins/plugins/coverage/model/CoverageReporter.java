package io.jenkins.plugins.coverage.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.model.coverage.CoverageTreeCreator;
import io.jenkins.plugins.coverage.model.coverage.FileCoverageDeltaProcessor;
import io.jenkins.plugins.coverage.model.visualization.code.SourceCodePainter;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.reference.ReferenceFinder;
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
     * @param healthReport
     *         health report
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
    public void run(final CoverageResult rootResult, final Run<?, ?> build, final FilePath workspace,
            final TaskListener listener, final HealthReport healthReport, final String scm,
            final Set<String> sourceDirectories, final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention)
            throws InterruptedException {
        LogHandler logHandler = new LogHandler(listener, "Coverage");
        FilteredLog log = new FilteredLog("Errors while reporting code coverage results:");

        rootResult.stripGroup();

        CoverageNodeConverter converter = new CoverageNodeConverter();
        CoverageNode rootNode = converter.convert(rootResult);
        rootNode.splitPackages();

        Optional<CoverageBuildAction> possibleReferenceResult = getReferenceBuildAction(build, log);

        logHandler.log(log);

        CoverageBuildAction action;
        if (possibleReferenceResult.isPresent()) {
            CoverageBuildAction referenceAction = possibleReferenceResult.get();

            // calculate code delta
            log.logInfo("Calculating the code delta...");
            CodeDeltaCalculator codeDeltaCalculator =
                    new CodeDeltaCalculator(build, workspace, listener, scm, sourceDirectories);
            Optional<Delta> delta = codeDeltaCalculator.calculateCodeDeltaToReference(referenceAction.getOwner(), log);

            if (delta.isPresent()) {
                Map<String, FileChanges> codeChanges = codeDeltaCalculator.getChangeCoverageRelevantChanges(
                        delta.get());
                FileCoverageDeltaProcessor fileCoverageDeltaProcessor = new FileCoverageDeltaProcessor();

                // calculate code changes
                log.logInfo("Obtaining code changes for files...");
                fileCoverageDeltaProcessor.attachChangedCodeLines(rootNode, codeChanges);

                // file coverage deltas
                log.logInfo("Obtaining coverage delta for files...");
                fileCoverageDeltaProcessor.attachFileCoverageDeltas(rootNode, referenceAction.getResult());

                // indirect coverage changes
                log.logInfo("Obtaining indirect coverage changes...");
                fileCoverageDeltaProcessor.attachIndirectCoveragesChanges(rootNode, referenceAction.getResult(),
                        codeChanges);
            }

            logHandler.log(log);

            // filtered coverage trees
            CoverageTreeCreator coverageTreeCreator = new CoverageTreeCreator();
            CoverageNode changeCoverageRoot = coverageTreeCreator.createChangeCoverageTree(rootNode);
            CoverageNode indirectCoverageChangesTree = coverageTreeCreator.createIndirectCoverageChangesTree(rootNode);

            // coverage delta
            SortedMap<CoverageMetric, Fraction> coverageDelta = rootNode.computeDelta(referenceAction.getResult());
            SortedMap<CoverageMetric, Fraction> changeCoverageDelta;
            if (rootNode.hasChangeCoverage() && referenceAction.getResult().hasChangeCoverage()) {
                CoverageNode referenceChangeCoverageRoot =
                        coverageTreeCreator.createChangeCoverageTree(referenceAction.getResult());
                changeCoverageDelta = changeCoverageRoot.computeDelta(referenceChangeCoverageRoot);
            }
            else {
                changeCoverageDelta = new TreeMap<>();
            }

            if (rootNode.hasCodeChanges() && !rootNode.hasChangeCoverage()) {
                log.logInfo("No detected code changes affect the code coverage");
            }

            action = new CoverageBuildAction(build, rootNode, healthReport,
                    referenceAction.getOwner().getExternalizableId(), coverageDelta,
                    changeCoverageRoot.getMetricPercentages(), changeCoverageDelta,
                    indirectCoverageChangesTree.getMetricPercentages());
        }
        else {
            action = new CoverageBuildAction(build, rootNode, healthReport);
        }

        SourceCodePainter sourceCodePainter = new SourceCodePainter(build, workspace);
        sourceCodePainter.processSourceCodePainting(converter.getPaintedFiles(), sourceDirectories,
                sourceCodeEncoding, sourceCodeRetention, log);

        logHandler.log(log);

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
