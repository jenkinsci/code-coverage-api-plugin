package io.jenkins.plugins.coverage.metrics.visualization.code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.util.FilteredLog;

import hudson.FilePath;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.util.TextFile;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.coverage.metrics.CoverageFormatter;
import io.jenkins.plugins.prism.CharsetValidation;
import io.jenkins.plugins.prism.FilePermissionEnforcer;
import io.jenkins.plugins.prism.SourceDirectoryFilter;

/**
 * Facade to the source code file structure in Jenkins build folder. Access of those files should be done using an
 * instance of this class only.
 *
 * @author Ullrich Hafner
 * @author Florian Orendi
 */
public class SourceCodeFacade {
    /** Toplevel directory in the build folder of the controller that contains the zipped source files. */
    static final String COVERAGE_SOURCES_DIRECTORY = "coverage-sources";
    static final String COVERAGE_SOURCES_ZIP = "coverage-sources.zip";
    static final int MAX_FILENAME_LENGTH = 245; // Windows has limitations on long file names
    static final String ZIP_FILE_EXTENSION = ".zip";

    String getCoverageSourcesDirectory() {
        return COVERAGE_SOURCES_DIRECTORY;
    }

    /**
     * Copies the zipped source files from the agent to the controller and unpacks them in the coverage-sources folder
     * of the current build.
     *
     * @param build
     *         the build with the coverage result
     * @param workspace
     *         the workspace on the agent that created the ZIP file
     * @param log
     *         the log
     *
     * @throws InterruptedException
     *         in case the user terminated the job
     */
    void copySourcesToBuildFolder(final Run<?, ?> build, final FilePath workspace, final FilteredLog log)
            throws InterruptedException {
        try {
            FilePath buildFolder = new FilePath(build.getRootDir()).child(COVERAGE_SOURCES_DIRECTORY);
            FilePath buildZip = buildFolder.child(COVERAGE_SOURCES_ZIP);
            workspace.child(COVERAGE_SOURCES_ZIP).copyTo(buildZip);
            log.logInfo("-> extracting...");
            buildZip.unzip(buildFolder);
            buildZip.delete();
            log.logInfo("-> done");
        }
        catch (IOException exception) {
            log.logException(exception, "Can't copy zipped sources from agent to controller");
        }
    }

