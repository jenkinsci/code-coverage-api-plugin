package io.jenkins.plugins.coverage.source.code;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JGitUtil {

    private JGitUtil() {
        //  no-op
    }

    private static final WeakHashMap<String, List<SourceCodeFile>> CACHE_MAP = new WeakHashMap<>();

    /**
     * @return the branch's name
     */
    public static String getCurrentBranchName(String gitRepoPath) {
        try (Git git = Git.open(new File(gitRepoPath)); Repository repo = git.getRepository()) {
            return repo.getBranch();
        } catch (Exception e) {
            e.printStackTrace();
            return "*";
        }
    }

    /**
     * Get the last commit's SHA-1
     */
    public static Stream<RevCommit> genGitCommitStream(String gitRepoPath, int depth) {
        try (Git git = Git.open(new File(gitRepoPath))) {
            Iterator<RevCommit> it = git.log().setMaxCount(depth).call().iterator();
            return Stream.generate(() -> {
                if (it.hasNext())
                    return it.next();
                return null;
            }).limit(depth);
        } catch (Exception e) {
            throw new RuntimeException("generate commit stream failed.", e);
        }
    }

    public static String getCommitByIndexOfEnd(String gitRepoPath, int index) {
        return genGitCommitStream(gitRepoPath, index)
                .skip(Math.max(0, index - 1))
                .findFirst()
                .map(RevCommit::getName)
                .orElse(null);
    }

    /**
     * Get the last commit's SHA-1
     */
    public static String getLastCommit(String gitRepoPath) {
        return getCommitByIndexOfEnd(gitRepoPath, 1);
    }

    public static List<SourceCodeFile> analysisLastCommitAddCodeBlock(String gitRepoPath) {
        String[] commits = genGitCommitStream(gitRepoPath, 2)
                .filter(Objects::nonNull)
                .map(RevCommit::getName)
                .toArray(String[]::new);
        if (commits.length < 2)
            return Collections.emptyList();
        return analysisAddCodeBlock(gitRepoPath, commits[1], commits[0]);
    }

    /**
     * analysis new commit's added code block.
     */
    public static List<SourceCodeFile> analysisAddCodeBlock(String gitRepoPath, String oldCommit, String newCommit) {
        Objects.requireNonNull(newCommit, "the newest commit is missing.");
        String cacheKey = oldCommit + "-" + newCommit;
        List<SourceCodeFile> sourceCodeFiles = CACHE_MAP.get(cacheKey);
        if (null != sourceCodeFiles) {
            return sourceCodeFiles;
        }

        if (oldCommit == null || oldCommit.isEmpty()) {
            oldCommit = getCommitByIndexOfEnd(gitRepoPath, 2);
        }
        //  check if same commit
        if (newCommit.equalsIgnoreCase(oldCommit))
            return Collections.emptyList();

        try (Git git = Git.open(new File(gitRepoPath))) {
            Stream<DiffEntry> stream = getDifferentBetweenTwoCommit(git, oldCommit, newCommit);
            if (null == stream)
                return null;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                DiffFormatter df = new DiffFormatter(out);
                // ignores all whitespace
                df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
                df.setRepository(git.getRepository());

                List<SourceCodeFile> map = stream.map(diffEntry -> {
                    try {
                        FileHeader header = df.toFileHeader(diffEntry);
                        //  analysis new add code block.
                        List<SourceCodeBlock> list = header.getHunks().stream()
                                .flatMap((Function<HunkHeader, Stream<Edit>>) hunk -> hunk.toEditList().stream())
                                .filter(edit -> edit.getEndB() - edit.getBeginB() > 0)
                                .map(edit -> SourceCodeBlock.of(edit.getBeginB(), edit.getEndB()))
                                .collect(Collectors.toList());
                        if (list.isEmpty())
                            return null;
                        return new SourceCodeFile(diffEntry.getNewPath(), list);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        out.reset();
                    }
                })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                CACHE_MAP.put(cacheKey, map);
                return map;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get different code block's data by two git commit.
     *
     * @param oldCommit the old commit.
     * @param newCommit the new commit.
     */
    public static Stream<DiffEntry> getDifferentBetweenTwoCommit(Git git, String oldCommit, String newCommit) {
        try (Repository repo = git.getRepository()) {
            AbstractTreeIterator oldAti = prepareTreeParser(repo, parseCommitBySHA(repo, oldCommit));
            if (oldAti == null)
                return null;
            AbstractTreeIterator newAti = prepareTreeParser(repo, parseCommitBySHA(repo, newCommit));
            if (newAti == null)
                return null;

            List<DiffEntry> list = git.diff()
                    .setOldTree(oldAti)
                    .setNewTree(newAti)
                    .call();
            return list.stream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RevCommit parseCommitBySHA(Repository repository, String sha1) {
        ObjectId commitId = ObjectId.fromString(sha1);
        try (RevWalk revWalk = new RevWalk(repository)) {
            return revWalk.parseCommit(commitId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) {
        if (null == commit
                || null == repository)
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            try (ObjectReader oldReader = repository.newObjectReader()) {
                oldTreeParser.reset(oldReader, tree.getId());
            }
            walk.dispose();
            return oldTreeParser;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
