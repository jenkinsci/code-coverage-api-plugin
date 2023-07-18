package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.List;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;

import io.jenkins.plugins.coverage.metrics.model.ChangedLinesModel;
import io.jenkins.plugins.coverage.metrics.model.FileWithChangedLinesCoverageModel;
import io.jenkins.plugins.coverage.metrics.model.Type;

public class LineCoverageViewModel {

    public static List<FileWithChangedLinesCoverageModel> getFilesWithChangedLines(final Node node) {
        var filesWithChangedLinesList = new ArrayList<FileWithChangedLinesCoverageModel>();

        for (FileNode fileNode :  node.filterByModifiedLines().getAllFileNodes()) {
            FileWithChangedLinesCoverageModel changedFile = new FileWithChangedLinesCoverageModel(fileNode.getRelativePath());

            var listOfCoveredLines = new ArrayList<>(fileNode.getLinesWithCoverage());
            var listOfMissedLines = new ArrayList<>(fileNode.getMissedLines());
            var listOfPartialLines = new ArrayList<>((fileNode.getPartiallyCoveredLines().keySet()));

            var changedLinesModelList = new ArrayList<ChangedLinesModel>();

            getChangedLineBlocks(listOfCoveredLines, changedLinesModelList, Type.COVERED);
            getChangedLineBlocks(listOfMissedLines, changedLinesModelList, Type.MISSED);
            getChangedLineBlocks(listOfPartialLines, changedLinesModelList, Type.PARTRIALLY_COVERED);


            changedFile.setListOfChangedLines(changedLinesModelList);
            filesWithChangedLinesList.add(changedFile);
        }
        return filesWithChangedLinesList;
    }

    public static void getChangedLineBlocks(final ArrayList<Integer> changedLines,
            final ArrayList<ChangedLinesModel> changedLinesModels, final Type type) {
        int currentLine = changedLines.get(0);
        for (int i = 0; i < changedLines.size(); i++){
            if (!changedLines.get(i).equals(changedLines.get(i + 1) - 1)) {
                ChangedLinesModel changedLinesBlock = new ChangedLinesModel(currentLine, i, type);
                changedLinesModels.add(changedLinesBlock);
                currentLine = i + 1;
            }
        }
    }

}
