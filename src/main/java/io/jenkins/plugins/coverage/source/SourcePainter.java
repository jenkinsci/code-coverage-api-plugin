package io.jenkins.plugins.coverage.source;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;

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

    public void paintSources(final List<PaintedNode> paintedFiles, final FilteredLog log, final File workspace,
            final Collection<String> sourceFolders, final Charset sourceEncoding) {
        try {
            Path outputFolder = workspace.toPath().resolve("coverage-sources");
            Files.createDirectory(outputFolder);

            log.logInfo("Painting %d files in folder '%s'", paintedFiles.size(), outputFolder);
            paintedFiles.parallelStream().forEach(
                    f -> paintFile(f, workspace.toPath(), sourceFolders, log, sourceEncoding));
            log.logInfo("-> finished painting successfully");
        }
        catch (IOException exception) {
            log.logException(exception,
                    "Cannot create temporary directory in folder '%s' for the painted source files", workspace);
        }
    }

    private void paintFile(final PaintedNode fileNode, final Path workspace,
            final Collection<String> sourceFolders, final FilteredLog log, final Charset sourceEncoding) {
        String sourcePath = fileNode.getNode().getPath();
        Optional<Path> sourceFile = findSourceFile(workspace, sourcePath, sourceFolders, log);
        sourceFile.ifPresent(path -> paint(fileNode.getPaint(), sourcePath, path, sourceEncoding, workspace, log));
    }

    private void paint(final CoveragePaint paint, final String fileName, final Path inputPath, final Charset charset,
            final Path workspace, final FilteredLog log) {
        Path outputPath = workspace.resolve(getTempName(fileName));
        try {
            log.logInfo("Writing painted source of '%s' to '%s'", fileName, outputPath);
            try (BufferedWriter output = Files.newBufferedWriter(outputPath)) {
                List<String> lines = Files.readAllLines(inputPath, charset);
                for (int line = 0; line < lines.size(); line++) {
                    String content = lines.get(line);
                    paintLine(line, content, paint, output);
                }
                paint.setTotalLines(lines.size());
            }
        }
        catch (IOException exception) {
            log.logException(exception, "Can't write coverage paint of '%s' to source file '%s'", fileName, outputPath);
        }
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

    private Optional<Path> findSourceFile(final Path workspace, final String nodePath,
            final Collection<String> sourceFolders, final FilteredLog log) {
        try {
            String relativeSourcePath = nodePath;
            Path absolutePath = Paths.get(relativeSourcePath);
            if (Files.exists(absolutePath)) {
                return Optional.of(absolutePath);
            }
            Path relativePath = workspace.resolve(relativeSourcePath);
            if (Files.exists(relativePath)) {
                return Optional.of(relativePath);
            }

            for (String sourceFolder : sourceFolders) {
                Path sourcePath = workspace.resolve(sourceFolder).resolve(relativeSourcePath);
                if (Files.exists(sourcePath)) {
                    return Optional.of(sourcePath);
                }
            }
            log.logError("Source file '%s' not found", nodePath);
        }
        catch (InvalidPathException exception) {
            log.logException(exception, "No valid path in coverage node: '%s'", nodePath);
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
            sourcePainter.paintSources(paintedFiles, log, workspace, Arrays.asList("checkout/src/main/java"), StandardCharsets.UTF_8);

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
