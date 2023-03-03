package io.jenkins.plugins.coverage.metrics.steps;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest(name = "should create checks (skip annotations = {0})")
    @ValueSource(booleans = {true, false})
    void shouldCreateChecksReport(final boolean skip) {
        var publisher = new CoverageChecksPublisher(createCoverageBuildAction(), REPORT_NAME,
                skip ? ChecksAnnotationScope.SKIP : ChecksAnnotationScope.MODIFIED_LINES, createJenkins());

        var checkDetails = publisher.extractChecksDetails();

        assertThat(checkDetails.getName()).isPresent().get().isEqualTo(REPORT_NAME);
        assertThat(checkDetails.getStatus()).isEqualTo(ChecksStatus.COMPLETED);
        assertThat(checkDetails.getConclusion()).isEqualTo(ChecksConclusion.SUCCESS);
        assertThat(checkDetails.getDetailsURL()).isPresent()
                .get()
                .isEqualTo("http://127.0.0.1:8080/job/pipeline-coding-style/job/5/coverage");
        assertThatDetailsAreCorrect(checkDetails, skip);
    }

    private void assertThatDetailsAreCorrect(final ChecksDetails checkDetails, final boolean skip) {
        assertThat(checkDetails.getOutput()).isPresent().get().satisfies(output -> {
            assertThat(output.getTitle()).isPresent()
                    .get()
                    .isEqualTo("Modified code lines: 50.00% (1/2)");
            assertThat(output.getText()).isEmpty();
            assertSummary(output);
            assertChecksAnnotations(output, skip);
        });
    }

    private void assertSummary(final ChecksOutput checksOutput) throws IOException {
        var expectedContent = Files.readString(getResourceAsFile("coverage-publisher-summary.MD"));
        assertThat(checksOutput.getSummary()).isPresent()
                .get()
                .isEqualTo(expectedContent);
    }

    private void assertChecksAnnotations(final ChecksOutput checksOutput, final boolean skip) {
        if (skip) {
            assertThat(checksOutput.getChecksAnnotations()).isEmpty();
        }
        else {
            assertThat(checksOutput.getChecksAnnotations()).hasSize(2);
            assertThat(checksOutput.getChecksAnnotations().get(0)).satisfies(annotation -> {
                assertThat(annotation.getTitle()).isPresent().get().isEqualTo("Missing Coverage");
                assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                assertThat(annotation.getPath()).isPresent().get().isEqualTo("edu/hm/hafner/util/TreeStringBuilder.java");
                assertThat(annotation.getMessage()).isPresent().get()
                        .isEqualTo("Changed line #L160 is not covered by tests");
                assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(160);
                assertThat(annotation.getEndLine()).isPresent().get().isEqualTo(160);
            });
            assertThat(checksOutput.getChecksAnnotations().get(1)).satisfies(annotation -> {
                assertThat(annotation.getTitle()).isPresent().get().isEqualTo("Missing Coverage");
                assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                assertThat(annotation.getPath()).isPresent().get().isEqualTo("edu/hm/hafner/util/TreeStringBuilder.java");
                assertThat(annotation.getMessage()).isPresent().get()
                        .isEqualTo("Changed lines #L162 - L164 are not covered by tests");
                assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(162);
                assertThat(annotation.getEndLine()).isPresent().get().isEqualTo(164);
            });
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
        result.getAllFileNodes().stream().filter(file -> file.getName().equals("TreeStringBuilder.java")).findFirst()
                .ifPresent(file -> {
                    assertThat(file.getLinesWithCoverage()).contains(160, 162, 163, 164);
                    file.addModifiedLine(160);
                    file.addModifiedLine(162);
                    file.addModifiedLine(163);
                    file.addModifiedLine(164);
                });

        return new CoverageBuildAction(run, COVERAGE_ID, REPORT_NAME, StringUtils.EMPTY, result,
                new QualityGateResult(), null, "refId",
                new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF, Metric.MODULE, Fraction.ONE_FIFTH)),
                List.of(testCoverage), new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF)), List.of(testCoverage),
                new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF)), List.of(testCoverage), false);
    }
}
