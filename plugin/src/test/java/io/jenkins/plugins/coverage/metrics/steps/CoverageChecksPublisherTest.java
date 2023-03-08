package io.jenkins.plugins.coverage.metrics.steps;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;

import hudson.model.Run;

import io.jenkins.plugins.checks.api.ChecksAnnotation.ChecksAnnotationLevel;
import io.jenkins.plugins.checks.api.ChecksConclusion;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksOutput;
import io.jenkins.plugins.checks.api.ChecksStatus;
import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder.ChecksAnnotationScope;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.QualityGateResult;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DefaultLocale("en")
class CoverageChecksPublisherTest extends AbstractCoverageTest {
    private static final String JENKINS_BASE_URL = "http://127.0.0.1:8080";
    private static final String BUILD_LINK = "job/pipeline-coding-style/job/5";
    private static final String COVERAGE_ID = "coverage";
    private static final String REPORT_NAME = "Name";

    @ParameterizedTest(name = "should create checks (scope = {0}, expected annotations = {1})")
    @CsvSource({"SKIP, 0", "ALL_LINES, 36", "MODIFIED_LINES, 3"})
    void shouldCreateChecksReport(final ChecksAnnotationScope scope, final int expectedAnnotations) {
        var publisher = new CoverageChecksPublisher(createCoverageBuildAction(), REPORT_NAME, scope, createJenkins());

        var checkDetails = publisher.extractChecksDetails();

        assertThat(checkDetails.getName()).isPresent().get().isEqualTo(REPORT_NAME);
        assertThat(checkDetails.getStatus()).isEqualTo(ChecksStatus.COMPLETED);
        assertThat(checkDetails.getConclusion()).isEqualTo(ChecksConclusion.SUCCESS);
        assertThat(checkDetails.getDetailsURL()).isPresent()
                .get()
                .isEqualTo("http://127.0.0.1:8080/job/pipeline-coding-style/job/5/coverage");
        assertThatDetailsAreCorrect(checkDetails, expectedAnnotations);
    }

    private void assertThatDetailsAreCorrect(final ChecksDetails checkDetails, final int expectedAnnotations) {
        assertThat(checkDetails.getOutput()).isPresent().get().satisfies(output -> {
            assertThat(output.getTitle()).isPresent()
                    .get()
                    .isEqualTo("Modified code lines: 50.00% (1/2)");
            assertThat(output.getText()).isEmpty();
            assertChecksAnnotations(output, expectedAnnotations);
            assertSummary(output);
        });
    }

    private void assertSummary(final ChecksOutput checksOutput) throws IOException {
        var expectedContent = Files.readString(getResourceAsFile("coverage-publisher-summary.md"));
        assertThat(checksOutput.getSummary()).isPresent()
                .get()
                .isEqualTo(expectedContent);
    }

    private void assertChecksAnnotations(final ChecksOutput checksOutput, final int expectedAnnotations) {
        if (expectedAnnotations == 3) {
            assertThat(checksOutput.getChecksAnnotations()).hasSize(expectedAnnotations).satisfiesExactly(
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Not covered line");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/util/TreeStringBuilder.java");
                        assertThat(annotation.getMessage()).contains("Line 61 is not covered by tests");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(61);
                    },
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Not covered line");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/util/TreeStringBuilder.java");
                        assertThat(annotation.getMessage()).contains("Line 62 is not covered by tests");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(62);
                    },
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Partially covered line");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/util/TreeStringBuilder.java");
                        assertThat(annotation.getMessage()).contains("Line 113 is only partially covered, one branch is missing");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(113);
                    });
        }
        else {
            assertThat(checksOutput.getChecksAnnotations()).hasSize(expectedAnnotations);
        }
    }

    private JenkinsFacade createJenkins() {
        JenkinsFacade jenkinsFacade = mock(JenkinsFacade.class);
        when(jenkinsFacade.getAbsoluteUrl(BUILD_LINK, COVERAGE_ID)).thenReturn(
                JENKINS_BASE_URL + "/" + BUILD_LINK + "/" + COVERAGE_ID);
        return jenkinsFacade;
    }

    private CoverageBuildAction createCoverageBuildAction() {
        var testCoverage = new CoverageBuilder().setMetric(Metric.LINE)
                .setCovered(1)
                .setMissed(1)
                .build();

        var run = mock(Run.class);
        when(run.getUrl()).thenReturn(BUILD_LINK);
        var result = readJacocoResult("jacoco-codingstyle.xml");
        result.findFile("TreeStringBuilder.java")
                .ifPresent(file -> {
                    assertThat(file.getMissedLines()).contains(61, 62);
                    assertThat(file.getPartiallyCoveredLines()).contains(entry(113, 1));
                    file.addModifiedLines(61, 62, 113);
                });

        return new CoverageBuildAction(run, COVERAGE_ID, REPORT_NAME, StringUtils.EMPTY, result,
                new QualityGateResult(), null, "refId",
                new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF, Metric.MODULE, Fraction.ONE_FIFTH)),
                List.of(testCoverage), new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF)), List.of(testCoverage),
                new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF)), List.of(testCoverage), false);
    }
}
