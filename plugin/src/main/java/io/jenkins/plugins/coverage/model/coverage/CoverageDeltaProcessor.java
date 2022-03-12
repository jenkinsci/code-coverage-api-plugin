package io.jenkins.plugins.coverage.model.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.hm.hafner.util.FilteredLog;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.FileCoverageNode;
import io.jenkins.plugins.forensics.delta.model.Change;
import io.jenkins.plugins.forensics.delta.model.ChangeEditType;
import io.jenkins.plugins.forensics.delta.model.FileChanges;

public class CoverageDeltaProcessor {

    public void attachChangedCodeLines(final CoverageNode coverageNode, final Map<String, FileChanges> codeChanges) {
        Map<String, CoverageNode> nodePathMapping = coverageNode.getAllFileCoverageNodes().stream()
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        codeChanges.forEach((path, fileChange) -> {
            if (nodePathMapping.containsKey(path)) {
                CoverageNode changedNode = nodePathMapping.get(path);
                if (changedNode instanceof FileCoverageNode) {
                    attachChanges((FileCoverageNode) changedNode,
                            fileChange.getChangesByType(ChangeEditType.INSERT));
                    attachChanges((FileCoverageNode) changedNode,
                            fileChange.getChangesByType(ChangeEditType.REPLACE));
                }
            }
        });
    }

    private void attachChanges(final FileCoverageNode changedNode, final Set<Change> relevantChanges) {
        for (Change change : relevantChanges) {
            for (int i = change.getFromLine(); i <= change.getToLine(); i++) {
                changedNode.addChangedCodeLine(i);
            }
        }
    }

    public void attachUnexpectedCoveragesChanges(final CoverageNode node, final CoverageNode referenceNode,
            final Map<String, FileChanges> codeChanges, final FilteredLog log) {
        log.logInfo("Obtaining unexpected coverage changes...");
        Map<String, FileCoverageNode> fileNodes = node.getAllFileCoverageNodes().stream()
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        Map<String, FileCoverageNode> referenceFileNodes = referenceNode.getAllFileCoverageNodes().stream()
                .filter(reference -> fileNodes.containsKey(reference.getPath()))
                .collect(Collectors.toMap(FileCoverageNode::getPath, Function.identity()));

        fileNodes.forEach((path, fileNode) ->
                getReferenceCoveragePerLine(referenceFileNodes, path).ifPresent(referenceCoveragePerLine -> {
                    SortedMap<Integer, Coverage> referenceCoverageMapping;
                    if (codeChanges.containsKey(path)) {
                        referenceCoverageMapping =
                                getAdjustedCoveragePerLine(referenceCoveragePerLine, codeChanges.get(path));
                    }
                    else {
                        referenceCoverageMapping = referenceCoveragePerLine;
                    }
                    fileNode.getCoveragePerLine().forEach((line, coverage) -> {
                        if (!fileNode.getChangedCodeLines().contains(line)) {
                            Coverage referenceCoverage = referenceCoverageMapping.get(line);
                            int covered = coverage.getCovered();
                            int referenceCovered = referenceCoverage.getCovered();
                            if (covered != referenceCovered) {
                                fileNode.putUnexpectedCoverageChange(line, covered - referenceCovered);
                            }
                        }
                    });
                }));
    }

    private Optional<SortedMap<Integer, Coverage>> getReferenceCoveragePerLine(
            final Map<String, FileCoverageNode> references, final String fullyQualifiedName) {
        if (references.containsKey(fullyQualifiedName)) {
            SortedMap<Integer, Coverage> coveragePerLine = references.get(fullyQualifiedName).getCoveragePerLine();
            if (coveragePerLine != null && !coveragePerLine.isEmpty()) {
                return Optional.of(coveragePerLine);
            }
        }
        return Optional.empty();
    }

    private SortedMap<Integer, Coverage> getAdjustedCoveragePerLine(
            final SortedMap<Integer, Coverage> coveragePerLine, final FileChanges fileChanges) {
        List<List<Coverage>> coverages = coveragePerLine.values().stream()
                .map(coverage -> new ArrayList<>(Collections.singletonList(coverage)))
                .collect(Collectors.toList());

        IntStream.range(0, coveragePerLine.lastKey() + 1)
                .filter(line -> !coveragePerLine.containsKey(line))
                .forEach(line -> {
                    if (line < coverages.size()) {
                        coverages.add(line, new ArrayList<>(Collections.singletonList(null)));
                    }
                    else {
                        coverages.add(new ArrayList<>(Collections.singletonList(null)));
                    }
                });

        fileChanges.getChangesByType(ChangeEditType.DELETE).forEach(change -> {
            for (int i = change.getChangedFromLine(); i <= change.getChangedToLine(); i++) {
                coverages.get(i).clear();
            }
        });

        fileChanges.getChangesByType(ChangeEditType.INSERT).forEach(change -> {
            List<Coverage> inserted = coverages.get(change.getChangedFromLine());
            for (int i = change.getFromLine(); i <= change.getToLine(); i++) {
                inserted.add(null);
            }
        });

        fileChanges.getChangesByType(ChangeEditType.REPLACE).forEach(change -> {
            List<Coverage> replaced = coverages.get(change.getChangedFromLine());
            replaced.clear(); // coverage of replaced code is irrelevant
            for (int i = change.getFromLine(); i <= change.getToLine(); i++) {
                replaced.add(null);
            }
            for (int i = change.getChangedFromLine() + 1; i <= change.getChangedToLine(); i++) {
                coverages.get(i).clear();
            }
        });

        List<Coverage> adjustedCoveragesList = coverages.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        SortedMap<Integer, Coverage> adjustedCoveragePerLine = new TreeMap<>();
        for (int line = 1; line < adjustedCoveragesList.size(); line++) {
            Coverage coverage = adjustedCoveragesList.get(line);
            if (coverage != null) {
                adjustedCoveragePerLine.put(line, coverage);
            }
        }
        return adjustedCoveragePerLine;
    }
}
