package io.jenkins.plugins.coverage.metrics;

import java.util.List;
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
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.coverage.metrics.visualization.code.SourceCodePainter;
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
    @SuppressWarnings("checkstyle:ParameterNumber")
    void publishAction(final String id, final String optionalName, final Node rootNode, final Run<?, ?> build, final FilePath workspace,
            final TaskListener listener, final List<QualityGate> qualityGates, final String scm,
            final Set<String> sourceDirectories, final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention, final StageResultHandler resultHandler)
            throws InterruptedException {
        LogHandler logHandler = new LogHandler(listener, "Coverage");
        FilteredLog log = new FilteredLog("Errors while reporting code coverage results:");

        verifyPathUniqueness(rootNode, log);

        Optional<CoverageBuildAction> possibleReferenceResult = getReferenceBuildAction(build, log);

        CoverageBuildAction action;
        if (possibleReferenceResult.isPresent()) {
            CoverageBuildAction referenceAction = possibleReferenceResult.get();
            Node referenceRoot = referenceAction.getResult();

            log.logInfo("Calculating the code delta...");
            CodeDeltaCalculator codeDeltaCalculator = new CodeDeltaCalculator(build, workspace, listener, scm);
            Optional<Delta> delta = codeDeltaCalculator.calculateCodeDeltaToReference(referenceAction.getOwner(), log);
            delta.ifPresent(value -> createDeltaReports(rootNode, log, referenceRoot, codeDeltaCalculator, value));

            log.logInfo("Calculating coverage deltas...");

            Node changeCoverageRoot = rootNode.filterChanges();

            NavigableMap<Metric, Fraction> changeCoverageDelta;
            if (hasChangeCoverage(changeCoverageRoot)) {
                changeCoverageDelta = changeCoverageRoot.computeDelta(rootNode);
            }
            else {
                changeCoverageDelta = new TreeMap<>();
                if (rootNode.hasChangedLines()) {
                    log.logInfo("No detected code changes affect the code coverage");
                }
            }

            NavigableMap<Metric, Fraction> coverageDelta = rootNode.computeDelta(referenceRoot);
            Node indirectCoverageChangesTree = rootNode.filterByIndirectlyChangedCoverage();

            QualityGateStatus qualityGateStatus;
            qualityGateStatus = evaluateQualityGates(rootNode, log,
                    changeCoverageRoot.aggregateValues(), changeCoverageDelta, coverageDelta,
                    resultHandler, qualityGates);

            action = new CoverageBuildAction(build, id, optionalName, rootNode, qualityGateStatus, log,
                    referenceAction.getOwner().getExternalizableId(),
                    coverageDelta,
                    changeCoverageRoot.aggregateValues(),
                    changeCoverageDelta,
                    indirectCoverageChangesTree.aggregateValues());
        }
        else {
            QualityGateStatus qualityGateStatus = evaluateQualityGates(rootNode, log,
                    List.of(), new TreeMap<>(), new TreeMap<>(), resultHandler, qualityGates);

            action = new CoverageBuildAction(build, id, optionalName, rootNode, qualityGateStatus, log);
        }

        log.logInfo("Executing source code painting...");
        SourceCodePainter sourceCodePainter = new SourceCodePainter(build, workspace);
        sourceCodePainter.processSourceCodePainting(rootNode, sourceDirectories,
                sourceCodeEncoding, sourceCodeRetention, log);

        log.logInfo("Finished coverage processing - adding the action to the build...");

        logHandler.log(log);

        build.addAction(action);
    }

    private void createDeltaReports(final Node rootNode, final FilteredLog log, final Node referenceRoot,
            final CodeDeltaCalculator codeDeltaCalculator, final Delta delta) {
        FileChangesProcessor fileChangesProcessor = new FileChangesProcessor();

        try {
            log.logInfo("Verify uniqueness of reference file paths...");
            verifyPathUniqueness(referenceRoot, log);

            log.logInfo("Preprocessing code changes...");
            Set<FileChanges> changes = codeDeltaCalculator.getCoverageRelevantChanges(delta);
            var mappedChanges = codeDeltaCalculator.mapScmChangesToReportPaths(changes, rootNode, log);
            var oldPathMapping = codeDeltaCalculator.createOldPathMapping(rootNode, referenceRoot, mappedChanges, log);

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

    private QualityGateStatus evaluateQualityGates(final Node rootNode, final FilteredLog log,
            final List<Value> changeCoverageDistribution, final NavigableMap<Metric, Fraction> changeCoverageDelta,
            final NavigableMap<Metric, Fraction> coverageDelta, final StageResultHandler resultHandler,
            final List<QualityGate> qualityGates) {
        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.addAll(qualityGates);
        QualityGateStatus qualityGateStatus;
        if (evaluator.isEnabled()) {
            log.logInfo("Evaluating quality gates");
            var statistics = new CoverageStatistics(rootNode.aggregateValues(), coverageDelta,
                    changeCoverageDistribution, changeCoverageDelta,
                    List.of(), new TreeMap<>());
            qualityGateStatus = evaluator.evaluate(statistics, log::logInfo);
            if (qualityGateStatus.isSuccessful()) {
                log.logInfo("-> All quality gates have been passed");
            }
            else {
                log.logInfo("-> Some quality gates have been missed: overall result is %s", qualityGateStatus);
            }
            if (!qualityGateStatus.isSuccessful()) {
                resultHandler.setResult(qualityGateStatus.getResult(),
                        "Some quality gates have been missed: overall result is " + qualityGateStatus.getResult());
            }
        }
        else {
            log.logInfo("No quality gates have been set - skipping");
            qualityGateStatus = QualityGateStatus.INACTIVE;
        }
        return qualityGateStatus;
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
