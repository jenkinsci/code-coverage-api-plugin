package io.jenkins.plugins.coverage;


import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.exception.ConversionException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CoverageProcessor {

    private static final String DEFAULT_REPORT_SAVE_NAME = "coverage-report.xml";

    public CoverageResult getCoverageReport(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener, CoverageReportAdapter[] adapters) throws IOException, InterruptedException {
        List<CoverageResult> results = convertToResults(run, workspace, listener, adapters);
        CoverageResult report = aggregatedToReport(results);

        saveReport(run, report);

        return report;
    }

    private List<CoverageResult> convertToResults(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener, CoverageReportAdapter[] adapters) throws IOException, InterruptedException {
        Map<CoverageReportAdapter, FilePath[]> reports = new HashMap<>();

        for (CoverageReportAdapter adapter : adapters) {
            String path = adapter.getPath();

            FilePath[] r = workspace.act(new ParseReportCallable(path, adapter));
            reports.put(adapter, r);
        }

        if (reports.size() == 0) {
            listener.getLogger().println("No reports were found in this path");
        }

        File runRootDir = run.getRootDir();
        Map<CoverageReportAdapter, File[]> copiedReportFile = new HashMap<>();

        for (Map.Entry<CoverageReportAdapter, FilePath[]> adapterReports : reports.entrySet()) {
            FilePath[] r = adapterReports.getValue();
            File[] copies = new File[r.length];

            for (int i = 0; i < r.length; i++) {
                File copy = new File(runRootDir, r[i].getName() + i);
                r[i].copyTo(new FilePath(copy));
                copies[i] = copy;
            }
            copiedReportFile.put(adapterReports.getKey(), copies);
        }
        reports.clear();

        List<CoverageResult> results = new LinkedList<>();
        for (Map.Entry<CoverageReportAdapter, File[]> adapterReports : copiedReportFile.entrySet()) {
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

    private CoverageResult aggregatedToReport(List<CoverageResult> results) {
        CoverageResult report = new CoverageResult(CoverageElement.AGGREGATED_REPORT, null, "All reports");
        for (CoverageResult result : results) {
            result.addParent(report);
        }
        return report;
    }


    private static class ParseReportCallable extends MasterToSlaveFileCallable<FilePath[]> {

        private final String reportFilePath;
        private final CoverageReportAdapter reportAdapter;

        public ParseReportCallable(String reportFilePath, CoverageReportAdapter reportAdapter) {
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

    public static void saveReport(Run<?, ?> run, CoverageResult report) throws IOException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reportFile));
        oos.writeObject(report);
    }

    public static CoverageResult recoverReport(Run<?, ?> run) throws IOException, ClassNotFoundException {
        File reportFile = new File(run.getRootDir(), DEFAULT_REPORT_SAVE_NAME);

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(reportFile));
        return (CoverageResult) ois.readObject();
    }
}
