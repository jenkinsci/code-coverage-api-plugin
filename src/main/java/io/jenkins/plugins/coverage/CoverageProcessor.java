package io.jenkins.plugins.coverage;


import com.google.common.collect.Sets;
import hudson.DescriptorExtensionList;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.Detectable;
import io.jenkins.plugins.coverage.exception.ConversionException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.threshold.Threshold;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Shenyu Zheng
 */
public class CoverageProcessor {

    private static final String DEFAULT_REPORT_SAVE_NAME = "coverage-report.xml";

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;
    private List<CoverageReportAdapter> adapters;
    private List<Threshold> globalThresholds;


    private boolean enableAutoDetect;
    private String autoDetectPath;

    public CoverageProcessor(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener,
                             List<CoverageReportAdapter> adapters, List<Threshold> globalThresholds) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
        this.adapters = adapters;
        this.globalThresholds = globalThresholds;
    }


    public CoverageResult processCoverageReport() throws IOException, InterruptedException {
        List<CoverageResult> results = convertToResults(adapters);
        CoverageResult report = aggregatedToReport(results);

        saveReport(run, report);

        return report;
    }

    /**
     * Convert report specified by {@link CoverageReportAdapter} into coverage results.
     *
     * @param adapters {@link CoverageReportAdapter} for each report
     * @return {@link CoverageResult} for each report
     */
    private List<CoverageResult> convertToResults(List<CoverageReportAdapter> adapters)
            throws IOException, InterruptedException {

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
            listener.getLogger().println("Auto Detect is enable: Start to detect report");
            List<FilePath> detectedFilePaths = Arrays.stream(workspace.act(new FindReportCallable(autoDetectPath, null)))
                    .filter(filePath -> {
                        for (Map.Entry<CoverageReportAdapter, Set<FilePath>> entry : reports.entrySet()) {
                            if (entry.getValue().contains(filePath))
                                return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
            listener.getLogger().printf("Auto Detect was ended: Found %d report\n", detectedFilePaths.size());

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
            listener.getLogger().println("No reports were found in this path");
        } else {
            listener.getLogger().printf("A total of %d reports were found\n", copiedReport.size());
        }

        reports.clear();


        List<CoverageResult> results = new LinkedList<>();
        for (Map.Entry<CoverageReportAdapter, List<File>> adapterReports : copiedReport.entrySet()) {
            CoverageReportAdapter adapter = adapterReports.getKey();
            for (File s : adapterReports.getValue()) {
                try {
                    results.add(adapter.getResult(s));
                } catch (ConversionException e) {
                    e.printStackTrace();
                    listener.getLogger().printf("report for %s has met some errors: %s",
                            adapter.getDescriptor().getDisplayName(), e.getMessage());
                }
                FileUtils.deleteQuietly(s);
            }
        }
        return results;
    }

    /**
     * Find all file that can match report adapter
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

    private CoverageResult aggregatedToReport(List<CoverageResult> results) {
        CoverageResult report = new CoverageResult(CoverageElement.AGGREGATED_REPORT, null, "All reports");
        for (CoverageResult result : results) {
            result.addParent(report);
        }
        return report;
    }

    /**
     * Find all detectable {@link CoverageReportAdapterDescriptor}
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
    public void enableAutoDetect(String autoDetectPath) {
        this.enableAutoDetect = true;
        this.autoDetectPath = autoDetectPath;
    }

    public boolean isEnableAutoDetect() {
        return enableAutoDetect;
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


    public List<Threshold> getGlobalThresholds() {
        return globalThresholds;
    }

    public static void saveReport(Run<?, ?> run, CoverageResult report) throws IOException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reportFile))) {
            oos.writeObject(report);
        }
    }

    public static CoverageResult recoverReport(Run<?, ?> run) throws IOException, ClassNotFoundException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(reportFile))) {
            return (CoverageResult) ois.readObject();
        }
    }
}
