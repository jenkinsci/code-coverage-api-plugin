package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;

import edu.hm.hafner.coverage.Node;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import io.jenkins.plugins.coverage.metrics.model.FileWithModifiedLines;

@ExportedBean
public class LineCoverageApi {
    private final List<FileWithModifiedLines> filesWithModifiedLines;

    public LineCoverageApi(final List<FileWithModifiedLines> filesWithModifiedLines) {
        this.filesWithModifiedLines = filesWithModifiedLines;
    }

    @Exported(inline = true)
    public List<FileWithModifiedLines> getFilesWithModifiedLines() {
        return filesWithModifiedLines;
    }
}