    /**
     * Reads the contents of the source file of the given coverage node into a String.
     *
     * @param buildResults
     *         Jenkins directory for build results
     * @param id
     *         if of the coverage results
     * @param path
     *         relative path to the coverage node base filename of the coverage node
     *
     * @return the file content as String
     */
    public String read(final File buildResults, final String id, final String path)
            throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory(COVERAGE_SOURCES_DIRECTORY);
        FilePath unzippedSourcesDir = new FilePath(tempDir.toFile());
        try {
            FilePath inputZipFile = new FilePath(
                    new SourceCodeFacade().createFileInBuildFolder(buildResults, id, path));
            inputZipFile.unzip(unzippedSourcesDir);
            String actualPaintedSourceFileName = StringUtils.removeEnd(AgentCoveragePainter.sanitizeFilename(path),
                    ZIP_FILE_EXTENSION);
            File sourceFile = tempDir.resolve(actualPaintedSourceFileName).toFile();
            return new TextFile(sourceFile).read();
        }
        finally {
            unzippedSourcesDir.deleteRecursive();
        }
    }

    /**
     * Returns a file to the sources in release 2.1.0 and newer. Note that the file might not exist.
     *
     * @param buildResults
     *         Jenkins directory for build results
     * @param id
     *         if of the coverage results
     * @param path
     *         relative path to the coverage node base filename of the coverage node
     *
     * @return the file
     */
    public File createFileInBuildFolder(final File buildResults, final String id, final String path) {
        File sourceFolder = new File(buildResults, COVERAGE_SOURCES_DIRECTORY);
        File elementFolder = new File(sourceFolder, id);

        return new File(elementFolder, AgentCoveragePainter.sanitizeFilename(path) + ZIP_FILE_EXTENSION);
    }

    /**
     * Checks whether any source files has been stored. Even if it is wanted, there might have been errors which cause
     * the absence of any source files.
     *
     * @param buildResults
     *         Jenkins directory for build results
     * @param id
     *         id of the coverage results
     *
     * @return {@code true} whether source files has been stored, else {@code false}
     */
    public boolean hasStoredSourceCode(final File buildResults, final String id) {
        File sourceFolder = new File(buildResults, COVERAGE_SOURCES_DIRECTORY);
        File elementFolder = new File(sourceFolder, id);
        File[] files = elementFolder.listFiles();
        return files != null && files.length > 0;
    }

    /**
     * Filters the sourcecode coverage highlighting for analyzing the change coverage only.
     *
     * @param content
     *         The original HTML content
     * @param fileNode
     *         The {@link FileNode node} which represents the coverage of the file
     *
     * @return the filtered HTML sourcecode view
     */
    public String calculateChangeCoverageSourceCode(final String content, final FileNode fileNode) {
        Set<Integer> lines = fileNode.getCoveredLines();
        Set<String> linesAsText = lines.stream().map(String::valueOf).collect(Collectors.toSet());
        Document doc = Jsoup.parse(content, Parser.xmlParser());
        int maxLine = Integer.parseInt(Objects.requireNonNull(
                doc.select("tr").last()).select("a").text());
        Map<String, Boolean> linesMapping = calculateLineMapping(lines, maxLine);
        Elements elements = doc.select("tr");
        for (Element element : elements) {
            String line = element.select("td > a").text();
            if (linesMapping.containsKey(line)) {
                if (linesMapping.get(line)) {
                    changeCodeToSkipLine(element);
                }
                else if (!linesAsText.contains(line)) {
                    element.removeClass(element.className());
                    element.addClass("noCover");
                    Objects.requireNonNull(element.select("td.hits").first()).text("");
                }
            }
            else {
                element.remove();
            }
        }
        return doc.html();
    }

    /**
     * Filters the sourcecode coverage highlighting for analyzing indirect coverage changes only.
     *
     * @param content
     *         The original HTML content
     * @param fileNode
     *         The {@link FileNode node} which represents the coverage of the file
     *
     * @return the filtered HTML sourcecode view
     */
    public String calculateIndirectCoverageChangesSourceCode(final String content, final FileNode fileNode) {
        Map<Integer, Integer> lines = fileNode.getIndirectCoverageChanges();
        Map<String, String> indirectCoverageChangesAsText = lines.entrySet().stream()
                .collect(Collectors
                        .toMap(entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue())));
        Document doc = Jsoup.parse(content, Parser.xmlParser());
        int maxLine = Integer.parseInt(Objects.requireNonNull(
                doc.select("tr").last()).select("a").text());
        Map<String, Boolean> linesMapping = calculateLineMapping(lines.keySet(), maxLine);
        doc.select("tr").forEach(element -> {
            String line = element.select("td > a").text();
            if (linesMapping.containsKey(line)) {
                colorIndirectCoverageChangeLine(element, line, linesMapping, indirectCoverageChangesAsText);
            }
            else {
                element.remove();
            }
        });
        return doc.html();
    }

    /**
     * Highlights a line to be a skip line which represents a bunch of not visible lines.
     *
     * @param element
     *         The HTML element which represents the line
     */
    private void changeCodeToSkipLine(final Element element) {
        element.removeClass(element.className());
        element.addClass("coverSkip");
        Objects.requireNonNull(element.select("td.line").first()).text("..");
        Objects.requireNonNull(element.select("td.hits").first()).text("");
        Objects.requireNonNull(element.select("td.code").first()).text("");
    }

    /**
     * Colors one line within the indirect coverage changes code view.
     *
     * @param element
     *         The HTML element which represents the line
     * @param line
     *         The line number
     * @param linesMapping
     *         The mapping which classifies how the line should be treated
     * @param indirectCoverageChangesAsText
     *         The indirect coverage changes mapping
     */
    private void colorIndirectCoverageChangeLine(final Element element, final String line,
            final Map<String, Boolean> linesMapping, final Map<String, String> indirectCoverageChangesAsText) {
        if (linesMapping.get(line)) {
            changeCodeToSkipLine(element);
        }
        else if (indirectCoverageChangesAsText.containsKey(line)) {
            element.removeClass(element.className());
            String hits = indirectCoverageChangesAsText.get(line);
            if (hits.startsWith("-")) {
                element.addClass("coverNone");
            }
            else {
                element.addClass("coverFull");
            }
            Objects.requireNonNull(element.select("td.hits").first()).text(hits);
        }
        else {
            element.removeClass(element.className());
            element.addClass("noCover");
            Objects.requireNonNull(element.select("td.hits").first()).text("");
        }
    }

    /**
     * Calculates a mapping of lines which should be shown. The mapping contains the passed line intervals surrounded by
     * +-3 lines each.
     *
     * @param lines
     *         The lines which build the line intervals to be shown
     * @param maxLine
     *         The maximum line number
     *
     * @return the line mapping as a map with the line number text as key and {@code true} if the line should be marked
     *         as a filling line, {@code false} if the line shows code
     */
    private Map<String, Boolean> calculateLineMapping(final Set<Integer> lines, final int maxLine) {
        SortedSet<Integer> linesWithSurroundings = new TreeSet<>(lines);
        lines.forEach(line -> {
            for (int i = 1; i <= 3; i++) {
                linesWithSurroundings.add(line + i);
                linesWithSurroundings.add(line - i);
            }
        });
        List<Integer> sortedLines = linesWithSurroundings.stream()
                .filter(line -> line >= 1 && line <= maxLine)
                .collect(Collectors.toList());
        SortedMap<String, Boolean> linesMapping = new TreeMap<>();
        for (int i = 0; i < sortedLines.size(); i++) {
            int line = sortedLines.get(i);
            linesMapping.put(String.valueOf(line), false);
            if (i < sortedLines.size() - 1 && line + 1 != sortedLines.get(i + 1)) {
                linesMapping.put(String.valueOf(line + 1), true);
            }
        }
        int highestLine = sortedLines.get(sortedLines.size() - 1);
        if (sortedLines.get(0) > 1) {
            linesMapping.put("1", true);
        }
        if (highestLine < maxLine) {
            linesMapping.put(String.valueOf(highestLine + 1), true);
        }
        return linesMapping;
    }

    /**
     * Paints source code files on the agent using the recorded coverage information. All files are stored as zipped
     * HTML files that contain the painted source code. In the last step all zipped source files are aggregated into a
     * single archive to simplify copying to the controller.
     */
    static class AgentCoveragePainter extends MasterToSlaveFileCallable<FilteredLog> {
        private static final long serialVersionUID = 3966282357309568323L;

        private static String sanitizeFilename(final String inputName) {
            return StringUtils.right(inputName.replaceAll("[^a-zA-Z0-9-_.]", "_"), MAX_FILENAME_LENGTH);
        }

        private final List<PaintedNode> paintedFiles;
        private final Set<String> permittedSourceDirectories;

        private final Set<String> requestedSourceDirectories;
        private final String sourceCodeEncoding;

        private final String directory;

        /**
         * Creates a new instance of {@link AgentCoveragePainter}.
         *
         * @param paintedFiles
         *         the model for the file painting for each coverage node
         * @param permittedSourceDirectories
         *         the permitted source code directories (in Jenkins global configuration)
         * @param requestedSourceDirectories
         *         the requested relative and absolute source directories (in the step configuration)
         * @param sourceCodeEncoding
         *         the encoding of the source code files
         * @param directory
         *         the subdirectory where the source files will be stored in
         */
        AgentCoveragePainter(final List<PaintedNode> paintedFiles,
                final Set<String> permittedSourceDirectories, final Set<String> requestedSourceDirectories,
                final String sourceCodeEncoding, final String directory) {
            super();

            this.paintedFiles = paintedFiles;
            this.permittedSourceDirectories = permittedSourceDirectories;
            this.requestedSourceDirectories = requestedSourceDirectories;
            this.sourceCodeEncoding = sourceCodeEncoding;
            this.directory = directory;
        }

        @Override
        public FilteredLog invoke(final File workspaceFile, final VirtualChannel channel) {
            FilteredLog log = new FilteredLog("Errors during source code painting:");
            Set<String> sourceDirectories = filterSourceDirectories(workspaceFile, log);
            if (sourceDirectories.isEmpty()) {
                log.logInfo("Searching for source code files in root of workspace '%s'", workspaceFile);
            }
            else if (sourceDirectories.size() == 1) {
                log.logInfo("Searching for source code files in '%s'", sourceDirectories.iterator().next());
            }
            else {
                log.logInfo("Searching for source code files in:", workspaceFile);
                sourceDirectories.forEach(dir -> log.logInfo("-> %s", dir));
            }
            FilePath workspace = new FilePath(workspaceFile);

            try {
                FilePath outputFolder = workspace.child(directory);
                outputFolder.mkdirs();
                Path temporaryFolder = Files.createTempDirectory(directory);

                Charset charset = getCharset();
                int count = paintedFiles.parallelStream()
                        .mapToInt(
                                file -> paintSource(file, workspace, temporaryFolder, sourceDirectories, charset, log))
                        .sum();

                if (count == paintedFiles.size()) {
                    log.logInfo("-> finished painting successfully");
                }
                else {
                    log.logInfo("-> finished painting (%d files have been painted, %d files failed)",
                            count, paintedFiles.size() - count);
                }

                FilePath zipFile = workspace.child(COVERAGE_SOURCES_ZIP);
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
            return new CharsetValidation().getCharset(sourceCodeEncoding);
        }

        private Set<String> filterSourceDirectories(final File workspace, final FilteredLog log) {
            SourceDirectoryFilter filter = new SourceDirectoryFilter();
            return filter.getPermittedSourceDirectories(workspace.getAbsolutePath(),
                    permittedSourceDirectories, requestedSourceDirectories, log);
        }

        private int paintSource(final PaintedNode fileNode, final FilePath workspace, final Path temporaryFolder,
                final Set<String> sourceSearchDirectories, final Charset sourceEncoding, final FilteredLog log) {
            String relativePathIdentifier = fileNode.getPath();
            FilePath paintedFilesDirectory = workspace.child(directory);
            return findSourceFile(workspace, relativePathIdentifier, sourceSearchDirectories, log)
                    .map(resolvedPath -> paint(fileNode, relativePathIdentifier, resolvedPath,
                            paintedFilesDirectory, temporaryFolder, sourceEncoding, log))
                    .orElse(0);
        }

        private int paint(final PaintedNode paint, final String relativePathIdentifier, final FilePath resolvedPath,
                final FilePath paintedFilesDirectory, final Path temporaryFolder,
                final Charset charset, final FilteredLog log) {
            String sanitizedFileName = sanitizeFilename(relativePathIdentifier);
            FilePath zipOutputPath = paintedFilesDirectory.child(sanitizedFileName + ZIP_FILE_EXTENSION);
            try {
                Path paintedFilesFolder = Files.createTempDirectory(temporaryFolder, directory);
                Path fullSourcePath = paintedFilesFolder.resolve(sanitizedFileName);
                try (BufferedWriter output = Files.newBufferedWriter(fullSourcePath)) {
                    List<String> lines = Files.readAllLines(Paths.get(resolvedPath.getRemote()), charset);
                    for (int line = 0; line < lines.size(); line++) {
                        String content = lines.get(line);
                        paintLine(line + 1, content, paint, output);
                    }
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

        private void paintLine(final int line, final String content, final PaintedNode paint,
                final BufferedWriter output) throws IOException {
            if (paint.isPainted(line)) {
                int covered = paint.getCovered(line);
                int missed = paint.getMissed(line);
                int total = missed + covered;

                if (covered == 0) {
                    output.write("<tr class=\"coverNone\">\n");
                }
                else {
                    if (missed == 0) {
                        output.write("<tr class=\"coverFull\">\n");
                    }
                    else {
                        var formatter = new CoverageFormatter();
                        output.write("<tr class=\"coverPart\" tooltip=\"Line " + line + ": branch coverage "
                                + formatter.formatPercentage(covered, total, Locale.ENGLISH) + "\">\n");
                    }

                }
                output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");
                output.write("<td class=\"hits\">" + covered + "</td>\n");
            }
            else {
                output.write("<tr class=\"noCover\">\n");
                output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");
                output.write("<td class=\"hits\"></td>\n");
            }
            output.write("<td class=\"code\">"
                    + content.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace(" ",
                            "&nbsp;")
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "</td>\n");
            output.write("</tr>\n");
        }

        private Optional<FilePath> findSourceFile(final FilePath workspace, final String fileName,
                final Set<String> sourceDirectories, final FilteredLog log) {
            try {
                FilePath absolutePath = new FilePath(new File(fileName));
                if (absolutePath.exists()) {
                    return enforcePermissionFor(absolutePath, workspace, sourceDirectories, log);
                }

                FilePath relativePath = workspace.child(fileName);
                if (relativePath.exists()) {
                    return enforcePermissionFor(relativePath, workspace, sourceDirectories, log);
                }

                for (String sourceFolder : sourceDirectories) {
                    FilePath sourcePath = workspace.child(sourceFolder).child(fileName);
                    if (sourcePath.exists()) {
                        return enforcePermissionFor(sourcePath, workspace, sourceDirectories, log);
                    }
                }

                log.logError("Source file '%s' not found", fileName);
            }
            catch (InvalidPathException | IOException | InterruptedException exception) {
                log.logException(exception, "No valid path in coverage node: '%s'", fileName);
            }
            return Optional.empty();
        }

        private Optional<FilePath> enforcePermissionFor(final FilePath absolutePath, final FilePath workspace,
                final Set<String> sourceDirectories, final FilteredLog log) {
            FilePermissionEnforcer enforcer = new FilePermissionEnforcer();
            if (enforcer.isInWorkspace(absolutePath.getRemote(), workspace, sourceDirectories)) {
                return Optional.of(absolutePath);
            }
            log.logError("Skipping coloring of file: %s (not part of workspace or permitted source code folders)",
                    absolutePath.getRemote());
            return Optional.empty();
        }

        /**
         * Deletes a folder.
         *
         * @param folder The directory to be deleted
         * @param log The log
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

}
