package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;

import hudson.FilePath;
import hudson.model.Run;
import hudson.util.TextFile;

import io.jenkins.plugins.coverage.source.AgentCoveragePainter;

/**
 * Server side model that provides the data for the source code view of the coverage results. The layout of the
 * associated view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 */
public class SourceViewModel extends CoverageViewModel {
    /**
     * Creates a new source view model instance.
     *
     * @param owner
     *         the owner of this view
     * @param fileNode
     *         the selected file node of the coverage tree
     */
    public SourceViewModel(final Run<?, ?> owner, final CoverageNode fileNode) {
        super(owner, fileNode);
    }

    /**
     * Returns the source file rendered in HTML.
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public String getSourceFileContent() {
        try {
            Optional<File> sourceFile = getSourceFile(getOwner().getRootDir(), getNode().getName(), getNode().getPath());
            if (sourceFile.isPresent()) {
                File file = sourceFile.get();
                if (file.toString().endsWith(".zip")) {
                    return unzip(file, AgentCoveragePainter.getTempName(getNode().getPath()));
                }
                return read(file);
            }
            return "n/a";
        }
        catch (IOException | InterruptedException exception) {
            return ExceptionUtils.getStackTrace(exception);
        }
    }

    private String read(final File file) throws IOException {
        return new TextFile(file).read();
    }

    private String unzip(final File zipFile, final String fileName) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("coverage-source");
        FilePath zipDir = new FilePath(tempDir.toFile());
        try {
            new FilePath(zipFile).unzip(zipDir);

            return read(tempDir.resolve(fileName.replace(".zip", ".source")).toFile());
        }
        finally {
            zipDir.deleteRecursive();
        }
    }
}
