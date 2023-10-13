package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

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

    /**
     * Finds all files with modified lines of code in a passed {@link Node} object.
     *
     * @param node
     *         containing the file tree.
     *
     * @return a list of {@link FileWithModifiedLines} objects.
     */
    private List<FileWithModifiedLines> createListOfFilesWithModifiedLines(final Node node) {
        var filesWithModifiedLines = new ArrayList<FileWithModifiedLines>();

        for (FileNode fileNode : node.filterByModifiedLines().getAllFileNodes()) {
            var missedLines = new ArrayList<>(fileNode.getMissedLines());
            var partiallyCoveredLines = new ArrayList<>(fileNode.getPartiallyCoveredLines().keySet());
            var coveredLines = new ArrayList<Integer>();

            for (Integer line : fileNode.getLinesWithCoverage()) {
                if (fileNode.getMissedOfLine(line) == 0 && fileNode.getCoveredOfLine(line) > 0) {
                    coveredLines.add(line);
                }
            }

            var modifiedLinesBlocks = new TreeSet<ModifiedLinesBlock>();

            modifiedLinesBlocks.addAll(calculateModifiedLineBlocks(coveredLines, LineCoverageType.COVERED));
            modifiedLinesBlocks.addAll(calculateModifiedLineBlocks(missedLines, LineCoverageType.MISSED));
            modifiedLinesBlocks.addAll(calculateModifiedLineBlocks(partiallyCoveredLines,
                    LineCoverageType.PARTIALLY_COVERED));

            var changedFile = new FileWithModifiedLines(fileNode.getRelativePath(), modifiedLinesBlocks);
            filesWithModifiedLines.add(changedFile);
        }

        return filesWithModifiedLines;
    }

    /**
     * This method parses over the modified lines of a file and combines consecutive line numbers with the same coverage
     * type into a {@link ModifiedLinesBlock} object. Result is sorted by starting line number.
     *
     * @param modifiedLines
     *         list containing the integer numbers of modified lines.
     * @param type
     *         type of coverage pertaining to each line of code ({@link LineCoverageType#COVERED},
     *         {@link LineCoverageType#MISSED}, or {@link LineCoverageType#PARTIALLY_COVERED})
     */
    private List<ModifiedLinesBlock> calculateModifiedLineBlocks(final List<Integer> modifiedLines,
            final LineCoverageType type) {
        var modifiedLinesBlocks = new ArrayList<ModifiedLinesBlock>();
        if (modifiedLines.isEmpty()) {
            return modifiedLinesBlocks;
        }

        int start = modifiedLines.get(0);
        int last = start;
        if (modifiedLines.size() > 1) {
            for (int line : modifiedLines.subList(1, modifiedLines.size())) {
                if (line > last + 1) {
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

    @Exported(inline = true, name = "files")
    public List<FileWithModifiedLines> getFilesWithModifiedLines() {
        return filesWithModifiedLines;
    }
}
