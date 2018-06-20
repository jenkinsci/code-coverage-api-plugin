package io.jenkins.plugins.coverage.detector;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.coverage.adapter.CoverageAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;
import io.jenkins.plugins.coverage.adapter.CoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.apache.commons.io.FileUtils;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class ReportDetector extends CoverageAdapter {

    protected abstract List<FilePath> findFiles(Run<?, ?> run, FilePath workspace, TaskListener listener) throws CoverageException;


    public Map<CoverageReportAdapter, List<File>> getReports(Run<?, ?> run, FilePath workspace, TaskListener listener, @CheckForNull Predicate<? super FilePath> includeOnly) throws CoverageException {
        try {
            List<FilePath> detectedFilePaths = findFiles(run, workspace, listener);

            if (includeOnly != null) {
                detectedFilePaths = detectedFilePaths.stream()
                        .filter(includeOnly)
                        .collect(Collectors.toList());
            }

            return detectReports(detectedFilePaths, run);
        } catch (IOException | InterruptedException | ReflectiveOperationException e) {
            throw new CoverageException(e);
        }
    }

    public Map<CoverageReportAdapter, List<File>> getReports(Run<?, ?> run, FilePath workspace, TaskListener listener) throws CoverageException {
        return getReports(run, workspace, listener, null);
    }

    /**
     * Find all file that can be matched by detectable report adapter.
     *
     * @param detectedFilePaths path of detected files
     * @return {@link CoverageReportAdapter} and matched file.
     */
    protected Map<CoverageReportAdapter, List<File>> detectReports(List<FilePath> detectedFilePaths, Run<?, ?> run) throws IOException, InterruptedException, ReflectiveOperationException {
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
                        @SuppressWarnings("unchecked")
                        Constructor c = clazz.getConstructor(String.class);
                        if (c == null) continue;

                        CoverageReportAdapter adapter = (CoverageReportAdapter) c.newInstance("");
                        results.putIfAbsent(adapter, new LinkedList<>());
                        results.get(adapter).add(copy);
                    } else {
                        FileUtils.deleteQuietly(copy);
                    }
                }
            }
        }

        return results;
    }


    /**
     * Find all detectable {@link CoverageReportAdapterDescriptor}.
     *
     * @return Detectable CoverageReportAdapterDescriptors
     */
    @SuppressWarnings("unchecked")
    private List<CoverageReportAdapterDescriptor> findDetectableReportDescriptors() {
        return CoverageAdapterDescriptor.all()
                .stream()
                .filter(d -> d instanceof CoverageReportAdapterDescriptor && d instanceof Detectable)
                .map(d -> (CoverageReportAdapterDescriptor) d)
                .collect(Collectors.toList());
    }


}
