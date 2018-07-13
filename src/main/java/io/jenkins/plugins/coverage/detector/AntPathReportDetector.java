package io.jenkins.plugins.coverage.detector;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.coverage.adapter.CoverageAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AntPathReportDetector extends ReportDetector {

    private String path;

    @DataBoundConstructor
    public AntPathReportDetector(String path) {
        this.path = path;
    }

    @Override
    public List<FilePath> findFiles(Run<?, ?> run, FilePath workspace, TaskListener listener) throws CoverageException {
        try {
            return Arrays.stream(workspace.act(new MasterToSlaveFileCallable<FilePath[]>() {
                @Override
                public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    return workspace.list(path);
                }
            })).collect(Collectors.toList());
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

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.AntPathDetector_displayName();
        }
    }
}
