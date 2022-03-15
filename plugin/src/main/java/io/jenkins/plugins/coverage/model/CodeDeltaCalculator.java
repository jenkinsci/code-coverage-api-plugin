package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import io.jenkins.plugins.forensics.delta.DeltaCalculatorFactory;
import io.jenkins.plugins.forensics.delta.model.Delta;
import io.jenkins.plugins.forensics.delta.model.FileChanges;
import io.jenkins.plugins.forensics.delta.model.FileEditType;

/**
 * Calculates the code delta between a Jenkins build and a reference build.
 *
 * @author Florian Orendi
 */
public class CodeDeltaCalculator {

    private final Run<?, ?> build;
    private final FilePath workspace;
    private final TaskListener listener;
    private final String scm;

    /**
     * The source directories are ordered descending by their length in order to grant a maximum match.
     */
    private final SortedSet<String> sourceDirectories =
            new TreeSet<>(Comparator.comparing(String::length, Comparator.reverseOrder()));

    /**
     * Creates a code delta calculator for a specific build.
     *
     * @param build
     *         The build
     * @param workspace
     *         The workspace
     * @param listener
     *         The listener
     * @param sourceDirectories
     *         All source directories which contain code
     */
    public CodeDeltaCalculator(@NonNull final Run<?, ?> build,
            @NonNull final FilePath workspace,
            @NonNull final TaskListener listener,
            @NonNull final String scm,
            @NonNull final Set<String> sourceDirectories
    ) {
        this.build = build;
        this.workspace = workspace;
        this.listener = listener;
        this.scm = scm;
        this.sourceDirectories.addAll(sourceDirectories);
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
        return DeltaCalculatorFactory
                .findDeltaCalculator(scm, build, workspace, listener, log)
                .calculateDelta(build, referenceBuild, scm, log);
    }

    /**
     * Gets all file changes which are relevant for the change coverage (added and modified files) - mapped by the fully
     * qualified name of the file.
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
     * Gets the fully qualified name of a file from the passed absolute path of the file within the workspace. If the
     * absolute path starts with one of the defined source directories, the maximum matching source directory path
     * prefix is removed since it does not belong to the fully qualified name of the file.
     *
     * @param absolutePath
     *         The path of the file within the project
     *
     * @return the fully qualified name of the file
     */
    private String getFullyQualifiedFileName(final String absolutePath) {
        for (String path : sourceDirectories) {
            if (absolutePath.startsWith(path)) {
                String fullyQualifiedName = absolutePath.replaceFirst(path, "");
                if (fullyQualifiedName.startsWith(File.separator)) {
                    fullyQualifiedName = fullyQualifiedName.replaceFirst(File.separator, "");
                }
                return fullyQualifiedName;
            }
        }
        return absolutePath;
    }
}
