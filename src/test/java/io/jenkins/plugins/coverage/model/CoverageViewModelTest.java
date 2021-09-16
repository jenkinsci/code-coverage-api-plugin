package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import hudson.model.Run;

import io.jenkins.plugins.coverage.model.CoverageViewModel.CoverageOverview;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageViewModel}.
 *
 * @author Ullrich Hafner
 */
class CoverageViewModelTest extends AbstractCoverageTest {
    private static final String DISPLAY_NAME = "Test View";

    @Test
    void shouldReportOverview() {
        CoverageViewModel model = new CoverageViewModel(mock(Run.class), readNode("jacoco-codingstyle.xml"),
                DISPLAY_NAME);

        assertThat(model.getDisplayName()).contains(DISPLAY_NAME);

        CoverageOverview overview = model.getOverview();
        assertThatJson(overview).node("elements").isArray().containsExactly(
                "Report", "Package", "File", "Class", "Method", "Instruction", "Line", "Conditional"
        );
        assertThatJson(overview).node("covered").isArray().containsExactly(
                1, 1, 7, 15, 97, 1260, 294, 109
        );
        assertThatJson(overview).node("missed").isArray().containsExactly(
                0, 0, 3, 3, 5, 90, 29, 7
        );
    }
}
