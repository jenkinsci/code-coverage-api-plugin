package io.jenkins.plugins.coverage.source;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.coverage.BuildUtils;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

public class SourceFileResolver implements ExtensionPoint, Describable<SourceFileResolver> {

    public static final String DEFAULT_SOURCE_CODE_STORE_DIRECTORY = "coverage-sources/";

    private transient Run<?, ?> run;
    private transient FilePath workspace;
    private transient TaskListener listener;

    private SourceFileResolverLevel level;


    @DataBoundConstructor
    public SourceFileResolver(SourceFileResolverLevel level) {
        this.level = level;
    }

    public void resolveSourceFiles(Map<String, CoveragePaint> paints) throws IOException, InterruptedException, CoverageException {
        if (level == null || level.equals(SourceFileResolverLevel.NEVER_STORE)) {
            return;
        }

        if (level.equals(SourceFileResolverLevel.STORE_LAST_BUILD)) {
            Run<?, ?> lastBuild = BuildUtils.getPreviousNotFailedCompletedBuild(run);

            // only store source files in this build and the last not failed completed build.
            if (lastBuild != null) {
                Run<?, ?> b = BuildUtils.getPreviousNotFailedCompletedBuild(lastBuild);
                if (b != null) {
                    File sourceFileDir = new File(b.getRootDir(), "coverage-sources");

                    if (sourceFileDir.exists() && sourceFileDir.isDirectory()) {
                        FileUtils.deleteDirectory(sourceFileDir);
                    }
                }
            }
        }

        paints.forEach((sourceFilePath, paint) -> {
            FilePath[] possibleFiles;
            try {
                possibleFiles = workspace.act(new MasterToSlaveFileCallable<FilePath[]>() {
                    @Override
                    public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                        return new FilePath(f).list("**/" + sourceFilePath); // husond/io/aa/DD.java
                    }
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return;
            }
            if (possibleFiles != null && possibleFiles.length > 0) {
                FilePath source = possibleFiles[0];
                FilePath buildDirSourceFile = new FilePath(new File(run.getRootDir(), DEFAULT_SOURCE_CODE_STORE_DIRECTORY + sourceFilePath));

                try {
                    paintSourceCode(source, paint, buildDirSourceFile);
                } catch (CoverageException e) {
                    listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
                }
            } else {
                listener.getLogger().printf("Cannot found source file for %s", sourceFilePath);
            }
        });

    }


    private void paintSourceCode(FilePath source, CoveragePaint paint, FilePath canvas) throws CoverageException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(canvas.write(), "UTF-8"));
             BufferedReader input = new BufferedReader(new InputStreamReader(source.read(), "UTF-8"))) {
            int line = 0;
            String content;
            while ((content = input.readLine()) != null) {
                line++;

                if (paint.isPainted(line)) {
                    final int hits = paint.getHits(line);
                    final int branchCoverage = paint.getBranchCoverage(line);
                    final int branchTotal = paint.getBranchTotal(line);
                    final int coveragePercent = (hits == 0) ? 0 : (int) (branchCoverage * 100.0 / branchTotal);
                    if (paint.getHits(line) > 0) {
                        if (branchTotal == branchCoverage) {
                            output.write("<tr class=\"coverFull\">\n");
                        } else {
                            output.write("<tr class=\"coverPart\" title=\"Line " + line + ": Conditional coverage " + coveragePercent + "% ("
                                    + branchCoverage + "/" + branchTotal + ")\">\n");
                        }
                    } else {
                        output.write("<tr class=\"coverNone\">\n");
                    }
                    output.write("<td class=\"line\"><a name='" + line + "'/>" + line + "</td>\n");
                    output.write("<td class=\"hits\">" + hits + "</td>\n");
                } else {
                    output.write("<tr class=\"noCover\">\n");
                    output.write("<td class=\"line\"><a name='" + line + "'/>" + line + "</td>\n");
                    output.write("<td class=\"hits\"/>\n");
                }
                output.write("<td class=\"code\">"
                        + content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "").replace("\r", "").replace(" ",
                        "&nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "</td>\n");
                output.write("</tr>\n");
            }

            paint.setTotalLines(line);
        } catch (IOException | InterruptedException e) {
            throw new CoverageException(e);
        }
    }


    public void setOwnerBuild(Run<?, ?> run, FilePath workspace, TaskListener listener) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
    }

    public SourceFileResolverLevel getLevel() {
        return level;
    }

    public void setLevel(SourceFileResolverLevel level) {
        this.level = level;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<SourceFileResolver> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static final class SourceFileResolverDescriptor<T extends SourceFileResolver> extends Descriptor<SourceFileResolver> {

        public static final ListBoxModel LEVELS = new ListBoxModel(
                new ListBoxModel.Option(SourceFileResolverLevel.NEVER_STORE.getName(), SourceFileResolverLevel.NEVER_STORE.toString()),
                new ListBoxModel.Option(SourceFileResolverLevel.STORE_LAST_BUILD.getName(), SourceFileResolverLevel.STORE_LAST_BUILD.toString()),
                new ListBoxModel.Option(SourceFileResolverLevel.STORE_ALL_BUILD.getName(), SourceFileResolverLevel.STORE_ALL_BUILD.toString()));

        public SourceFileResolverDescriptor() {
            super(SourceFileResolver.class);
        }

        public ListBoxModel doFillLevelItems() {
            return LEVELS;
        }
    }

    public enum SourceFileResolverLevel {
        NEVER_STORE(Messages.SourceFileResolver_neverSave()),
        STORE_LAST_BUILD(Messages.SourceFileResolver_saveLast()),
        STORE_ALL_BUILD(Messages.SourceFileResolver_saveAll());

        private String name;

        SourceFileResolverLevel(String name) {
            this.name = name;
        }


        public String getName() {
            return name;
        }
    }
}
