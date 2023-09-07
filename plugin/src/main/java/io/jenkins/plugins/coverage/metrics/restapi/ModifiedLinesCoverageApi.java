package io.jenkins.plugins.coverage.metrics.restapi;

import java.util.ArrayList;
import java.util.List;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Remote API to list the details of modified line coverage results.
 */
@ExportedBean
public class ModifiedLinesCoverageApi {
    private final List<FileWithModifiedLines> filesWithModifiedLines;

    /**
     * Creates a new instance of {@link ModifiedLinesCoverageApi}.
     *
     * @param node
     *         File node from which modified lines of code are calculated.
     */
    public ModifiedLinesCoverageApi(final Node node) {
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
    private static List<FileWithModifiedLines> createListOfFilesWithModifiedLines(final Node node) {
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

            var modifiedLinesBlocks = new ArrayList<ModifiedLinesBlock>();

            calculateModifiedLineBlocks(coveredLines, modifiedLinesBlocks, LineCoverageType.COVERED);
            calculateModifiedLineBlocks(missedLines, modifiedLinesBlocks, LineCoverageType.MISSED);
            calculateModifiedLineBlocks(partiallyCoveredLines, modifiedLinesBlocks, LineCoverageType.PARTIALLY_COVERED);

            FileWithModifiedLines changedFile = new FileWithModifiedLines(fileNode.getRelativePath(),
                    modifiedLinesBlocks);
            filesWithModifiedLines.add(changedFile);
        }
        return filesWithModifiedLines;
    }

    /**
     * This method parses over the modified lines of a file and combines consecutive line numbers with the same coverage
     * type into a {@link ModifiedLinesBlock} object.
     *
     * @param modifiedLines
     *         list containing the integer numbers of modified lines.
     * @param modifiedLinesBlocks
     *         list containing modifiedLinesBlock objects.
     * @param type
     *         type of coverage pertaining to each line of code ({@link LineCoverageType#COVERED},
     *         {@link LineCoverageType#MISSED}, or {@link LineCoverageType#PARTIALLY_COVERED})
     */
    private static void calculateModifiedLineBlocks(final List<Integer> modifiedLines,
            final List<ModifiedLinesBlock> modifiedLinesBlocks, final LineCoverageType type) {

        if (modifiedLines.isEmpty()) {
            return;
        }

        int currentLine = modifiedLines.get(0);
        for (int i = 0; i < modifiedLines.size(); i++) {
            if (i == modifiedLines.size() - 1 || !modifiedLines.get(i).equals(modifiedLines.get(i + 1) - 1)) {

                var modifiedLinesBlock = new ModifiedLinesBlock(currentLine, modifiedLines.get(i), type);
                modifiedLinesBlocks.add(modifiedLinesBlock);

                if (i < modifiedLines.size() - 1) {
                    currentLine = modifiedLines.get(i + 1);
                }
            }
        }
    }

    @Exported(inline = true)
    public List<FileWithModifiedLines> createListOfFilesWithModifiedLines() {
        return filesWithModifiedLines;
    }
}
