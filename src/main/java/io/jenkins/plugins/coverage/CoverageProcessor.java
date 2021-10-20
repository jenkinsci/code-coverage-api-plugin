package io.jenkins.plugins.coverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.NonNull;

import org.jvnet.localizer.Localizable;
import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.detector.Detectable;
import io.jenkins.plugins.coverage.detector.ReportDetector;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.forensics.reference.ReferenceFinder;
import io.jenkins.plugins.util.PluginLogger;

public class CoverageProcessor {

    private static final String DEFAULT_REPORT_SAVE_NAME = "coverage-report";

    private final Run<?, ?> run;
    private final FilePath workspace;
    private final TaskListener listener;

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private boolean applyThresholdRecursively;

    private String globalTag;

    private boolean failBuildIfCoverageDecreasedInChangeRequest;

    private SourceFileResolver sourceFileResolver;

    /**
     * @param run
     *         a build this is running as a part of
     * @param workspace
     *         a workspace to use for any file operations
     * @param listener
     *         a place to send output
     */
    public CoverageProcessor(@NonNull final Run<?, ?> run, @NonNull final FilePath workspace,
            @NonNull final TaskListener listener) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
    }

    /**
     * Convert all reports that are specified by {@link CoverageReportAdapter}s and detected by {@link ReportDetector}s
     * to {@link CoverageResult}, and generate health report from CoverageResult. Add them to {@link CoverageAction} and
     * add Action to {@link Run}.
     *
     * @param reportAdapters
     *         reportAdapters specified by user
     * @param reportDetectors
     *         reportDetectors specified by user
     * @param globalThresholds
     *         global threshold specified by user
     */
    public void performCoverageReport(final List<CoverageReportAdapter> reportAdapters,
            final List<ReportDetector> reportDetectors, final List<Threshold> globalThresholds)
            throws IOException, InterruptedException, CoverageException {
        Map<CoverageReportAdapter, List<CoverageResult>> results = convertToResults(reportAdapters, reportDetectors);

        CoverageResult coverageReport = aggregateReports(results);
        if (coverageReport == null) {
            return;
        }

        coverageReport.setOwner(run);

        if (sourceFileResolver != null) {
            Set<String> possiblePaths = new HashSet<>();
            coverageReport.getChildrenReal().forEach((s, coverageResult) -> {
                Set<String> paths = coverageResult.getAdditionalProperty(
                        CoverageFeatureConstants.FEATURE_SOURCE_FILE_PATH);
                if (paths != null) {
                    possiblePaths.addAll(paths);
                }
            });

            if (possiblePaths.size() > 0) {
                sourceFileResolver.setPossiblePaths(possiblePaths);
            }

            sourceFileResolver.resolveSourceFiles(run, workspace, listener, coverageReport.getPaintedSources());
        }

        PluginLogger pluginLogger = new PluginLogger(listener.getLogger(), "Coverage");
        FilteredLog log = new FilteredLog("Errors while computing delta coverage:");
        Optional<Run<?, ?>> possibleReferenceBuild = setDiffInCoverageForChangeRequest(coverageReport, log);
        pluginLogger.logEachLine(log.getInfoMessages());
        pluginLogger.logEachLine(log.getErrorMessages());

        CoverageAction action = convertResultToAction(coverageReport);

        HealthReport healthReport = processThresholds(results, globalThresholds, action);
        action.setHealthReport(healthReport);

        if (failBuildIfCoverageDecreasedInChangeRequest) {
            failBuildIfChangeRequestDecreasedCoverage(coverageReport);
        }

        CoverageNode coverageNode = convertCoverageResultToCoverageNode(coverageReport);
        this.run.addOrReplaceAction(createNewBuildAction(coverageNode, possibleReferenceBuild));
    }

    private CoverageNode convertCoverageResultToCoverageNode(final CoverageResult coverageReport) {
        CoverageResult root = coverageReport.getRoot();
        root.stripGroup();
        CoverageNode coverageNode = CoverageNodeConverter.convert(root);
        coverageNode.splitPackages();
        return coverageNode;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CoverageBuildAction createNewBuildAction(final CoverageNode coverageNode,
            final Optional<Run<?, ?>> possibleReferenceBuild) {
        if (possibleReferenceBuild.isPresent()) {
            Run<?, ?> referenceBuild = possibleReferenceBuild.get();
            CoverageBuildAction previousAction = referenceBuild.getAction(CoverageBuildAction.class);
            if (previousAction != null) {
                SortedMap<CoverageMetric, Double> delta = coverageNode.computeDelta(previousAction.getResult());
                return new CoverageBuildAction(this.run, coverageNode, referenceBuild.getExternalizableId(), delta);
            }
        }

        return new CoverageBuildAction(this.run, coverageNode);
    }

    private CoverageAction convertResultToAction(final CoverageResult coverageReport) throws IOException {
        synchronized (CoverageProcessor.class) {
            CoverageAction previousAction = run.getAction(CoverageAction.class);
            if (previousAction == null) {
                saveCoverageResult(run, coverageReport);

                CoverageAction action = new CoverageAction(coverageReport);
                run.addAction(action);

                return action;
            }
            else {
                CoverageResult previousResult = previousAction.getResult();
                Collection<CoverageResult> previousReports = previousResult.getChildrenReal().values();

                for (CoverageResult report : coverageReport.getChildrenReal().values()) {
                    if (StringUtils.isEmpty(report.getTag())) {
                        report.resetParent(previousResult);
                        continue;
                    }

                    Optional<CoverageResult> matchedTagReport;
                    if ((matchedTagReport = previousReports.stream()
                            .filter(r -> !StringUtils.isEmpty(r.getTag()) && r.getTag().equals(report.getTag()))
                            .findAny()).isPresent()) {
                        try {
                            matchedTagReport.get().merge(report);
                        }
                        catch (CoverageException e) {
                            e.printStackTrace();
                            report.resetParent(previousResult);
                        }
                    }
                    else {
                        report.resetParent(previousResult);
                    }
                }

                previousResult.setOwner(run);
                saveCoverageResult(run, previousResult);
                return previousAction;
            }
        }
    }

    private Optional<Run<?, ?>> setDiffInCoverageForChangeRequest(final CoverageResult coverageReport, final FilteredLog log) {
        log.logInfo("Computing coverage delta report");

        ReferenceFinder referenceFinder = new ReferenceFinder();
        Optional<Run<?, ?>> reference = referenceFinder.findReference(run, log);
        Optional<CoverageAction> previousResult;
        if (reference.isPresent()) {
            Run<?, ?> referenceRun = reference.get();
            log.logInfo("-> Using reference build '%s'", referenceRun);
            coverageReport.setReferenceBuildUrl(referenceRun.getUrl()); // FIXME: use ID
            previousResult = getPreviousResult(reference.get());
        }
        else {
            log.logInfo("-> No reference build defined, falling back to previous build");
            previousResult = getPreviousResult(run.getPreviousBuild());
        }

        if (!previousResult.isPresent() || previousResult.get().getResult() == null) {
            log.logInfo("-> Found no reference result in reference build");

            return Optional.empty();
        }

        CoverageAction referenceAction = previousResult.get();
        log.logInfo("-> Found reference result '%s'", referenceAction);

        CoverageResult referenceCoverageResult = referenceAction.getResult();

        Map<CoverageElement, Float> deltaCoverage = new TreeMap<>();
        referenceCoverageResult.getResults().forEach((coverageElement, referenceRatio) -> {
            Ratio buildRatio = coverageReport.getCoverage(coverageElement);

            if (buildRatio != null) {
                float diff = buildRatio.getPercentageFloat() - referenceRatio.getPercentageFloat();
                listener.getLogger()
                        .println(coverageElement.getName() + " coverage diff: " + diff + "%. Add to CoverageResult.");
                deltaCoverage.put(coverageElement, diff);
            }
        });

        coverageReport.setDeltaResults(deltaCoverage);

        return Optional.of(previousResult.get().getOwner());
    }

    private Optional<CoverageAction> getPreviousResult(final Run<?, ?> startSearch) {
        for (Run<?, ?> build = startSearch; build != null; build = build.getPreviousBuild()) {
            CoverageAction action = build.getAction(CoverageAction.class);
            if (action != null) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    private void failBuildIfChangeRequestDecreasedCoverage(final CoverageResult coverageResult) throws CoverageException {
        float coverageDiff = coverageResult.getCoverageDelta(CoverageElement.LINE);
        if (coverageDiff < 0) {
            throw new CoverageException(
                    "Fail build because this change request decreases line coverage by " + coverageDiff);
        }
    }

    /**
     * Convert reports that are specified by {@link CoverageReportAdapter} and detected by {@link ReportDetector}s to
     * {@link CoverageResult}.
     *
     * @param adapters
     *         {@link CoverageReportAdapter} for each report
     *
     * @return {@link CoverageResult} for each report
     */
    private Map<CoverageReportAdapter, List<CoverageResult>> convertToResults(final List<CoverageReportAdapter> adapters,
            final List<ReportDetector> reportDetectors)
            throws IOException, InterruptedException, CoverageException {
        PrintStream logger = listener.getLogger();

        Map<CoverageReportAdapter, Set<FilePath>> reports = new HashMap<>();
        Map<CoverageReportAdapter, List<File>> copiedReport = new HashMap<>();

        if (adapters != null) {
            // find report according to the path of each adapter
            for (CoverageReportAdapter adapter : adapters) {
                String path = adapter.getPath();

                FilePath[] r = workspace.act(new FindReportCallable(path, adapter));
                reports.put(adapter, Sets.newHashSet(r));
            }

            // copy reports to build dir
            File runRootDir = run.getRootDir();

            for (Map.Entry<CoverageReportAdapter, Set<FilePath>> adapterReports : reports.entrySet()) {
                Set<FilePath> r = adapterReports.getValue();
                List<File> copies = new LinkedList<>();

                for (FilePath f : r) {
                    File copy = new File(runRootDir, f.getName());

                    //if copy exist, it means there have reports have same name.
                    int i = 1;
                    while (copy.exists()) {
                        copy = new File(runRootDir, String.format("%s(%d)", f.getName(), i++));
                    }

                    f.copyTo(new FilePath(copy));
                    copies.add(copy);
                }

                copiedReport.put(adapterReports.getKey(), copies);
            }
        }

        int detectCount = 0;
        if (reportDetectors.size() != 0) {
            for (ReportDetector reportDetector : reportDetectors) {
                Map<CoverageReportAdapter, List<File>> detectedReportFiles = reportDetector.getReports(run, workspace,
                        listener, filePath -> {
                            for (Map.Entry<CoverageReportAdapter, Set<FilePath>> entry : reports.entrySet()) {
                                if (entry.getValue().contains(filePath)) {
                                    return false;
                                }
                            }
                            return true;
                        });

                detectCount += detectedReportFiles.values().stream().mapToInt(List::size).sum();

                for (Map.Entry<CoverageReportAdapter, List<File>> e : detectedReportFiles.entrySet()) {
                    if (copiedReport.containsKey(e.getKey())) {
                        copiedReport.get(e.getKey()).addAll(e.getValue());
                    }
                    else {
                        copiedReport.put(e.getKey(), e.getValue());
                    }
                }
            }
            logger.printf("Auto Detect was ended: Found %d report%n", detectCount);
        }

        reports.clear();

        // convert report to results
        Map<CoverageReportAdapter, List<CoverageResult>> results = new HashMap<>();
        for (Map.Entry<CoverageReportAdapter, List<File>> adapterReports : copiedReport.entrySet()) {
            CoverageReportAdapter adapter = adapterReports.getKey();
            CoverageReportAdapterDescriptor descriptor = (CoverageReportAdapterDescriptor) adapter.getDescriptor();
            for (File foundedFile : adapterReports.getValue()) {
                try {
                    boolean isValidate;

                    // If is Detectable, then use detect to validate file, else simply use file length
                    if (descriptor instanceof Detectable) {
                        isValidate = ((Detectable) descriptor).detect(foundedFile);
                    }
                    else {
                        // skip file if file is empty
                        isValidate = Files.size(Paths.get(foundedFile.toURI())) > 0;
                    }

                    if (isValidate) {
                        results.putIfAbsent(adapter, new LinkedList<>());
                        CoverageResult result = adapter.getResult(foundedFile);

                        if (!StringUtils.isEmpty(globalTag)) {
                            result.setTag(globalTag);
                        }

                        results.get(adapter).add(result);
                    }
                }
                catch (CoverageException e) {
                    e.printStackTrace();
                    logger.printf("report %s for %s has met some errors: %s%n",
                            foundedFile.getAbsolutePath(),
                            adapter.getDescriptor().getDisplayName(),
                            e.getMessage());
                }
                finally {
                    FileUtils.deleteQuietly(foundedFile);
                }
            }
            if (adapter.isMergeToOneReport()) {
                List<CoverageResult> resultOfAdapter = results.get(adapter);
                if (resultOfAdapter == null || resultOfAdapter.size() <= 1) {
                    continue;
                }
                CoverageResult report = aggregateToOneReport(adapter, resultOfAdapter);
                resultOfAdapter.clear();
                resultOfAdapter.add(report);
            }

        }

        if (results.size() == 0) {
            logger.println("No reports were found");
            if (getFailNoReports()) {
                throw new CoverageException("Publish Coverage Failed : No Reports were found");
            }
        }
        else {
            logger.printf("A total of %d reports were found%n",
                    results.values()
                            .stream()
                            .mapToLong(Collection::size)
                            .sum());
        }

        return results;
    }

    /**
     * Process threshold and return health report.
     *
     * @param adapterWithResults
     *         Coverage report adapter and its correspond Coverage results.
     * @param globalThresholds
     *         global threshold
     * @param action
     *         coverage action
     *
     * @return Health report
     */
    private HealthReport processThresholds(final Map<CoverageReportAdapter, List<CoverageResult>> adapterWithResults,
            final List<Threshold> globalThresholds, final CoverageAction action) throws CoverageException {

        int healthyCount = 0;
        int unhealthyCount = 0;
        int unstableCount = 0;

        Set<Threshold> unstableThresholds = new HashSet<>();
        Set<Threshold> unhealthyThresholds = new HashSet<>();

        LinkedList<CoverageResult> resultTask = new LinkedList<>();

        for (Map.Entry<CoverageReportAdapter, List<CoverageResult>> results : adapterWithResults.entrySet()) {
            // make local threshold over global threshold
            List<Threshold> thresholds = results.getKey().getThresholds();
            if (thresholds != null) {
                thresholds = new ArrayList<>(thresholds);
                for (Threshold t : globalThresholds) {
                    if (!thresholds.contains(t)) {
                        thresholds.add(t);
                    }
                }
            }
            else {
                thresholds = globalThresholds;
            }

            for (CoverageResult coverageResult : results.getValue()) {
                resultTask.push(coverageResult);

                while (!resultTask.isEmpty()) {
                    CoverageResult r = resultTask.pollFirst();
                    assert r != null;

                    // if apply threshold recursively is true, we will add all children to queue
                    if (isApplyThresholdRecursively()) {
                        resultTask.addAll(r.getChildrenReal().values());
                    }
                    for (Threshold threshold : thresholds) {
                        Ratio ratio = r.getCoverage(threshold.getThresholdTargetElement());
                        if (ratio == null) {
                            continue;
                        }

                        float percentage = ratio.getPercentageFloat();
                        if (percentage < threshold.getUnstableThreshold()) {
                            unstableCount++;
                            listener.getLogger()
                                    .printf("Code coverage enforcement failed: %s coverage in %s level '%s' is lower than %.2f stable threshold%n",
                                            threshold.getThresholdTarget(),
                                            r.getElement().getName(),
                                            r.getName(), threshold.getUnstableThreshold());
                            unstableThresholds.add(threshold);
                        }
                        else if (percentage < threshold.getUnhealthyThreshold()) {
                            unhealthyCount++;
                            listener.getLogger()
                                    .printf("Code coverage enforcement failed: %s coverage in %s level '%s' is lower than %.2f healthy threshold%n",
                                            threshold.getThresholdTarget(),
                                            r.getElement().getName(),
                                            r.getName(), threshold.getUnhealthyThreshold());
                            unhealthyThresholds.add(threshold);
                        }
                        else {
                            healthyCount++;
                        }
                    }
                }
            }
        }

        if (unstableCount > 0) {
            if (getFailUnstable()) {
                action.setFailMessage(
                        String.format("Build failed because following metrics did not meet stability target: %s.",
                                unstableThresholds));
                throw new CoverageException(action.getFailMessage());
            }
            else {
                action.setFailMessage(
                        String.format("Build unstable because following metrics did not meet stability target: %s.",
                                unstableThresholds));
                run.setResult(Result.UNSTABLE);
            }
        }

        if (unhealthyCount > 0) {
            if (getFailUnhealthy()) {
                action.setFailMessage(
                        String.format("Build failed because following metrics did not meet health target: %s.",
                                unhealthyThresholds));
                throw new CoverageException(action.getFailMessage());
            }

            unhealthyThresholds = unhealthyThresholds.stream().filter(Threshold::isFailUnhealthy)
                    .collect(Collectors.toSet());
            if (unhealthyThresholds.size() > 0) {
                action.setFailMessage(
                        String.format("Build failed because following metrics did not meet health target: %s.",
                                unhealthyThresholds));
                throw new CoverageException(action.getFailMessage());
            }
        }

        int score;
        if (healthyCount == 0 && unhealthyCount == 0 && unstableCount == 0) {
            score = 100;
        }
        else {
            score = healthyCount * 100 / (healthyCount + unhealthyCount + unstableCount);
        }
        Localizable localizeDescription = Messages._CoverageProcessor_healthReportDescriptionTemplate(score);

        return new HealthReport(score, localizeDescription);
    }

    /**
     * aggregate coverage results into one report
     *
     * @param adapter
     *         CoverageAdapter
     * @param results
     *         CoverageResults converted by adapter
     *
     * @return Coverage report that have all coverage results
     */
    private CoverageResult aggregateToOneReport(final CoverageReportAdapter adapter, final List<CoverageResult> results) {
        CoverageResult report = new CoverageResult(CoverageElement.REPORT, null,
                adapter.getDescriptor().getDisplayName() + ": " + adapter.getPath());

        results.forEach(r -> {
            if (r.getElement().equals(CoverageElement.REPORT)) {
                try {
                    report.merge(r);
                }
                catch (CoverageException e) {
                    listener.getLogger()
                            .printf("Failed to aggregate coverage report %s into one report, reason %s", r.getName(),
                                    e.getMessage());
                }
            }
            else {
                r.resetParent(report);
            }
        });
        return report;
    }

    /**
     * Aggregate results to a aggregated report.
     *
     * @param results
     *         results will be aggregated
     *
     * @return aggregated report
     */
    private CoverageResult aggregateReports(final Map<CoverageReportAdapter, List<CoverageResult>> results) {
        if (results.size() == 0) {
            return null;
        }

        CoverageResult report = new CoverageResult(CoverageElement.AGGREGATED_REPORT, null, "All reports");
        for (List<CoverageResult> resultList : results.values()) {
            for (CoverageResult result : resultList) {
                result.addParent(report);
            }
        }
        return report;
    }

    /**
     * Getter for property 'failUnhealthy'
     *
     * @return value for property 'failUnhealthy'
     */
    public boolean getFailUnhealthy() {
        return failUnhealthy;
    }

    /**
     * Setter for property 'failUnhealthy'
     *
     * @param failUnhealthy
     *         value to set for property 'failUnhealthy'
     */
    public void setFailUnhealthy(final boolean failUnhealthy) {
        this.failUnhealthy = failUnhealthy;
    }

    /**
     * Getter for property 'failUnstable'
     *
     * @return value for property 'failUnstable'
     */
    public boolean getFailUnstable() {
        return failUnstable;
    }

    /**
     * Setter for property 'failUnstable'
     *
     * @param failUnstable
     *         valzue to set for property 'failUnstable'
     */
    public void setFailUnstable(final boolean failUnstable) {
        this.failUnstable = failUnstable;
    }

    /**
     * Getter for property 'failNoReports'
     *
     * @return value for property 'failNoReports'
     */
    public boolean getFailNoReports() {
        return failNoReports;
    }

    /**
     * Setter for property 'failNoReports'
     *
     * @param failNoReports
     *         value to set for property 'failNoReports'
     */
    public void setFailNoReports(final boolean failNoReports) {
        this.failNoReports = failNoReports;
    }

    public void setSourceFileResolver(final SourceFileResolver sourceFileResolver) {
        this.sourceFileResolver = sourceFileResolver;
    }

    public String getGlobalTag() {
        return globalTag;
    }

    public void setGlobalTag(final String globalTag) {
        this.globalTag = globalTag;
    }

    public boolean isApplyThresholdRecursively() {
        return applyThresholdRecursively;
    }

    public void setApplyThresholdRecursively(final boolean applyThresholdRecursively) {
        this.applyThresholdRecursively = applyThresholdRecursively;
    }

    public boolean isFailBuildIfCoverageDecreasedInChangeRequest() {
        return failBuildIfCoverageDecreasedInChangeRequest;
    }

    public void setFailBuildIfCoverageDecreasedInChangeRequest(final boolean failBuildIfCoverageDecreasedInChangeRequest) {
        this.failBuildIfCoverageDecreasedInChangeRequest = failBuildIfCoverageDecreasedInChangeRequest;
    }

    private static class FindReportCallable extends MasterToSlaveFileCallable<FilePath[]> {

        private final String reportFilePath;
        private final CoverageReportAdapter reportAdapter;

        public FindReportCallable(final String reportFilePath, final CoverageReportAdapter reportAdapter) {
            this.reportFilePath = reportFilePath;
            this.reportAdapter = reportAdapter;
        }

        @Override
        public FilePath[] invoke(final File f, final VirtualChannel channel) throws IOException, InterruptedException {

            FilePath[] r = new FilePath(f).list(reportFilePath);

            for (FilePath filePath : r) {
                //TODO check report file (it might should be implement by adapter)
            }
            return r;
        }
    }

    /**
     * Save {@link CoverageResult} in build directory.
     *
     * @param run
     *         build
     * @param report
     *         report
     */
    public static void saveCoverageResult(final Run<?, ?> run, final CoverageResult report) throws IOException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reportFile))) {
            oos.writeObject(report);
        }
    }

    /**
     * Recover {@link CoverageResult} from build directory.
     *
     * @param run
     *         build
     *
     * @return Coverage result
     */
    public static CoverageResult recoverCoverageResult(final Run<?, ?> run) throws IOException, ClassNotFoundException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        try (ObjectInputStream ois = new CompatibleObjectInputStream(new FileInputStream(reportFile))) {
            return (CoverageResult) ois.readObject();
        }
    }
}
