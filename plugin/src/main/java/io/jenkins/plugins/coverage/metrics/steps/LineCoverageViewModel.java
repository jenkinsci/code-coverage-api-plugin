package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;
import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;

//TODO: rename to util
/**
 * Server side model that provides data for the details of line coverage results in modified lines.
 */
public class LineCoverageViewModel {

    private LineCoverageViewModel() {
        //this is a utility class
    }

    /**
     * Static method to extract modified lines of code and their respective coverage status from passed Node object.
     * @param node the root of the tree from which modified lines are extracted.
     * @return returns a List of FileWithModifiedLines objects.
     */
    public static List<FileWithModifiedLines> getFilesWithModifiedLines(final Node node) {
        var filesWithModifiedLinesList = new ArrayList<FileWithModifiedLines>();

        for (FileNode fileNode :  node.filterByModifiedLines().getAllFileNodes()) {
            var listOfMissedLines = new ArrayList<>(fileNode.getMissedLines());
            var listOfPartialLines = new ArrayList<>(fileNode.getPartiallyCoveredLines().keySet());
            List<Integer> listOfCoveredLines = new ArrayList<>();

            int i = 0;
            for (Integer a:fileNode.getLinesWithCoverage()) {
                if (fileNode.getCoveredCounters()[i] > 0 && fileNode.getMissedCounters()[i] == 0) {
                    listOfCoveredLines.add(a);
                }
                i++;
            }

            var modifiedLinesBlocksList = new ArrayList<ModifiedLinesBlock>();

            getModifiedLineBlocks(listOfCoveredLines, modifiedLinesBlocksList, LineCoverageType.COVERED);
            getModifiedLineBlocks(listOfMissedLines, modifiedLinesBlocksList, LineCoverageType.MISSED);
            getModifiedLineBlocks(listOfPartialLines, modifiedLinesBlocksList, LineCoverageType.PARTRIALLY_COVERED);

            FileWithModifiedLines changedFile = new FileWithModifiedLines(fileNode.getRelativePath(), modifiedLinesBlocksList);
            filesWithModifiedLinesList.add(changedFile);
        }
        return filesWithModifiedLinesList;
    }

    /**
     * This method parses over the modified lines of a file and combines consecutive line numbers with the same coverage type
     * into a modifiedLinesBlock object.
     * @param modifiedLines list containing the integer numbers of modified lines.
     * @param modifiedLinesBlocks list containing modifiedLinesBlock objects.
     * @param type type of coverage pertaining to each line of code (COVERED, MISSED, or PARTIALLY_COVERED)
     */
    public static void getModifiedLineBlocks(final List<Integer> modifiedLines,
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
