package io.jenkins.plugins.coverage;


import com.google.common.collect.Sets;

import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.RunList;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.detector.Detectable;
import io.jenkins.plugins.coverage.detector.ReportDetector;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.targets.*;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.branch.MultiBranchProject;
import jenkins.MasterToSlaveFileCallable;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMRevision;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jvnet.localizer.Localizable;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoverageProcessor {

    private static final String DEFAULT_REPORT_SAVE_NAME = "coverage-report";

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private String globalTag;

    private boolean calculateDiffForChangeRequests;

    private SourceFileResolver sourceFileResolver;

    /**
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param listener  a place to send output
     */
    public CoverageProcessor(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
    }

    /**
     * Convert all reports that are specified by {@link CoverageReportAdapter}s and detected by {@link ReportDetector}s to {@link CoverageResult},
     * and generate health report from CoverageResult. Add them to {@link CoverageAction} and add Action to {@link Run}.
     *
     * @param reportAdapters   reportAdapters specified by user
     * @param reportDetectors  reportDetectors specified by user
     * @param globalThresholds global threshold specified by user
     */
    public void performCoverageReport(List<CoverageReportAdapter> reportAdapters, List<ReportDetector> reportDetectors, List<Threshold> globalThresholds)
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
                Set<String> paths = coverageResult.getAdditionalProperty(CoverageFeatureConstants.FEATURE_SOURCE_FILE_PATH);
                if (paths != null) {
                    possiblePaths.addAll(paths);
                }
            });

            if (possiblePaths.size() > 0) {
                sourceFileResolver.setPossiblePaths(possiblePaths);
            }

            sourceFileResolver.resolveSourceFiles(run, workspace, listener, coverageReport.getPaintedSources());
        }

        HealthReport healthReport = processThresholds(results, globalThresholds);

        if (calculateDiffForChangeRequests) {
            setDiffInCoverageForChangeRequest(coverageReport);
        }
        convertResultToAction(coverageReport, healthReport);
    }

    private void convertResultToAction(CoverageResult coverageReport, HealthReport healthReport) throws IOException {
        synchronized (CoverageProcessor.class) {

            CoverageAction previousAction = run.getAction(CoverageAction.class);

            if (previousAction == null) {
                saveCoverageResult(run, coverageReport);

                CoverageAction action = new CoverageAction(coverageReport);
                action.setHealthReport(healthReport);
                run.addAction(action);

            } else {
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
                        } catch (CoverageException e) {
                            e.printStackTrace();
                            report.resetParent(previousResult);
                        }
                    } else {
                        report.resetParent(previousResult);
                    }
                }

                previousResult.setOwner(run);
                saveCoverageResult(run, previousResult);
            }
        }
    }

    private void setDiffInCoverageForChangeRequest(CoverageResult coverageReport) {
        // Use limited number of builds to don't search too long
        final int numberOfBuildsFromTargetBranch = 50;

        // Calculate diff only for line coverage
        Ratio changeRequestLinesCoverage = coverageReport.getCoverage(CoverageElement.LINE);
        if (changeRequestLinesCoverage == null) {
            return;
        }

        Job job = run.getParent();
        ItemGroup parent = job.getParent();
        if (!(parent instanceof MultiBranchProject)) {
            return;
        }

        SCMHead currentBuildSCMHead = SCMHead.HeadByItem.findHead(job);
        if (!(currentBuildSCMHead instanceof ChangeRequestSCMHead)) {
            listener.getLogger().println(
                    "Current build is not change request build, won't calculate coverage diff");
            return;
        }

        SCMRevisionAction scmRevisionAction = run.getAction(SCMRevisionAction.class);
        if (scmRevisionAction == null) {
            return;
        }

        SCMRevision currentBuildSCMRevision = scmRevisionAction.getRevision();
        if (!(currentBuildSCMRevision instanceof ChangeRequestSCMRevision)) {
            return;
        }

        // Get target branch revision to compare coverage with
        int targetBranchSCMHash = ((ChangeRequestSCMRevision) currentBuildSCMRevision).getTarget().hashCode();
        SCMHead targetBranchSCMHead = ((ChangeRequestSCMHead) currentBuildSCMHead).getTarget();
        String targetBranchName = targetBranchSCMHead.getName();

        listener.getLogger().println(
                String.format("Calculating coverage change for change request build is enabled, target branch %s", targetBranchName));

        Job targetBranchJob = ((MultiBranchProject) parent).getItemByBranchName(targetBranchName);
        if (targetBranchJob == null) {
            return;
        }

        Run buildToTakeCoverageFrom = null;

        // Search for a build from the target branch job to compare coverage with
        RunList<Run> buildsFromTargetBranchJob = ((RunList<Run>)targetBranchJob.getBuilds()).limit(numberOfBuildsFromTargetBranch);
        for (Run targetBranchBuild: buildsFromTargetBranchJob){
            SCMRevisionAction targetBranchBuildRevision = targetBranchBuild.getAction(SCMRevisionAction.class);
            if (targetBranchBuildRevision != null) {
                if (targetBranchBuildRevision.getRevision().hashCode() == targetBranchSCMHash) {
                    buildToTakeCoverageFrom = targetBranchBuild;
                    break;
                }
            }
        }

        if (buildToTakeCoverageFrom == null) {
            return;
        }
        CoverageAction targetBranchCoverageAction = buildToTakeCoverageFrom.getAction(CoverageAction.class);
        if (targetBranchCoverageAction == null) {
            return;
        }
        CoverageResult targetBranchCoverageResult = targetBranchCoverageAction.getResult();
        if (targetBranchCoverageResult == null) {
            return;
        }
        Ratio targetBranchLinesCoverage = targetBranchCoverageResult.getCoverage(CoverageElement.LINE);
        if (targetBranchLinesCoverage == null) {
            return;
        }
        float percentageDiff =
                changeRequestLinesCoverage.getPercentageFloat() - targetBranchLinesCoverage.getPercentageFloat();
        coverageReport.setChangeRequestCoverageDiffWithTargetBranch(percentageDiff);
        coverageReport.setLinkToBuildThatWasUsedForComparison(buildToTakeCoverageFrom.getUrl());
    }

    /**
     * Convert reports that are specified by {@link CoverageReportAdapter} and detected by {@link ReportDetector}s to {@link CoverageResult}.
     *
     * @param adapters {@link CoverageReportAdapter} for each report
     * @return {@link CoverageResult} for each report
     */
    private Map<CoverageReportAdapter, List<CoverageResult>> convertToResults(List<CoverageReportAdapter> adapters, List<ReportDetector> reportDetectors)
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
                        copy = new File(runRootDir, String.format("%s(%d)", f.getName() , i++));
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
                Map<CoverageReportAdapter, List<File>> detectedReportFiles = reportDetector.getReports(run, workspace, listener, filePath -> {
                    for (Map.Entry<CoverageReportAdapter, Set<FilePath>> entry : reports.entrySet()) {
                        if (entry.getValue().contains(filePath))
                            return false;
                    }
                    return true;
                });

                detectCount += detectedReportFiles.values().stream().mapToInt(List::size).sum();

                for (Map.Entry<CoverageReportAdapter, List<File>> e : detectedReportFiles.entrySet()) {
                    if (copiedReport.containsKey(e.getKey())) {
                        copiedReport.get(e.getKey()).addAll(e.getValue());
                    } else {
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
                    } else {
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
                } catch (CoverageException e) {
                    e.printStackTrace();
                    logger.printf("report %s for %s has met some errors: %s%n",
                            foundedFile.getAbsolutePath(),
                            adapter.getDescriptor().getDisplayName(),
                            e.getMessage());
                } finally {
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
        } else {
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
     * @param adapterWithResults Coverage report adapter and its correspond Coverage results.
     * @param globalThresholds   global threshold
     * @return Health report
     */
    private HealthReport processThresholds(Map<CoverageReportAdapter, List<CoverageResult>> adapterWithResults,
                                           List<Threshold> globalThresholds) throws CoverageException {

        int healthyCount = 0;
        int unhealthyCount = 0;

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
            } else {
                thresholds = globalThresholds;
            }

            for (CoverageResult coverageResult : results.getValue()) {
                resultTask.push(coverageResult);

                while (!resultTask.isEmpty()) {
                    CoverageResult r = resultTask.pollFirst();
                    assert r != null;

                    resultTask.addAll(r.getChildrenReal().values());
                    for (Threshold threshold : thresholds) {
                        Ratio ratio = r.getCoverage(threshold.getThresholdTargetElement());
                        if (ratio == null) {
                            continue;
                        }

                        float percentage = ratio.getPercentageFloat();
                        if (percentage < threshold.getUnhealthyThreshold()) {
                            if (getFailUnhealthy() || threshold.isFailUnhealthy()) {
                                throw new CoverageException(
                                        String.format("Publish Coverage Failed (Unhealthy): %s coverage in %s is lower than %.2f",
                                                threshold.getThresholdTarget(),
                                                r.getName(), threshold.getUnhealthyThreshold()));
                            } else {
                                unhealthyCount++;
                            }
                        } else {
                            healthyCount++;
                        }

                        if (percentage < threshold.getUnstableThreshold()) {
                            if (getFailUnstable()) {
                                throw new CoverageException(
                                        String.format("Publish Coverage Failed (Unstable): %s coverage in %s is lower than %.2f",
                                                threshold.getThresholdTarget(),
                                                r.getName(), threshold.getUnstableThreshold()));
                            } else {
                                run.setResult(Result.UNSTABLE);
                            }
                        }
                    }
                }
            }
        }


        resultTask.addAll(
                adapterWithResults.values().stream()
                        .flatMap((Function<List<CoverageResult>, Stream<CoverageResult>>) Collection::stream)
                        .collect(Collectors.toList()));

        int score;
        if (healthyCount == 0 && unhealthyCount == 0) {
            score = 100;
        } else {
            score = healthyCount * 100 / (healthyCount + unhealthyCount);
        }
        Localizable localizeDescription = Messages._CoverageProcessor_healthReportDescriptionTemplate(score);

        return new HealthReport(score, localizeDescription);
    }


    /**
     * aggregate coverage results into one report
     * @param adapter CoverageAdapter
     * @param results CoverageResults converted by adapter
     * @return Coverage report that have all coverage results
     */
    private CoverageResult aggregateToOneReport(CoverageReportAdapter adapter, List<CoverageResult> results) {
        CoverageResult report = new CoverageResult(CoverageElement.REPORT, null, adapter.getDescriptor().getDisplayName() + ": " + adapter.getPath());

        results.forEach(r -> {
            if (r.getElement().equals(CoverageElement.REPORT)) {
                try {
                    report.merge(r);
                } catch (CoverageException e) {
                    listener.getLogger().printf("Failed to aggregate coverage report %s into one report, reason %s", r.getName(), e.getMessage());
                }
            } else {
                r.resetParent(report);
            }
        });
        return report;
    }

    /**
     * Aggregate results to a aggregated report.
     *
     * @param results results will be aggregated
     * @return aggregated report
     */
    private CoverageResult aggregateReports(Map<CoverageReportAdapter, List<CoverageResult>> results) {
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
     * @param failUnhealthy value to set for property 'failUnhealthy'
     */
    public void setFailUnhealthy(boolean failUnhealthy) {
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
     * @param failUnstable valzue to set for property 'failUnstable'
     */
    public void setFailUnstable(boolean failUnstable) {
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
     * @param failNoReports value to set for property 'failNoReports'
     */
    public void setFailNoReports(boolean failNoReports) {
        this.failNoReports = failNoReports;
    }

    public void setSourceFileResolver(SourceFileResolver sourceFileResolver) {
        this.sourceFileResolver = sourceFileResolver;
    }

    public String getGlobalTag() {
        return globalTag;
    }

    public void setGlobalTag(String globalTag) {
        this.globalTag = globalTag;
    }

    /**
     * Getter for property 'calculateDiffForChangeRequests'
     *
     * @return value for property 'calculateDiffForChangeRequests'
     */
    public boolean getCalculateDiffForChangeRequests() {
        return this.calculateDiffForChangeRequests;
    }

    /**
     * Setter for property 'calculateDiffForChangeRequests'
     *
     * @param calculateDiffForChangeRequests value to set for property 'calculateDiffForChangeRequests'
     */
    public void setCalculateDiffForChangeRequests(boolean calculateDiffForChangeRequests) {
        this.calculateDiffForChangeRequests = calculateDiffForChangeRequests;
    }

    private static class FindReportCallable extends MasterToSlaveFileCallable<FilePath[]> {

        private final String reportFilePath;
        private final CoverageReportAdapter reportAdapter;

        public FindReportCallable(String reportFilePath, CoverageReportAdapter reportAdapter) {
            this.reportFilePath = reportFilePath;
            this.reportAdapter = reportAdapter;
        }


        @Override
        public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

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
     * @param run    build
     * @param report report
     */
    public static void saveCoverageResult(Run<?, ?> run, CoverageResult report) throws IOException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reportFile))) {
            oos.writeObject(report);
        }
    }

    /**
     * Recover {@link CoverageResult} from build directory.
     *
     * @param run build
     * @return Coverage result
     */
    public static CoverageResult recoverCoverageResult(Run<?, ?> run) throws IOException, ClassNotFoundException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        try (ObjectInputStream ois = new CompatibleObjectInputStream(new FileInputStream(reportFile))) {
            return (CoverageResult) ois.readObject();
        }
    }
}
