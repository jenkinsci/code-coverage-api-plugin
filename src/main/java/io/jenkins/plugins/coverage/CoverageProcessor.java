package io.jenkins.plugins.coverage;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.detector.Detectable;
import io.jenkins.plugins.coverage.detector.ReportDetector;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;
import org.jvnet.localizer.Localizable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private SourceFileResolver sourceFileResolver;
    private String branchName;

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

        CoverageResult coverageReport = aggregatedResults(results);
        if (coverageReport == null) {
            return;
        }

        coverageReport.setOwner(run);

        if (sourceFileResolver != null) {
            sourceFileResolver.resolveSourceFiles(run, workspace, listener, coverageReport.getPaintedSources());
        }

        HealthReport healthReport = processThresholds(results, globalThresholds);

        convertResultsToAction(coverageReport, healthReport);
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
                    File copy = new File(runRootDir, f.getBaseName());

                    //if copy exist, it means there have reports have same name.
                    int i = 1;
                    while (copy.exists()) {
                        copy = new File(copy.getName() + i++);
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


    private void convertResultsToAction(CoverageResult coverageReport, HealthReport healthReport) throws IOException {
        synchronized (CoverageProcessor.class) {
            CoverageAction previousAction = run.getAction(CoverageAction.class);

            if (previousAction != null) {
                CoverageResult previousResult = previousAction.getResult();

                int branchCount = previousAction.getBranchCount();
                if (branchCount == 1) {
                    String branchNameOfFirstCall = previousAction.getCacheBranchName();
                    // if null, we need to generate a suffix to differentiate them.
                    if (branchNameOfFirstCall == null) {
                        String defaultBranchName = "Branch " + branchCount;

                        List<CoverageResult> renamedReport = Lists.newArrayList(previousResult
                                .getChildrenReal()
                                .values());

                        previousResult.getChildrenReal().clear();
                        renamedReport.forEach(result -> {
                            result.setName(result.getName() + "-" + defaultBranchName);
                            result.resetParent(previousResult);
                        });
                    }
                    branchCount++;
                }

                for (CoverageResult child : coverageReport.getChildrenReal().values()) {
                    if(branchName == null) {
                        child.setName(child.getName() + "-" + "Branch" + branchCount);
                    }
                    child.resetParent(previousResult);
                }

                previousAction.setBranchCount(branchCount + 1);
                previousResult.setOwner(run);

                saveCoverageResult(run, previousResult);
            } else {
                saveCoverageResult(run, coverageReport);

                CoverageAction action = new CoverageAction(coverageReport);
                action.setCacheBranchName(branchName);
                action.setHealthReport(healthReport);

                run.addAction(action);
            }
        }
    }

    /**
     * Aggregate results to a aggregated report.
     *
     * @param results results will be aggregated
     * @return aggregated report
     */
    private CoverageResult aggregatedResults(Map<CoverageReportAdapter, List<CoverageResult>> results) {
        if (results.size() == 0) {
            return null;
        }

        CoverageResult report = new CoverageResult(CoverageElement.AGGREGATED_REPORT, null, "All reports");
        for (List<CoverageResult> resultList : results.values()) {
            for (CoverageResult result : resultList) {
                if(branchName != null) {
                    result.setName(result.getName() + "-" + branchName);
                }

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

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setSourceFileResolver(SourceFileResolver sourceFileResolver) {
        this.sourceFileResolver = sourceFileResolver;
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

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(reportFile))) {
            return (CoverageResult) ois.readObject();
        }
    }
}
