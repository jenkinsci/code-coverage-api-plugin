package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;
import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;

/**
 * Server side model that provides data for the details of line coverage results of modified lines.
 */
public class CoverageApiUtil {

    private CoverageApiUtil() {
        //this is a utility class
    }

    /**
     * Method to extract modified lines of code and their respective coverage status from passed {@link Node} object.
     *
     * @param node
     *         the root of the tree from which modified lines are extracted.
     *
     * @return returns a List of {@link FileWithModifiedLines} objects.
     */
    public static List<FileWithModifiedLines> getFilesWithModifiedLines(final Node node) {
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

}
