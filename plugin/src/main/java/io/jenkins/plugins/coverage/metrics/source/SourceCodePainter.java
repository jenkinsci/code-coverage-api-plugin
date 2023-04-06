package io.jenkins.plugins.coverage.metrics.source;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.FilePath;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.ValidationUtilities;

/**
 * Highlights the code coverage information in all source code files. This process is executed on the agent node that
 * has all source files checked out.
 */
public class SourceCodePainter {
    private final Run<?, ?> build;
    private final FilePath workspace;
    private final String id;

    /**
     * Creates a painter for the passed build, using the passed properties.
     *
     * @param build
     *         The build which processes the source code
     * @param workspace
     *         The workspace which contains the source code files
     * @param id
     *         the ID of the coverage results - each ID will store the files in a separate directory
     */
    public SourceCodePainter(@NonNull final Run<?, ?> build, @NonNull final FilePath workspace, final String id) {
        this.build = build;
        this.workspace = workspace;
        this.id = id;
    }

    /**
     * Processes the source code painting.
     *
     * @param files
     *         the files to paint
     * @param sourceCodeEncoding
     *         the encoding of the source code files
     * @param sourceCodeRetention
     *         the source code retention strategy
     * @param log
     *         The log
     *
     * @throws InterruptedException
     *         if the painting process has been interrupted
     */
    public void processSourceCodePainting(final List<FileNode> files,
            final String sourceCodeEncoding,
            final SourceCodeRetention sourceCodeRetention, final FilteredLog log)
            throws InterruptedException {
        SourceCodeFacade sourceCodeFacade = new SourceCodeFacade();
        if (sourceCodeRetention != SourceCodeRetention.NEVER) {
            var paintedFiles = files.stream()
                    .map(PaintedNode::new)
                    .collect(Collectors.toList());
            log.logInfo("Painting %d source files on agent", paintedFiles.size());

            paintFilesOnAgent(paintedFiles, sourceCodeEncoding, log);
            log.logInfo("Copying painted sources from agent to build folder");

            sourceCodeFacade.copySourcesToBuildFolder(build, workspace, log);
        }
        sourceCodeRetention.cleanup(build, sourceCodeFacade.getCoverageSourcesDirectory(), log);
    }

    private void paintFilesOnAgent(final List<PaintedNode> paintedFiles,
            final String sourceCodeEncoding, final FilteredLog log) throws InterruptedException {
        try {
            var painter = new AgentCoveragePainter(paintedFiles, sourceCodeEncoding, id);
            FilteredLog agentLog = workspace.act(painter);
            log.merge(agentLog);
        }
        catch (IOException exception) {
            log.logException(exception, "Can't paint and zip sources on the agent");
        }
    }

    /**
     * Paints source code files on the agent using the recorded coverage information. All files are stored as zipped
     * HTML files that contain the painted source code. In the last step all zipped source files are aggregated into a
     * single archive to simplify copying to the controller.
     */
    static class AgentCoveragePainter extends MasterToSlaveFileCallable<FilteredLog> {
        private static final long serialVersionUID = 3966282357309568323L;

        private final List<PaintedNode> paintedFiles;
        private final String sourceCodeEncoding;
        private final String directory;

        /**
         * Creates a new instance of {@link AgentCoveragePainter}.
         *
         * @param paintedFiles
         *         the model for the file painting for each coverage node
         * @param sourceCodeEncoding
         *         the encoding of the source code files
         * @param directory
         *         the subdirectory where the source files will be stored in
         */
        AgentCoveragePainter(final List<PaintedNode> paintedFiles, final String sourceCodeEncoding,
                final String directory) {
            super();

            this.paintedFiles = paintedFiles;
            this.sourceCodeEncoding = sourceCodeEncoding;
            this.directory = directory;
        }

        @Override
        public FilteredLog invoke(final File workspaceFile, final VirtualChannel channel) {
            FilteredLog log = new FilteredLog("Errors during source code painting:");
            FilePath workspace = new FilePath(workspaceFile);

            try {
                FilePath outputFolder = workspace.child(directory);
                outputFolder.mkdirs();

                Path temporaryFolder = Files.createTempDirectory(directory);

                int count = paintedFiles.parallelStream()
                        .mapToInt(file -> paintSource(file, workspace, temporaryFolder, log))
                        .sum();

                if (count == paintedFiles.size()) {
                    log.logInfo("-> finished painting successfully");
                }
                else {
                    log.logInfo("-> finished painting (%d files have been painted, %d files failed)",
                            count, paintedFiles.size() - count);
                }

                FilePath zipFile = workspace.child(SourceCodeFacade.COVERAGE_SOURCES_ZIP);
                outputFolder.zip(zipFile);
                log.logInfo("-> zipping sources from folder '%s' as '%s'", outputFolder, zipFile);

                deleteFolder(temporaryFolder.toFile(), log);
            }
            catch (IOException exception) {
                log.logException(exception,
                        "Cannot create temporary directory in folder '%s' for the painted source files", workspace);
            }
            catch (InterruptedException exception) {
                log.logException(exception,
                        "Processing has been interrupted: skipping zipping of source files", workspace);
            }

            return log;
        }

