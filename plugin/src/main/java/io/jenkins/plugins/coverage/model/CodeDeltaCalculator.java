package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;

import io.jenkins.plugins.forensics.delta.DeltaCalculator;
import io.jenkins.plugins.forensics.delta.DeltaCalculatorFactory;
import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.delta.model.FileEditType;
import io.jenkins.plugins.forensics.git.delta.GitDeltaCalculatorFactory;

public class CodeDeltaCalculator {

    private final Run<?, ?> build;
    private final FilePath workspace;
    private final TaskListener listener;

    public CodeDeltaCalculator(@NonNull final Run<?, ?> build, @NonNull final FilePath workspace,
            @NonNull final TaskListener listener) {
        this.build = build;
        this.workspace = workspace;
        this.listener = listener;
    }

    public Optional<Delta> calculateCodeDeltaToReference(final Run<?, ?> referenceBuild, final FilteredLog log) {
        BuildData buildAction = build.getAction(BuildData.class);
        BuildData previousBuildAction = referenceBuild.getAction(BuildData.class);

        if (buildAction != null && previousBuildAction != null) {
            String commit = buildAction.lastBuild.getRevision().getSha1String();
            String previousCommit = previousBuildAction.lastBuild.getRevision().getSha1String();

            DeltaCalculator deltaCalculator = getDeltaCalculator();
            return deltaCalculator.calculateDelta(commit, previousCommit, log);
        }
        return Optional.empty();
    }

    public Map<String, FileChanges> getCoverageRelevantChanges(final Delta delta) {
        return delta.getFileChangesMap().values().stream()
                .filter(fileChange -> fileChange.getFileEditType().equals(FileEditType.MODIFY)
                        || fileChange.getFileEditType().equals(FileEditType.ADD))
                .collect(Collectors.toMap(
                        fileChange -> getFullyQualifiedFileName(fileChange.getFileName()), Function.identity()));
    }

    private DeltaCalculator getDeltaCalculator() {
        FilteredLog log = new FilteredLog("Calculate code delta");
        SCM scm = ((AbstractBuild<?, ?>) build).getProject().getScm();
        if (scm instanceof GitSCM) {
            GitDeltaCalculatorFactory gitDeltaCalculatorFactory = new GitDeltaCalculatorFactory();
            Optional<DeltaCalculator> gitDeltaCalculator = gitDeltaCalculatorFactory.createDeltaCalculator(scm, build,
                    workspace, listener, log);
            if (gitDeltaCalculator.isPresent()) {
                return gitDeltaCalculator.get();
            }
        }
        return DeltaCalculatorFactory
                .findDeltaCalculator(build, Collections.singleton(workspace), listener, log);
    }

    private String getFullyQualifiedFileName(final String path) {
        return path.replace(Paths.get("src", "main", "java") + File.separator, "");
    }
}
