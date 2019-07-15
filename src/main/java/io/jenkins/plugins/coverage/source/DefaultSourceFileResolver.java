/*
 * Copyright (c) 2007-2018 Stephen Connolly, Michael Barrientos, Michael Barrientos, Shenyu Zheng and Jenkins contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.coverage.source;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.coverage.BuildUtils;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DefaultSourceFileResolver extends SourceFileResolver {

    public static final String DEFAULT_SOURCE_CODE_STORE_DIRECTORY = "coverage-sources/";

    @DataBoundConstructor
    public DefaultSourceFileResolver(SourceFileResolverLevel level) {
        super(level);
    }

    public void resolveSourceFiles(Run<?, ?> run, FilePath workspace, TaskListener listener, Map<String, CoveragePaint> paints) throws IOException {
        if (getLevel() == null || getLevel().equals(SourceFileResolverLevel.NEVER_STORE)) {
            return;
        }
        File runRootDir = run.getRootDir();

        listener.getLogger().printf("Source File Navigation is enabled - Current level: %s%n", getLevel());

        if (getLevel().equals(SourceFileResolverLevel.STORE_LAST_BUILD)) {
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

        listener.getLogger().printf("%d source files need to be copied%n", paints.size());

        ExecutorService service = Executors.newFixedThreadPool(5);
        // wait until all tasks completed
        CountDownLatch latch = new CountDownLatch(paints.entrySet().size());

        paints.forEach((sourceFilePath, paint) -> service.submit(() -> {
            try {
                FilePath[] possibleFiles;
                try {
                    if (getPossiblePaths() != null && getPossiblePaths().size() > 0) {
                        possibleFiles = workspace.act(new FindSourceFileCallable(sourceFilePath, getPossiblePaths()));
                    } else {
                        possibleFiles = workspace.act(new FindSourceFileCallable(sourceFilePath));
                    }
                } catch (IOException | InterruptedException e) {
                    listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
                    return;
                }
                if (possibleFiles != null && possibleFiles.length > 0) {
                    FilePath source = possibleFiles[0];
                    FilePath copiedSource = new FilePath(new File(runRootDir, DEFAULT_SOURCE_CODE_STORE_DIRECTORY + sourceFilePath + "_copied"));
                    try {
                        source.copyTo(copiedSource);
                    } catch (IOException | InterruptedException e) {
                        listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
                        return;
                    }

                    FilePath buildDirSourceFile = new FilePath(new File(runRootDir, DEFAULT_SOURCE_CODE_STORE_DIRECTORY + sourceFilePath));

                    try {
                        paintSourceCode(copiedSource, paint, buildDirSourceFile);
                    } catch (CoverageException e) {
                        listener.getLogger().println(ExceptionUtils.getFullStackTrace(e));
                    }

                    deleteFilePathQuietly(copiedSource);
                } else {
                    listener.getLogger().printf("Cannot found source file for %s%n", sourceFilePath);
                }
            } finally {
                // ensure latch will count down
                latch.countDown();
            }
        }));


        try {
            //TODO make this configurable
            latch.await(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException("Unable to copy source files", e);
        }
    }


    private void paintSourceCode(FilePath source, CoveragePaint paint, FilePath canvas) throws CoverageException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(canvas.write(), StandardCharsets.UTF_8));
             BufferedReader input = new BufferedReader(new InputStreamReader(source.read(), StandardCharsets.UTF_8))) {
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

    private void deleteFilePathQuietly(FilePath file) {
        try {
            file.delete();
        } catch (IOException | InterruptedException ignore) {
        }
    }


    @Symbol("sourceFiles")
    @Extension
    public static final class DefaultSourceFileResolverDescriptor<T extends SourceFileResolver> extends Descriptor<SourceFileResolver> {

        private static final ListBoxModel LEVELS = new ListBoxModel(
                new ListBoxModel.Option(SourceFileResolver.SourceFileResolverLevel.NEVER_STORE.getName(), SourceFileResolver.SourceFileResolverLevel.NEVER_STORE.toString()),
                new ListBoxModel.Option(SourceFileResolver.SourceFileResolverLevel.STORE_LAST_BUILD.getName(), SourceFileResolver.SourceFileResolverLevel.STORE_LAST_BUILD.toString()),
                new ListBoxModel.Option(SourceFileResolver.SourceFileResolverLevel.STORE_ALL_BUILD.getName(), SourceFileResolver.SourceFileResolverLevel.STORE_ALL_BUILD.toString()));

        public DefaultSourceFileResolverDescriptor() {
            super(DefaultSourceFileResolver.class);
        }

        public ListBoxModel doFillLevelItems() {
            return LEVELS;
        }
    }

    private static class FindSourceFileCallable extends MasterToSlaveFileCallable<FilePath[]> {
        private String sourceFilePath;
        private Set<String> possiblePaths;

        public FindSourceFileCallable(String sourceFilePath) {
            this(sourceFilePath, Collections.emptySet());
        }

        public FindSourceFileCallable(String sourceFilePath, Set<String> possiblePaths) {
            this.sourceFilePath = sourceFilePath;
            this.possiblePaths = possiblePaths;
        }

        @Override
        public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            List<File> possibleDirectories = new LinkedList<>();

            for (String directory : possiblePaths) {
                File pathFromRoot = new File(directory);
                if (pathFromRoot.exists() && pathFromRoot.isDirectory()) {
                    possibleDirectories.add(pathFromRoot);
                }

                File pathFromWorkDir = new File(f, directory);
                if (pathFromWorkDir.exists() && pathFromWorkDir.isDirectory() && !pathFromWorkDir.equals(pathFromRoot)) {
                    possibleDirectories.add(pathFromWorkDir);
                }
            }


            File sourceFile = new File(f, sourceFilePath);
            if (sourceFile.exists() && sourceFile.isFile() && sourceFile.canRead()) {
                return new FilePath[]{new FilePath(sourceFile)};
            }

            for (File directory : possibleDirectories) {
                sourceFile = new File(directory, sourceFilePath);
                if (sourceFile.exists() && sourceFile.isFile() && sourceFile.canRead()) {
                    return new FilePath[]{new FilePath(sourceFile)};
                }
            }

            return new FilePath(f).list("**/" + sourceFilePath);
        }
    }
}