        private Charset getCharset() {
            return new ValidationUtilities().getCharset(sourceCodeEncoding);
        }

        private int paintSource(final PaintedNode fileNode, final FilePath workspace,
                final Path temporaryFolder, final FilteredLog log) {
            String relativePathIdentifier = fileNode.getPath();
            FilePath paintedFilesDirectory = workspace.child(directory);
            return findSourceFile(workspace, relativePathIdentifier, log)
                    .map(resolvedPath -> paint(fileNode, relativePathIdentifier, resolvedPath,
                            paintedFilesDirectory, temporaryFolder, getCharset(), log))
                    .orElse(0);
        }

        private int paint(final PaintedNode paint, final String relativePathIdentifier,
                final FilePath resolvedPath, final FilePath paintedFilesDirectory,
                final Path temporaryFolder, final Charset charset, final FilteredLog log) {
            String sanitizedFileName = SourceCodeFacade.sanitizeFilename(relativePathIdentifier);
            FilePath zipOutputPath = paintedFilesDirectory.child(
                    sanitizedFileName + SourceCodeFacade.ZIP_FILE_EXTENSION);
            try {
                Path paintedFilesFolder = Files.createTempDirectory(temporaryFolder, directory);
                Path fullSourcePath = paintedFilesFolder.resolve(sanitizedFileName);
                try (BufferedWriter output = Files.newBufferedWriter(fullSourcePath)) {
                    List<String> lines = Files.readAllLines(Paths.get(resolvedPath.getRemote()), charset);
                    new SourceToHtml().paintSourceCodeWithCoverageInformation(paint, output, lines);
                }
                new FilePath(fullSourcePath.toFile()).zip(zipOutputPath);
                FileUtils.deleteDirectory(paintedFilesFolder.toFile());
                return 1;
            }
            catch (IOException | InterruptedException exception) {
                log.logException(exception, "Can't write coverage paint of '%s' to zipped source file '%s'",
                        relativePathIdentifier, zipOutputPath);
                return 0;
            }
        }

        private Optional<FilePath> findSourceFile(final FilePath workspace, final String fileName,
                final FilteredLog log) {
            try {
                FilePath absolutePath = new FilePath(new File(fileName));
                if (absolutePath.exists()) {
                    return Optional.of(absolutePath);
                }

                FilePath relativePath = workspace.child(fileName);
                if (relativePath.exists()) {
                    return Optional.of(relativePath);
                }

                log.logError("Source file '%s' not found", fileName);
            }
            catch (InvalidPathException | IOException | InterruptedException exception) {
                log.logException(exception, "No valid path in coverage node: '%s'", fileName);
            }
            return Optional.empty();
        }

        /**
         * Deletes a folder.
         *
         * @param folder
         *         The directory to be deleted
         * @param log
         *         The log
         */
        private void deleteFolder(final File folder, final FilteredLog log) {
            if (folder.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(folder);
                }
                catch (IOException e) {
                    log.logError("The folder '%s' could not be deleted",
                            folder.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Provides all required information for a {@link FileNode} so that its source code can be rendered in HTML.
     */
    static class PaintedNode implements Serializable {
        private static final long serialVersionUID = -6044649044983631852L;

        private enum Type {
            MUTATION,
            COVERAGE
        }

        private final String path;
        private final int[] linesToPaint;
        private final int[] coveredPerLine;
        private final int[] missedPerLine;
        private final int[] survivedPerLine;
        private final int[] killedPerLine;

        PaintedNode(final FileNode file) {
            path = file.getRelativePath();

            linesToPaint = file.getLinesWithCoverage().stream().mapToInt(i -> i).toArray();
            coveredPerLine = file.getCoveredCounters();
            missedPerLine = file.getMissedCounters();

            survivedPerLine = new int[linesToPaint.length];
            killedPerLine = new int[linesToPaint.length];

            for (Mutation mutation : file.getMutations()) {
                if (mutation.hasSurvived()) {
                    survivedPerLine[findLine(mutation.getLine())]++;
                }
                else if (mutation.isKilled()) {
                    killedPerLine[findLine(mutation.getLine())]++;
                }
            }
        }

        public String getPath() {
            return path;
        }

        public boolean isPainted(final int line) {
            return findLine(line) >= 0;
        }

        private int findLine(final int line) {
            return Arrays.binarySearch(linesToPaint, line);
        }

        public int getCovered(final int line) {
            return getCounter(line, coveredPerLine);
        }

        public int getMissed(final int line) {
            return getCounter(line, missedPerLine);
        }

        public int getSurvived(final int line) {
            return getCounter(line, survivedPerLine);
        }

        public int getKilled(final int line) {
            return getCounter(line, killedPerLine);
        }

        private int getCounter(final int line, final int... counters) {
            var index = findLine(line);
            if (index >= 0) {
                return counters[index];
            }
            return 0;
        }
    }
}
