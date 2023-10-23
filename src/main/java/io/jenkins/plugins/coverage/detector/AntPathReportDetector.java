package io.jenkins.plugins.coverage.detector;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AntPathReportDetector extends ReportDetector {

    private String path;

    @DataBoundConstructor
    public AntPathReportDetector(String path) {
        this.path = path;
    }

    @Override
    public List<FilePath> findFiles(Run<?, ?> run, FilePath workspace, TaskListener listener) throws CoverageException {
        try {
            return Arrays.asList(workspace.list(path));
        } catch (IOException | InterruptedException e) {
            throw new CoverageException(e);
        }
    }

    public String getPath() {
        return path;
    }

    @Symbol("antPath")
    @Extension
    public static final class AntPathReportDetectorDescriptor extends CoverageAdapterDescriptor<AntPathReportDetector> {

        public AntPathReportDetectorDescriptor() {
            super(AntPathReportDetector.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.AntPathDetector_displayName();
        }
    }
}
