package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Remote API to list the details of modified line coverage results.
 */
@ExportedBean
class ModifiedLinesCoverageApi {
    private final List<FileWithModifiedLines> filesWithModifiedLines;

    ModifiedLinesCoverageApi(final Node node) {
        this.filesWithModifiedLines = createListOfFilesWithModifiedLines(node);
    }

    @Exported(inline = true, name = "files")
    public List<FileWithModifiedLines> getFilesWithModifiedLines() {
        return filesWithModifiedLines;
    }

    /**
     * Finds all files with modified lines of code in a passed {@link Node} object.
     *
     * @param node
     *         containing the file tree.
     *
     * @return a list of {@link FileWithModifiedLines} objects.
     */
    private List<FileWithModifiedLines> createListOfFilesWithModifiedLines(final Node node) {
        var result = new ArrayList<FileWithModifiedLines>();

        for (FileNode fileNode : node.filterByModifiedLines().getAllFileNodes()) {
            var modifiedLines = new ArrayList<>(fileNode.getModifiedLines());
            var modifiedLinesWithoutCoverage = modifiedLines.stream()
                    .filter(line -> !fileNode.getLinesWithCoverage().contains(line))
                    .collect(Collectors.toList());

            var missedLines = filterByModifiedLines(modifiedLines, fileNode.getMissedLines());
            var partiallyCoveredLines =
                    filterByModifiedLines(modifiedLines, fileNode.getPartiallyCoveredLines().keySet());
            var coveredLines = fileNode.getLinesWithCoverage().stream()
                    .filter(line -> fileNode.getMissedOfLine(line) == 0)
                    .filter(modifiedLines::contains)
                    .collect(Collectors.toList());

            var modifiedLinesBlocks = new TreeSet<ModifiedLinesBlock>();

            modifiedLinesBlocks.addAll(
                    calculateModifiedLineBlocks(coveredLines, modifiedLinesWithoutCoverage, LineCoverageType.COVERED));
            modifiedLinesBlocks.addAll(
                    calculateModifiedLineBlocks(missedLines, modifiedLinesWithoutCoverage, LineCoverageType.MISSED));
            modifiedLinesBlocks.addAll(calculateModifiedLineBlocks(partiallyCoveredLines, modifiedLinesWithoutCoverage,
                    LineCoverageType.PARTIALLY_COVERED));

            var changedFile = new FileWithModifiedLines(fileNode.getRelativePath(), modifiedLinesBlocks);
            result.add(changedFile);
        }

        return result;
    }

    /**
     * Filters the given lines to only contain the given modified lines.
     *
     * @param modifiedLines
     *         the lines that have been modified
     * @param lines
     *         the lines that should be filtered for modified lines only
     *
     * @return the filtered lines
     */
    private List<Integer> filterByModifiedLines(final Collection<Integer> modifiedLines,
            final Collection<Integer> lines) {
        return lines.stream().filter(modifiedLines::contains).collect(Collectors.toList());
    }

    /**
     * This method parses over the modified lines of a file and combines consecutive line numbers with the same coverage
     * type into a {@link ModifiedLinesBlock} object. If there are empty lines without coverage information between
     * lines with the same type, they will be included in the block. The result is sorted by the starting line number.
     *
     * @param modifiedLines
     *         list containing the integer numbers of modified lines.
     * @param modifiedLinesWithoutCoverage
     *         lines that have been modified but have no coverage information
     * @param type
     *         type of coverage pertaining to each line of code ({@link LineCoverageType#COVERED},
     *         {@link LineCoverageType#MISSED}, or {@link LineCoverageType#PARTIALLY_COVERED})
     *
     * @return the list of {@link ModifiedLinesBlock}
     */
    private List<ModifiedLinesBlock> calculateModifiedLineBlocks(final List<Integer> modifiedLines,
            final List<Integer> modifiedLinesWithoutCoverage, final LineCoverageType type) {
        var modifiedLinesBlocks = new ArrayList<ModifiedLinesBlock>();
        if (modifiedLines.isEmpty()) {
            return modifiedLinesBlocks;
        }

        int start = modifiedLines.get(0);
        int last = start;
        if (modifiedLines.size() > 1) {
            for (int line : modifiedLines.subList(1, modifiedLines.size())) {
                if (line > last + 1
                        && hasAnyLinesWithCoverageBetween(last, line, modifiedLinesWithoutCoverage)) {
                    var modifiedLinesBlock = new ModifiedLinesBlock(start, last, type);
                    modifiedLinesBlocks.add(modifiedLinesBlock);
                    start = line;
                }
                last = line;
            }
        }
        var modifiedLinesBlock = new ModifiedLinesBlock(start, last, type);
        modifiedLinesBlocks.add(modifiedLinesBlock);

        return modifiedLinesBlocks;
    }

    /**
     * Checks whether the range between the first and the second given line contains any lines with coverage
     * information.
     *
     * @param start
     *         the first line number
     * @param end
     *         the second line number (must be greater than the first)
     * @param modifiedLinesWithoutCoverage
     *         the modified lines without coverage information
     *
     * @return {@code true} whether there are any lines within the given line range that contains coverage information,
     *         else {@code false}
     */
    private boolean hasAnyLinesWithCoverageBetween(final int start, final int end,
            final List<Integer> modifiedLinesWithoutCoverage) {
        return IntStream.range(start + 1, end).anyMatch(line -> !modifiedLinesWithoutCoverage.contains(line));
    }
}
