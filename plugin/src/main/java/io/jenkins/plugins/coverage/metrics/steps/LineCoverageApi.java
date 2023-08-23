package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;

/**
 * Remote API to list the details of modified line coverage results.
 */
@ExportedBean
public class LineCoverageApi {
    private final List<FileWithModifiedLines> filesWithModifiedLines;

    /**
     * Constructor for the LineCoverageApi class.
     * @param filesWithModifiedLines List of files with modified lines to expose via the api.
     */
    public LineCoverageApi(final List<FileWithModifiedLines> filesWithModifiedLines) {
        this.filesWithModifiedLines = filesWithModifiedLines;
    }

    @Exported(inline = true)
    public List<FileWithModifiedLines> getFilesWithModifiedLines() {
        return filesWithModifiedLines;
    }
}
