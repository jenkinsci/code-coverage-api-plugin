package io.jenkins.plugins.coverage.model.visualization;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import hudson.model.Run;

import io.jenkins.plugins.coverage.model.AbstractCoverageTest;
import io.jenkins.plugins.coverage.model.visualization.CoverageViewModel.CoverageOverview;
import io.jenkins.plugins.coverage.model.visualization.code.SourceViewModel;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageViewModel}.
 *
 * @author Ullrich Hafner
 */
class CoverageViewModelTest extends AbstractCoverageTest {
    @Test
    void shouldReportOverview() {
        CoverageViewModel model = createModel();

        assertThat(model.getDisplayName()).contains("Java coding style: jacoco-codingstyle.xml");

        CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("metrics").isArray().containsExactly(
                "Package", "File", "Class", "Method", "Line", "Instruction", "Branch"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                1, 7, 15, 97, 294, 1260, 109
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 3, 3, 5, 29, 90, 7
        );

        assertThat(model.getDynamic("unknown", null, null))
                .isNull();
    }

    @Test
    void shouldReturnEmptySourceViewForExistingLinkButMissingSourceFile() {
        CoverageViewModel model = createModel();

        String link = String.valueOf("PathUtil.java".hashCode());
        assertThat(model.getDynamic(link, null, null))
                .extracting(SourceViewModel::getSourceFileContent).isEqualTo("n/a");
    }

    private CoverageViewModel createModel() {
        return new CoverageViewModel(mock(Run.class),
                readNode(Paths.get("..", "jacoco-codingstyle.xml").toString()));
    }
}
