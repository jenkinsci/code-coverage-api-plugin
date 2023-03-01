package io.jenkins.plugins.coverage.metrics.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import hudson.util.TextFile;

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

    static String sanitizeFilename(final String inputName) {
        return StringUtils.right(inputName.replaceAll("[^a-zA-Z0-9-_.]", "_"), MAX_FILENAME_LENGTH);
    }

    /**
     * Reads the contents of the source file of the given file into a String.
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
            FilePath inputZipFile = new FilePath(createFileInBuildFolder(buildResults, id, path));
            inputZipFile.unzip(unzippedSourcesDir);
            String actualPaintedSourceFileName = StringUtils.removeEnd(sanitizeFilename(path), ZIP_FILE_EXTENSION);
            File sourceFile = tempDir.resolve(actualPaintedSourceFileName).toFile();

            return new TextFile(sourceFile).read();
        }
        finally {
            unzippedSourcesDir.deleteRecursive();
        }
    }

    /**
     * Returns whether the source code is available for the specified source file.
     *
     * @param buildResults
     *         Jenkins directory for build results
     * @param id
     *         if of the coverage results
     * @param path
     *         relative path to the source code filename name
     *
     * @return the file content as String
     */
    public boolean canRead(final File buildResults, final String id, final String path) {
        return createFileInBuildFolder(buildResults, id, path).canRead();
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
     * Returns a file to a source file in Jenkins' build folder. Note that the file might not exist.
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
    File createFileInBuildFolder(final File buildResults, final String id, final String path) {
        File sourceFolder = new File(buildResults, COVERAGE_SOURCES_DIRECTORY);
        File elementFolder = new File(sourceFolder, id);

        return new File(elementFolder, sanitizeFilename(path) + ZIP_FILE_EXTENSION);
    }

    /**
     * Filters the sourcecode coverage highlighting for analyzing the modified lines coverage only.
     *
     * @param content
     *         The original HTML content
     * @param fileNode
     *         The {@link FileNode node} which represents the coverage of the file
     *
     * @return the filtered HTML sourcecode view
     */
    public String calculateModifiedLinesCoverageSourceCode(final String content, final FileNode fileNode) {
        Set<Integer> lines = fileNode.getLinesWithCoverage();
        lines.retainAll(fileNode.getChangedLines());
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
}
