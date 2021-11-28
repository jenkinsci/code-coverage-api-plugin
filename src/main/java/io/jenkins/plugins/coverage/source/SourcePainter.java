package io.jenkins.plugins.coverage.source;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.targets.CoveragePaint;

/**
 * Paints a collection of files with the recorded coverage information and stores all files in a temporary directory.
 *
 * @author Ullrich Hafner
 */
public class SourcePainter implements Serializable {
    /*
     TODO:  ideally we would scan for coverage reports on the agent and covert the reports there as well. Then we
            can simply create the painted files on the agent without transferring the list of nodes that will be painted.
            Additionally we could strip the node tree at the file level, since the lower nodes are only required for
            the painting.
     */
    private static final long serialVersionUID = -7390586016893061868L;
    public static final String COVERAGE_SOURCES_ZIP = "coverage-sources.zip";

    public void paintSources(final List<PaintedNode> paintedFiles, final FilteredLog log, final FilePath workspace,
            final Collection<String> sourceFolders, final Charset sourceEncoding) {
        try {
            FilePath outputFolder = getSourcesFolder(workspace);
            outputFolder.mkdirs();

            int count = paintedFiles.parallelStream().mapToInt(
                    f -> paintFile(f, workspace, sourceFolders, log, sourceEncoding)).sum();
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
        }
        catch (IOException exception) {
            log.logException(exception,
                    "Cannot create temporary directory in folder '%s' for the painted source files", workspace);
        }
        catch (InterruptedException exception) {
            log.logException(exception,
                    "Processing has been interrupted: skipping zipping of source files", workspace);
        }
    }

    private int paintFile(final PaintedNode fileNode, final FilePath workspace,
            final Collection<String> sourceFolders, final FilteredLog log, final Charset sourceEncoding) {
        String sourcePath = fileNode.getNode().getPath();
        return findSourceFile(workspace, sourcePath, sourceFolders, log)
                .map(path -> paint(fileNode.getPaint(), sourcePath, path, sourceEncoding, workspace, log))
                .orElse(0);
    }

    private int paint(final CoveragePaint paint, final String fileName, final FilePath inputPath, final Charset charset,
            final FilePath workspace, final FilteredLog log) {
        FilePath outputPath = getSourcesFolder(workspace).child(getTempName(fileName));
        try {
            try (BufferedWriter output = Files.newBufferedWriter(Paths.get(outputPath.getRemote()))) {
                List<String> lines = Files.readAllLines(Paths.get(inputPath.getRemote()), charset);
                for (int line = 0; line < lines.size(); line++) {
                    String content = lines.get(line);
                    paintLine(line, content, paint, output);
                }
                paint.setTotalLines(lines.size());
            }
            return 1;
        }
        catch (IOException exception) {
            log.logException(exception, "Can't write coverage paint of '%s' to source file '%s'", fileName, outputPath);
            return 0;
        }
    }

    private FilePath getSourcesFolder(final FilePath workspace) {
        return workspace.child("coverage-sources");
    }

    private void paintLine(final int line, final String content, final CoveragePaint paint,
            final BufferedWriter output) throws IOException {
        if (paint.isPainted(line)) {
            final int hits = paint.getHits(line);
            final int branchCoverage = paint.getBranchCoverage(line);
            final int branchTotal = paint.getBranchTotal(line);
            final int coveragePercent = (hits == 0) ? 0 : (int) (branchCoverage * 100.0 / branchTotal);
            if (paint.getHits(line) > 0) {
                if (branchTotal == branchCoverage) {
                    output.write("<tr class=\"coverFull\">\n");
                }
                else {
                    output.write("<tr class=\"coverPart\" title=\"Line " + line + ": Conditional coverage "
                            + coveragePercent + "% ("
                            + branchCoverage + "/" + branchTotal + ")\">\n");
                }
            }
            else {
                output.write("<tr class=\"coverNone\">\n");
            }
            output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");
            output.write("<td class=\"hits\">" + hits + "</td>\n");
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

    /**
     * Returns a file name for a temporary file that will hold the contents of the source.
     *
     * @param fileName
     *         the file name to convert
     *
     * @return the temporary name
     */
    private static String getTempName(final String fileName) {
        return Integer.toHexString(fileName.hashCode()) + ".tmp";
    }

    private Optional<FilePath> findSourceFile(final FilePath workspace, final String fileName,
            final Collection<String> sourceFolders, final FilteredLog log) {
        try {
            FilePath absolutePath = new FilePath(new File(fileName));
            if (absolutePath.exists()) {
                return Optional.of(absolutePath);
            }

            FilePath relativePath = workspace.child(fileName);
            if (relativePath.exists()) {
                return Optional.of(relativePath);
            }

            for (String sourceFolder : sourceFolders) {
                FilePath sourcePath = workspace.child(sourceFolder).child(fileName);
                if (sourcePath.exists()) {
                    return Optional.of(sourcePath);
                }
            }

            log.logError("Source file '%s' not found", fileName);
        }
        catch (InvalidPathException | IOException | InterruptedException exception) {
            log.logException(exception, "No valid path in coverage node: '%s'", fileName);
        }
        return Optional.empty();
    }

    public static class AgentPainter extends MasterToSlaveFileCallable<FilteredLog> {
        private static final long serialVersionUID = 3966282357309568323L;

        private final List<PaintedNode> paintedFiles;

        public AgentPainter(final Set<Entry<CoverageNode, CoveragePaint>> paintedFiles) {
            this.paintedFiles = paintedFiles.stream()
                    .map(e -> new PaintedNode(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }

        @Override
        public FilteredLog invoke(final File workspace, final VirtualChannel channel)
                throws IOException, InterruptedException {
            FilteredLog log = new FilteredLog("Errors during source code painting:");

            SourcePainter sourcePainter = new SourcePainter();
            sourcePainter.paintSources(paintedFiles, log, new FilePath(workspace), Arrays.asList("checkout/src/main/java"), StandardCharsets.UTF_8);

            return log;
        }
    }

    static class PaintedNode implements Serializable {
        private static final long serialVersionUID = -6044649044983631852L;

        private final CoverageNode node;
        private final CoveragePaint paint;

        PaintedNode(final CoverageNode node, final CoveragePaint paint) {
            this.node = node;
            this.paint = paint;
        }

        public CoverageNode getNode() {
            return node;
        }

        public CoveragePaint getPaint() {
            return paint;
        }
    }
}
