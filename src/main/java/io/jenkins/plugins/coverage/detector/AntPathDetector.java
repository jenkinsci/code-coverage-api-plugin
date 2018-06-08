package io.jenkins.plugins.coverage.detector;

import hudson.Extension;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AntPathDetector extends Detector {

    private String path;

    @DataBoundConstructor
    public AntPathDetector(String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public List<FilePath> findFiles(FilePath workspace) throws IOException, InterruptedException {

        return Arrays.stream(workspace.act(new MasterToSlaveFileCallable<FilePath[]>() {
            @Override
            public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                return workspace.list(path);
            }
        })).collect(Collectors.toList());
    }


    @Symbol("antDetector")
    @Extension
    public static final class AntPathDetectorDescriptor extends DetectorDescriptor<Detector> {

        public AntPathDetectorDescriptor() {
            super(AntPathDetector.class, "Ant Path Detector");
        }
    }

}
