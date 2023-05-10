package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.source.SourceCodePainter;
import io.jenkins.plugins.forensics.delta.Delta;
import io.jenkins.plugins.forensics.delta.FileChanges;
import io.jenkins.plugins.forensics.reference.ReferenceFinder;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.LogHandler;
import io.jenkins.plugins.util.QualityGateResult;
import io.jenkins.plugins.util.StageResultHandler;

/**
 * Transforms the old model to the new model and invokes all steps that work on the new model. Currently, only the
 * source code painting and copying has been moved to this new reporter class.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class CoverageReporter {
    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageBuildAction publishAction(final String id, final String optionalName, final String icon, final Node rootNode,
            final Run<?, ?> build, final FilePath workspace, final TaskListener listener,
            final List<CoverageQualityGate> qualityGates, final String scm, final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention, final StageResultHandler resultHandler)
            throws InterruptedException {
        FilteredLog log = new FilteredLog("Errors while reporting code coverage results:");

        Optional<CoverageBuildAction> possibleReferenceResult = getReferenceBuildAction(build, id, log);

        List<FileNode> filesToStore;
        CoverageBuildAction action;
        if (possibleReferenceResult.isPresent()) {
            CoverageBuildAction referenceAction = possibleReferenceResult.get();
            Node referenceRoot = referenceAction.getResult();

            log.logInfo("Calculating the code delta...");
            CodeDeltaCalculator codeDeltaCalculator = new CodeDeltaCalculator(build, workspace, listener, scm);
            Optional<Delta> delta = codeDeltaCalculator.calculateCodeDeltaToReference(referenceAction.getOwner(), log);
            delta.ifPresent(value -> createDeltaReports(rootNode, log, referenceRoot, codeDeltaCalculator, value));

            log.logInfo("Calculating coverage deltas...");

            Node modifiedLinesCoverageRoot = rootNode.filterByModifiedLines();

            NavigableMap<Metric, Fraction> modifiedLinesCoverageDelta;
            List<Value> aggregatedModifiedFilesCoverage;
            NavigableMap<Metric, Fraction> modifiedFilesCoverageDelta;
            if (hasModifiedLinesCoverage(modifiedLinesCoverageRoot)) {
                Node modifiedFilesCoverageRoot = rootNode.filterByModifiedFiles();
                aggregatedModifiedFilesCoverage = modifiedFilesCoverageRoot.aggregateValues();
                modifiedFilesCoverageDelta = modifiedFilesCoverageRoot.computeDelta(rootNode);
                modifiedLinesCoverageDelta = modifiedLinesCoverageRoot.computeDelta(modifiedFilesCoverageRoot);
            }
            else {
                modifiedLinesCoverageDelta = new TreeMap<>();
                aggregatedModifiedFilesCoverage = new ArrayList<>();
                modifiedFilesCoverageDelta = new TreeMap<>();
                if (rootNode.hasModifiedLines()) {
                    log.logInfo("No detected code changes affect the code coverage");
                }
            }

            NavigableMap<Metric, Fraction> coverageDelta = rootNode.computeDelta(referenceRoot);

            QualityGateResult qualityGateResult = evaluateQualityGates(rootNode, log,
                    modifiedLinesCoverageRoot.aggregateValues(), modifiedLinesCoverageDelta, coverageDelta,
                    resultHandler, qualityGates);

            if (sourceCodeRetention == SourceCodeRetention.MODIFIED) {
                filesToStore = modifiedLinesCoverageRoot.getAllFileNodes();
                log.logInfo("-> Selecting %d modified files for source code painting", filesToStore.size());
            }
            else {
                filesToStore = rootNode.getAllFileNodes();
            }

            action = new CoverageBuildAction(build, id, optionalName, icon, rootNode, qualityGateResult, log,
                    referenceAction.getOwner().getExternalizableId(), coverageDelta,
                    modifiedLinesCoverageRoot.aggregateValues(), modifiedLinesCoverageDelta,
                    aggregatedModifiedFilesCoverage, modifiedFilesCoverageDelta,
                    rootNode.filterByIndirectChanges().aggregateValues());
        }
        else {
            QualityGateResult qualityGateStatus = evaluateQualityGates(rootNode, log,
                    List.of(), new TreeMap<>(), new TreeMap<>(), resultHandler, qualityGates);

            filesToStore = rootNode.getAllFileNodes();

            action = new CoverageBuildAction(build, id, optionalName, icon, rootNode, qualityGateStatus, log);
        }

        log.logInfo("Executing source code painting...");
        SourceCodePainter sourceCodePainter = new SourceCodePainter(build, workspace, id);
        sourceCodePainter.processSourceCodePainting(rootNode, filesToStore,
                sourceCodeEncoding, sourceCodeRetention, log);

        log.logInfo("Finished coverage processing - adding the action to the build...");

        LogHandler logHandler = new LogHandler(listener, "Coverage");
        logHandler.log(log);

        build.addAction(action);
        return action;
    }

    private void createDeltaReports(final Node rootNode, final FilteredLog log, final Node referenceRoot,
            final CodeDeltaCalculator codeDeltaCalculator, final Delta delta) {
        FileChangesProcessor fileChangesProcessor = new FileChangesProcessor();

        try {
            log.logInfo("Preprocessing code changes...");
            Set<FileChanges> changes = codeDeltaCalculator.getCoverageRelevantChanges(delta);
            var mappedChanges = codeDeltaCalculator.mapScmChangesToReportPaths(changes, rootNode, log);
            var oldPathMapping = codeDeltaCalculator.createOldPathMapping(rootNode, referenceRoot, mappedChanges, log);

            log.logInfo("Obtaining code changes for files...");
            fileChangesProcessor.attachChangedCodeLines(rootNode, mappedChanges);

            log.logInfo("Obtaining indirect coverage changes...");
            fileChangesProcessor.attachIndirectCoveragesChanges(rootNode, referenceRoot,
                    mappedChanges, oldPathMapping);

            log.logInfo("Obtaining coverage delta for files...");
            fileChangesProcessor.attachFileCoverageDeltas(rootNode, referenceRoot, oldPathMapping);
        }
        catch (IllegalStateException exception) {
            log.logError("An error occurred while processing code and coverage changes:");
            log.logError("-> Message: " + exception.getMessage());
            log.logError("-> Skipping calculating modified lines coverage, modified files coverage"
                    + " and indirect coverage changes");
        }
    }

    private QualityGateResult evaluateQualityGates(final Node rootNode, final FilteredLog log,
            final List<Value> modifiedLinesCoverageDistribution,
            final NavigableMap<Metric, Fraction> modifiedLinesCoverageDelta,
            final NavigableMap<Metric, Fraction> coverageDelta, final StageResultHandler resultHandler,
            final List<CoverageQualityGate> qualityGates) {
        var statistics = new CoverageStatistics(rootNode.aggregateValues(), coverageDelta,
                modifiedLinesCoverageDistribution, modifiedLinesCoverageDelta, List.of(), new TreeMap<>());
        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, statistics);
        var qualityGateStatus = evaluator.evaluate();
        if (qualityGateStatus.isInactive()) {
            log.logInfo("No quality gates have been set - skipping");
        }
        else {
            log.logInfo("Evaluating quality gates");
            if (qualityGateStatus.isSuccessful()) {
                log.logInfo("-> All quality gates have been passed");
            }
            else {
                var message = String.format("-> Some quality gates have been missed: overall result is %s",
                        qualityGateStatus.getOverallStatus().getResult());
                log.logInfo(message);
                resultHandler.setResult(qualityGateStatus.getOverallStatus().getResult(), message);
            }
            log.logInfo("-> Details for each quality gate:");
            qualityGateStatus.getMessages().forEach(log::logInfo);
        }
        return qualityGateStatus;
    }

    private boolean hasModifiedLinesCoverage(final Node modifiedLinesCoverageRoot) {
        Optional<Value> lineCoverage = modifiedLinesCoverageRoot.getValue(Metric.LINE);
        if (lineCoverage.isPresent() && hasLineCoverageSet(lineCoverage.get())) {
            return true;
        }
        Optional<Value> branchCoverage = modifiedLinesCoverageRoot.getValue(Metric.BRANCH);
        return branchCoverage.filter(this::hasLineCoverageSet).isPresent();
    }

    private boolean hasLineCoverageSet(final Value value) {
        return ((edu.hm.hafner.coverage.Coverage) value).isSet();
    }

    private Optional<CoverageBuildAction> getReferenceBuildAction(final Run<?, ?> build, final String id, final FilteredLog log) {
        log.logInfo("Obtaining action of reference build");

        ReferenceFinder referenceFinder = new ReferenceFinder();
        Optional<Run<?, ?>> reference = referenceFinder.findReference(build, log);

        Optional<CoverageBuildAction> previousResult;
        if (reference.isPresent()) {
            Run<?, ?> referenceBuild = reference.get();
            log.logInfo("-> Using reference build '%s'", referenceBuild);
            previousResult = getPreviousResult(id, reference.get());
            if (previousResult.isPresent()) {
                Run<?, ?> fallbackBuild = previousResult.get().getOwner();
                if (!fallbackBuild.equals(referenceBuild)) {
                    log.logInfo("-> Reference build has no action, falling back to last build with action: '%s'",
                            fallbackBuild.getDisplayName());
                }
            }
        }
        else {
            previousResult = getPreviousResult(id, build.getPreviousBuild());
            previousResult.ifPresent(coverageBuildAction ->
                    log.logInfo("-> No reference build defined, falling back to previous build: '%s'",
                            coverageBuildAction.getOwner().getDisplayName()));
        }

        if (previousResult.isEmpty()) {
            log.logInfo("-> Found no reference result in reference build");

            return Optional.empty();
        }

        CoverageBuildAction referenceAction = previousResult.get();
        log.logInfo("-> Found reference result in build '%s'", referenceAction.getOwner().getDisplayName());

        return Optional.of(referenceAction);
    }

    private Optional<CoverageBuildAction> getPreviousResult(final String id, @CheckForNull final Run<?, ?> startSearch) {
        for (Run<?, ?> build = startSearch; build != null; build = build.getPreviousBuild()) {
            List<CoverageBuildAction> actions = build.getActions(CoverageBuildAction.class);
            for (CoverageBuildAction action : actions) {
                if (action.getUrlName().equals(id)) {
                    return Optional.of(action);
                }
            }
        }
        return Optional.empty();
    }
}
