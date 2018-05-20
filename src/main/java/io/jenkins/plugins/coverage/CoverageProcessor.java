package io.jenkins.plugins.coverage;


import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CoverageProcessor {

    public List<CoverageResult> precess(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener, List<CoverageReportAdapter> adapters) throws IOException, InterruptedException {
        return convertToResult(run, workspace, listener, adapters);
    }

    private List<CoverageResult> convertToResult(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull TaskListener listener, List<CoverageReportAdapter> adapters) throws IOException, InterruptedException {
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
                results.add(adapter.getResult(s));
                FileUtils.deleteQuietly(s);
            }
        }
        return results;
    }

    /**
     * check report file
     *
     * @param file
     * @return
     */
    private boolean check(File file) {
        //TODO write a schema file to validate report files
        return true;
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
                //TODO use schema to validate xml file
            }
            return r;
        }
    }
}
