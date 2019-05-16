package io.jenkins.plugins.coverage.targets;

import io.jenkins.plugins.coverage.source.code.JGitUtil;
import io.jenkins.plugins.coverage.source.code.SourceCodeFile;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExportedBean
public class CoverageSourceFileAnalysis implements Serializable {
    private static final long serialVersionUID = -3114190475749094715L;

    /**
     * Source file's root path.
     */
    private String rootPath;
    /**
     * the branch name.
     */
    private String branchName;
    /**
     * the last commit name.
     */
    private String lastCommitName;
    /**
     * the branch name matching regex. default is ".*"
     */
    private String branchNameMatchRegex = ".*";
    /**
     * relative coverage summary's level.
     */
    private CoverageElement level;

    public CoverageSourceFileAnalysis(String rootPath, String branchNameMatchRegex, CoverageElement level, String branchName, String lastCommitName) {
        this.rootPath = rootPath;
        this.branchNameMatchRegex = branchNameMatchRegex;
        this.level = level;
        this.branchName = branchName;
        this.lastCommitName = lastCommitName;
    }

    @Exported
    public String getRootPath() {
        return rootPath;
    }

    @Exported
    public String getBranchNameMatchRegex() {
        return branchNameMatchRegex;
    }

    @Exported
    public CoverageElement getLevel() {
        return level;
    }

    @Exported
    public String getBranchName() {
        return branchName;
    }

    @Exported
    public String getLastCommitName() {
        return lastCommitName;
    }

    public String getTargetLastCommitName(CoverageResult current) {
        CoverageSourceFileAnalysis vcs = resolveTargetVCS(current);
        return vcs == null ? null : vcs.getLastCommitName();
    }

    public CoverageSourceFileAnalysis resolveTargetVCS(CoverageResult current) {
        CoverageResult previousResult = current;
        while ((previousResult = previousResult.getPreviousResult()) != null) {
            CoverageSourceFileAnalysis vcs = previousResult.getCoverageSourceFileAnalysis();
            if (vcs != null
                    && vcs.getBranchName().matches(this.branchNameMatchRegex)) {
                return vcs;
            }
        }
        return null;
    }

    public List<CoverageRelativeResultElement> getCoverageRelativeResultElement(CoverageResult report) {
        //  the relative summary feature is disabled
        final CoverageSourceFileAnalysis csfa = report.getCoverageSourceFileAnalysis();
        if (csfa == null)
            return Collections.emptyList();

        String lastVcsCommitName = csfa.getLastCommitName();
        if (null == lastVcsCommitName)
            return Collections.emptyList();

        List<SourceCodeFile> scbInfo = JGitUtil.analysisAddCodeBlock(csfa.getRootPath(), csfa.getTargetLastCommitName(report), lastVcsCommitName);
        if (scbInfo == null || scbInfo.isEmpty())
            return Collections.emptyList();

        return report.getChildrenResults()
                .parallelStream()
                .filter(cr -> CoverageElement.FILE.equals(cr.getElement()))
                .filter(cr -> cr.getPaint() != null)
                .map(cr -> scbInfo.stream()
                        .filter(p -> p.getPath().endsWith(cr.getRelativeSourcePath()))
                        .limit(1)
                        .findAny()
                        .map(scf -> {
                            int[] lines = scf.getBlocks()
                                    .stream()
                                    .flatMapToInt(block -> IntStream.rangeClosed((int) (block.getStartLine() + 1), (int) block.getEndLine())
                                            .filter(line -> cr.getPaint().isPainted(line))
                                    ).toArray();
                            return new CoverageRelativeResultElement(cr.getRelativeSourcePath(), analysisCoverageByLevel(cr.getPaint(), csfa.getLevel(), lines));
                        })
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * calculate code coverage by analysis level.
     */
    private Ratio analysisCoverageByLevel(CoveragePaint paint, CoverageElement level, int[] sourceCodeBlockLines) {
        if (CoverageElement.LINE.equals(level)) {
            long missed = Arrays.stream(sourceCodeBlockLines).filter(line -> paint.getHits(line) <= 0).count();
            long covered = Arrays.stream(sourceCodeBlockLines).filter(line -> paint.getHits(line) > 0).count();
            return Ratio.create(covered, missed + covered);
        } else {
            //  analysis with [Conditional]
            long missedBranch = Arrays.stream(sourceCodeBlockLines).map(line -> {
                int covered = paint.getBranchCoverage(line);
                if (covered <= 0 && paint.getHits(line) > 0)
                    return 1;
                else
                    return paint.getBranchTotal(line) - covered;
            }).sum();
            long coveredBranch = Arrays.stream(sourceCodeBlockLines).map(line -> {
                int covered = paint.getBranchCoverage(line);
                if (covered <= 0 && paint.getHits(line) > 0)
                    return 1;
                else
                    return covered;
            }).sum();
            return Ratio.create(missedBranch, coveredBranch + missedBranch);
        }
    }
}
