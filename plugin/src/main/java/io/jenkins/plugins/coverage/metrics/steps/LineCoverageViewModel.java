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

            //var listOfCoveredLines = new ArrayList<>(fileNode.getLinesWithCoverage());

            var changedLinesModelList = new ArrayList<ChangedLinesModel>();

            getChangedLineBlocks(listOfCoveredLines, changedLinesModelList, Type.COVERED);
            getChangedLineBlocks(listOfMissedLines, changedLinesModelList, Type.MISSED);
            getChangedLineBlocks(listOfPartialLines, changedLinesModelList, Type.PARTRIALLY_COVERED);


            changedFile.setListOfChangedLines(changedLinesModelList);
            filesWithChangedLinesList.add(changedFile);
        }
        return filesWithChangedLinesList;
    }

    public static void getChangedLineBlocks(final List<Integer> changedLines,
            final ArrayList<ChangedLinesModel> changedLinesModels, final Type type) {
        int currentLine = changedLines.get(0);
        for (int i = 0; i < changedLines.size(); i++){
            if (i == changedLines.size() - 1 || !changedLines.get(i).equals(changedLines.get(i + 1) - 1)) {
                var changedLinesBlock = new ChangedLinesModel(currentLine, changedLines.get(i), type);
                changedLinesModels.add(changedLinesBlock);
                if (i < changedLines.size() - 1) {
                    currentLine = changedLines.get(i + 1);
                }
            }
        }
    }

}
