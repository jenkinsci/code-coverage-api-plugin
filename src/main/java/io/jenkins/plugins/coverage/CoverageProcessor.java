package io.jenkins.plugins.coverage;


import com.google.common.collect.Sets;
import hudson.DescriptorExtensionList;
import hudson.FilePath;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.Detectable;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jvnet.localizer.Localizable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoverageProcessor {

    private static final String DEFAULT_REPORT_SAVE_NAME = "coverage-report.xml";

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;

    private boolean failUnhealthy;
    private boolean failUnstable;
    private boolean failNoReports;

    private String autoDetectPath;


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
     * Convert all reports specified by adapters and detected by processor to {@link CoverageResult},
     * and generate health report from CoverageResult. Add them to {@link CoverageAction} and add Action to {@link Run}.
     *
     * @param adapters         adapters specified by user
     * @param globalThresholds global threshold specified by user
     */
    public void performCoverageReport(List<CoverageReportAdapter> adapters, List<Threshold> globalThresholds) throws IOException, InterruptedException, CoverageException {
        Map<CoverageReportAdapter, List<CoverageResult>> results = convertToResults(adapters);
        CoverageResult coverageReport = aggregatedResults(results);

        coverageReport.setOwner(run);
        HealthReport healthReport = processThresholds(results, globalThresholds);

        saveCoverageResult(run, coverageReport);

        CoverageAction action = new CoverageAction(coverageReport);
        action.setHealthReport(healthReport);
        run.addAction(action);

    }

    /**
     * Convert reports that specified by {@link CoverageReportAdapter} and detected by processor to {@link CoverageResult}.
     *
     * @param adapters {@link CoverageReportAdapter} for each report
     * @return {@link CoverageResult} for each report
     */
    private Map<CoverageReportAdapter, List<CoverageResult>> convertToResults(List<CoverageReportAdapter> adapters)
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

        // If enable automatically detecting, it will try to find report and correspond adapter.
        if (isEnableAutoDetect()) {
            logger.println("Auto Detect is enabled: Looking for reports...");
            List<FilePath> detectedFilePaths = Arrays.stream(workspace.act(new FindReportCallable(autoDetectPath, null)))
                    .filter(filePath -> {
                        for (Map.Entry<CoverageReportAdapter, Set<FilePath>> entry : reports.entrySet()) {
                            if (entry.getValue().contains(filePath))
                                return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
            logger.printf("Auto Detect was ended: Found %d report%n", detectedFilePaths.size());

            try {
                Map<CoverageReportAdapter, List<File>> detectedReportFiles = detectReports(detectedFilePaths);
                for (Map.Entry<CoverageReportAdapter, List<File>> e : detectedReportFiles.entrySet()) {
                    if (copiedReport.containsKey(e.getKey())) {
                        copiedReport.get(e.getKey()).addAll(e.getValue());
                    } else {
                        copiedReport.put(e.getKey(), e.getValue());
                    }
                }
            } catch (ReflectiveOperationException ignore) {
            }

        }

        if (copiedReport.size() == 0) {
            logger.println("No reports were found in this path");
            if (getFailNoReports()) {
                throw new CoverageException("Publish Coverage Failed : No Reports were found");
            }
        } else {
            logger.printf("A total of %d reports were found%n", copiedReport.size());
        }

        reports.clear();

        // convert report to results
        Map<CoverageReportAdapter, List<CoverageResult>> results = new HashMap<>();
        for (Map.Entry<CoverageReportAdapter, List<File>> adapterReports : copiedReport.entrySet()) {
            CoverageReportAdapter adapter = adapterReports.getKey();
            for (File s : adapterReports.getValue()) {
                try {
                    results.putIfAbsent(adapter, new LinkedList<>());
                    results.get(adapter).add(adapter.getResult(s));
                } catch (CoverageException e) {
                    e.printStackTrace();
                    logger.printf("report for %s has met some errors: %s",
                            adapter.getDescriptor().getDisplayName(), e.getMessage());
                }
                FileUtils.deleteQuietly(s);
            }
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
                        Ratio ratio = r.getCoverage(threshold.getThresholdTarget());
                        if (ratio == null) {
                            continue;
                        }

                        float percentage = ratio.getPercentageFloat();
                        if (percentage < threshold.getUnhealthyThreshold()) {
                            if (getFailUnhealthy() || threshold.isFailUnhealthy()) {
                                throw new CoverageException(
                                        String.format("Publish Coverage Failed (Unhealthy): %s coverage in %s is lower than %.2f",
                                                threshold.getThresholdTarget().getName(),
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
                                                threshold.getThresholdTarget().getName(),
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
     * Find all file that can be matched by detectable report adapter.
     *
     * @param detectedFilePaths path of detected files
     * @return {@link CoverageReportAdapter} and matched file.
     */
    private Map<CoverageReportAdapter, List<File>> detectReports(List<FilePath> detectedFilePaths) throws IOException, InterruptedException, ReflectiveOperationException {
        List<CoverageReportAdapterDescriptor> detectableReportDescriptors = findDetectableReportDescriptors();
        Map<CoverageReportAdapter, List<File>> results = new HashMap<>();

        File rootBuildDir = run.getRootDir();
        for (FilePath fp : detectedFilePaths) {
            // The suffix (D) means the report is found by auto detect
            File copy = new File(rootBuildDir, fp.getBaseName() + "(D)");


            //if copy exist, it means there have reports have same name.
            int i = 0;
            while (copy.exists()) {
                copy = new File(copy.getName() + i++);
            }

            fp.copyTo(new FilePath(copy));

            for (CoverageReportAdapterDescriptor d : detectableReportDescriptors) {
                if (d instanceof Detectable) {
                    Detectable detectable = ((Detectable) d);
                    if (detectable.detect(copy)) {
                        Class clazz = d.clazz;
                        Constructor c = clazz.getConstructor(String.class);
                        if (c == null) continue;

                        CoverageReportAdapter adapter = (CoverageReportAdapter) c.newInstance("");
                        results.putIfAbsent(adapter, new LinkedList<>());
                        results.get(adapter).add(copy);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Aggregate results to a aggregated report.
     *
     * @param results results will be aggregated
     * @return aggregated report
     */
    private CoverageResult aggregatedResults(Map<CoverageReportAdapter, List<CoverageResult>> results) {
        CoverageResult report = new CoverageResult(CoverageElement.AGGREGATED_REPORT, null, "All reports");
        for (List<CoverageResult> resultList : results.values()) {
            for (CoverageResult result : resultList) {
                result.addParent(report);
            }
        }
        return report;
    }

    /**
     * Find all detectable {@link CoverageReportAdapterDescriptor}.
     *
     * @return Detectable CoverageReportAdapterDescriptors
     */
    @SuppressWarnings("unchecked")
    private List<CoverageReportAdapterDescriptor> findDetectableReportDescriptors() {
        DescriptorExtensionList<CoverageReportAdapter, CoverageReportAdapterDescriptor<?>>
                availableCoverageReportDescriptors = CoverageReportAdapterDescriptor.all();

        List<CoverageReportAdapterDescriptor> results = new LinkedList<>();
        Iterator<CoverageReportAdapterDescriptor<?>> i = availableCoverageReportDescriptors.iterator();
        while (i.hasNext()) {
            CoverageReportAdapterDescriptor c = i.next();
            if (c instanceof Detectable) {
                results.add(c);
            }
        }

        return results;
    }

    /**
     * Enable report auto-detect
     *
     * @param autoDetectPath Ant-Style path to specify coverage report
     */
    public void setAutoDetectPath(String autoDetectPath) {
        this.autoDetectPath = autoDetectPath;
    }

    /**
     * @return <code>true</code> if auto detect is enable
     */
    public boolean isEnableAutoDetect() {
        return !StringUtils.isEmpty(autoDetectPath);
    }

    /**
     * Getter for property 'failUnhealthy'
     * @return value for property 'failUnhealthy'
     */
    public boolean getFailUnhealthy() {
        return failUnhealthy;
    }

    /**
     * Setter for property 'failUnhealthy'
     * @param failUnhealthy value to set for property 'failUnhealthy'
     */
    public void setFailUnhealthy(boolean failUnhealthy) {
        this.failUnhealthy = failUnhealthy;
    }

    /**
     * Getter for property 'failUnstable'
     * @return value for property 'failUnstable'
     */
    public boolean getFailUnstable() {
        return failUnstable;
    }

    /**
     * Setter for property 'failUnstable'
     * @param failUnstable value to set for property 'failUnstable'
     */
    public void setFailUnstable(boolean failUnstable) {
        this.failUnstable = failUnstable;
    }

    /**
     * Getter for property 'failNoReports'
     * @return value for property 'failNoReports'
     */
    public boolean getFailNoReports() {
        return failNoReports;
    }

    /**
     * Setter for property 'failNoReports'
     * @param failNoReports value to set for property 'failNoReports'
     */
    public void setFailNoReports(boolean failNoReports) {
        this.failNoReports = failNoReports;
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
