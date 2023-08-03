package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import io.jenkins.plugins.coverage.metrics.model.LineCoverageType;
import io.jenkins.plugins.coverage.metrics.model.ModifiedLinesBlock;
import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;

@ExportedBean
public class LineCoverageViewModel {

    private final List<FileWithModifiedLines> filesWithModifiedLines;

    public LineCoverageViewModel(final Node node) {
        this.filesWithModifiedLines = getFilesWithModifiedLines(node);
    }

    public static List<FileWithModifiedLines> getFilesWithModifiedLines(final Node node) {
        var filesWithModifiedLinesList = new ArrayList<FileWithModifiedLines>();

        for (FileNode fileNode :  node.filterByModifiedLines().getAllFileNodes()) {


            var listOfMissedLines = new ArrayList<>(fileNode.getMissedLines());
            var listOfPartialLines = new ArrayList<>((fileNode.getPartiallyCoveredLines().keySet()));
            List<Integer> listOfCoveredLines = new ArrayList<>();

            int i = 0;
            for (Integer a:fileNode.getLinesWithCoverage()
            ) {
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

    @Exported(inline = true)
    public List<FileWithModifiedLines> getFilesWithModifiedLines() {
        return filesWithModifiedLines;
    }

    public static void getModifiedLineBlocks(final List<Integer> changedLines,
            final ArrayList<ModifiedLinesBlock> modifiedLinesBlocks, final LineCoverageType type) {
        int currentLine = changedLines.get(0);
        for (int i = 0; i < changedLines.size(); i++){
            if (i == changedLines.size() - 1 || !changedLines.get(i).equals(changedLines.get(i + 1) - 1)) {
                var changedLinesBlock = new ModifiedLinesBlock(currentLine, changedLines.get(i), type);
                modifiedLinesBlocks.add(changedLinesBlock);
                if (i < changedLines.size() - 1) {
                    currentLine = changedLines.get(i + 1);
                }
            }
        }
    }

}
