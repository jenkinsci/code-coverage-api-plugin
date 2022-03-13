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

/**
 * Calculates the code delta between a Jenkins build and a reference build.
 *
 * @author Florian Orendi
 */
public class CodeDeltaCalculator {

    private final Run<?, ?> build;
    private final FilePath workspace;
    private final TaskListener listener;

    /**
     * Creates a code delta calculator for a specific build.
     *
     * @param build
     *         The build
     * @param workspace
     *         The workspace
     * @param listener
     *         The listener
     */
    public CodeDeltaCalculator(@NonNull final Run<?, ?> build,
            @NonNull final FilePath workspace,
            @NonNull final TaskListener listener) {
        this.build = build;
        this.workspace = workspace;
        this.listener = listener;
    }

    /**
     * Calculates the code delta between the {@link #build} and the passed reference build.
     *
     * @param referenceBuild
     *         The reference build
     * @param log
     *         The log
     *
     * @return the {@link Delta code delta} as Optional if existent, else an empty Optional
     */
    public Optional<Delta> calculateCodeDeltaToReference(final Run<?, ?> referenceBuild, final FilteredLog log) {
        BuildData buildAction = build.getAction(BuildData.class);
        BuildData previousBuildAction = referenceBuild.getAction(BuildData.class);

        if (buildAction != null && previousBuildAction != null) {
            String commit = buildAction.lastBuild.getRevision().getSha1String();
            String previousCommit = previousBuildAction.lastBuild.getRevision().getSha1String();

            DeltaCalculator deltaCalculator = createDeltaCalculator();
            return deltaCalculator.calculateDelta(commit, previousCommit, log);
        }
        return Optional.empty();
    }

    /**
     * Gets all changes which are relevant for the change coverage (added and modified files).
     *
     * @param delta
     *         The code delta between the {@link #build} and its reference
     *
     * @return the filtered code changes
     */
    public Map<String, FileChanges> getChangeCoverageRelevantChanges(final Delta delta) {
        return delta.getFileChangesMap().values().stream()
                .filter(fileChange -> fileChange.getFileEditType().equals(FileEditType.MODIFY)
                        || fileChange.getFileEditType().equals(FileEditType.ADD))
                .collect(Collectors.toMap(
                        fileChange -> getFullyQualifiedFileName(fileChange.getFileName()), Function.identity()));
    }

    /**
     * Creates a {@link DeltaCalculator code delta calculator} which is compatible with the used SCM.
     *
     * @return an instance of the delta calculator
     */
    private DeltaCalculator createDeltaCalculator() {
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

    /**
     * Gets the fully qualified name of a file from the passed path. In case of Maven projects for example, the default
     * folder 'src/main/java' is removed to get the fully qualified name.
     *
     * @param path
     *         The path of the file within the project
     *
     * @return the fully qualified name of the file
     */
    private String getFullyQualifiedFileName(final String path) {
        return path.replace(Paths.get("src", "main", "java") + File.separator, "");
    }
}
