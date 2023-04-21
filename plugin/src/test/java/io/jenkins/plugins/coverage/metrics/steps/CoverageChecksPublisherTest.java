package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.parser.PitestParser;

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
    private static final int ANNOTATIONS_COUNT_FOR_MODIFIED = 3;

    @Test
    void shouldShowQualityGateDetails() {
        var result = readJacocoResult("jacoco-codingstyle.xml");

        var publisher = new CoverageChecksPublisher(createActionWithoutDelta(result,
                CoverageQualityGateEvaluatorTest.createQualityGateResult()), result, REPORT_NAME,
                ChecksAnnotationScope.SKIP, createJenkins());

        var checkDetails = publisher.extractChecksDetails();
        var expectedSummary = toString("coverage-publisher-quality-gate.checks-expected-result");
        assertThat(checkDetails.getOutput()).isPresent().get().satisfies(output -> {
            assertThat(output.getSummary()).isPresent()
                    .get()
                    .asString()
                    .containsIgnoringWhitespaces(expectedSummary);
        });
    }

    @Test
    void shouldShowProjectBaselineForJaCoCo() {
        var result = readJacocoResult("jacoco-codingstyle.xml");

        var publisher = new CoverageChecksPublisher(createActionWithoutDelta(result), result, REPORT_NAME,
                ChecksAnnotationScope.SKIP, createJenkins());

        assertThatTitleIs(publisher, "Line Coverage: 91.02%, Branch Coverage: 93.97%");
    }

    @Test
    void shouldShowProjectBaselineForPit() {
        var result = readResult("mutations.xml", new PitestParser());

        var publisher = new CoverageChecksPublisher(createActionWithoutDelta(result), result, REPORT_NAME,
                ChecksAnnotationScope.SKIP, createJenkins());

        assertThatTitleIs(publisher, "Line Coverage: 93.84%, Mutation Coverage: 90.24%");
    }

    @ParameterizedTest(name = "should create checks (scope = {0}, expected annotations = {1})")
    @CsvSource({"SKIP, 0", "ALL_LINES, 18", "MODIFIED_LINES, 1"})
    void shouldCreateChecksReportPit(final ChecksAnnotationScope scope, final int expectedAnnotations) {
        var result = readResult("mutations.xml", new PitestParser());

        var publisher = new CoverageChecksPublisher(createCoverageBuildAction(result), result, REPORT_NAME,
                scope, createJenkins());

        assertThat(publisher.extractChecksDetails().getOutput()).isPresent().get().satisfies(output -> {
            assertSummary(output, "coverage-publisher-summary.checks-expected-result-pit");
            assertMutationAnnotations(output, expectedAnnotations);
        });
    }

    private void assertMutationAnnotations(final ChecksOutput output, final int expectedAnnotations) {
        assertThat(output.getChecksAnnotations()).hasSize(expectedAnnotations);
        if (expectedAnnotations == 1) {
            assertThat(output.getChecksAnnotations()).satisfiesExactly(
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Mutation survived");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getRawDetails()).contains("Survived mutations:\n"
                                + "- Replaced integer addition with subtraction (org.pitest.mutationtest.engine.gregor.mutators.MathMutator)\n");
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/coverage/parser/CoberturaParser.java");
                        assertThat(annotation.getMessage()).contains("One mutation survived in line 251");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(251);
                        assertThat(annotation.getEndLine()).isPresent().get().isEqualTo(251);
                    });
        }
    }

    private void assertThatTitleIs(final CoverageChecksPublisher publisher, final String expectedTitle) {
        var checkDetails = publisher.extractChecksDetails();
        assertThat(checkDetails.getOutput()).isPresent().get().satisfies(output -> {
            assertThat(output.getTitle()).isPresent()
                    .get()
                    .isEqualTo(expectedTitle);
        });
    }

    @ParameterizedTest(name = "should create checks (scope = {0}, expected annotations = {1})")
    @CsvSource({"SKIP, 0", "ALL_LINES, 36", "MODIFIED_LINES, 3"})
    void shouldCreateChecksReportJaCoCo(final ChecksAnnotationScope scope, final int expectedAnnotations) {
        var result = readJacocoResult("jacoco-codingstyle.xml");

        var publisher = new CoverageChecksPublisher(createCoverageBuildAction(result), result, REPORT_NAME, scope, createJenkins());

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
                    .isEqualTo("Line Coverage: 50.00% (+50.00%)");
            var expectedDetails = toString("coverage-publisher-details.checks-expected-result");
            assertThat(output.getText()).isPresent().get().asString().isEqualToNormalizingWhitespace(expectedDetails);
            assertChecksAnnotations(output, expectedAnnotations);
            assertSummary(output, "coverage-publisher-summary.checks-expected-result");
        });
    }

    private void assertSummary(final ChecksOutput checksOutput, final String fileName) {
        assertThat(checksOutput.getSummary()).isPresent()
                .get()
                .asString().isEqualToNormalizingWhitespace(toString(fileName));
    }

    private void assertChecksAnnotations(final ChecksOutput checksOutput, final int expectedAnnotations) {
        if (expectedAnnotations == ANNOTATIONS_COUNT_FOR_MODIFIED) {
            assertThat(checksOutput.getChecksAnnotations()).hasSize(expectedAnnotations).satisfiesExactly(
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Not covered line");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/util/TreeStringBuilder.java");
                        assertThat(annotation.getMessage()).contains("Line 61 is not covered by tests");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(61);
                        assertThat(annotation.getEndLine()).isPresent().get().isEqualTo(61);
                    },
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Not covered line");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/util/TreeStringBuilder.java");
                        assertThat(annotation.getMessage()).contains("Line 62 is not covered by tests");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(62);
                        assertThat(annotation.getEndLine()).isPresent().get().isEqualTo(62);
                    },
                    annotation -> {
                        assertThat(annotation.getTitle()).contains("Partially covered line");
                        assertThat(annotation.getAnnotationLevel()).isEqualTo(ChecksAnnotationLevel.WARNING);
                        assertThat(annotation.getPath()).contains("edu/hm/hafner/util/TreeStringBuilder.java");
                        assertThat(annotation.getMessage()).contains("Line 113 is only partially covered, one branch is missing");
                        assertThat(annotation.getStartLine()).isPresent().get().isEqualTo(113);
                        assertThat(annotation.getEndLine()).isPresent().get().isEqualTo(113);
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

    private CoverageBuildAction createCoverageBuildAction(final Node result) {
        var testCoverage = new CoverageBuilder().setMetric(Metric.LINE)
                .setCovered(1)
                .setMissed(1)
                .build();

        var run = mock(Run.class);
        when(run.getUrl()).thenReturn(BUILD_LINK);

        result.findFile("TreeStringBuilder.java")
                .ifPresent(file -> {
                    assertThat(file.getMissedLines()).contains(61, 62);
                    assertThat(file.getPartiallyCoveredLines()).contains(entry(113, 1));
                    file.addModifiedLines(61, 62, 113);
                });
        result.findFile("CoberturaParser.java")
                .ifPresent(file -> {
                    assertThat(file.getSurvivedMutations()).containsKey(251);
                    file.addModifiedLines(251);
                });

        return new CoverageBuildAction(run, COVERAGE_ID, REPORT_NAME, StringUtils.EMPTY, result,
                new QualityGateResult(), null, "refId",
                new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF, Metric.MODULE, Fraction.ONE_FIFTH)),
                List.of(testCoverage), new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF)), List.of(testCoverage),
                new TreeMap<>(Map.of(Metric.LINE, Fraction.ONE_HALF)), List.of(testCoverage), false);
    }

    private CoverageBuildAction createActionWithoutDelta(final Node result) {
        return createActionWithoutDelta(result, new QualityGateResult());
    }

    CoverageBuildAction createActionWithoutDelta(final Node result, final QualityGateResult qualityGateResult) {
        var run = mock(Run.class);
        when(run.getUrl()).thenReturn(BUILD_LINK);

        return new CoverageBuildAction(run, COVERAGE_ID, REPORT_NAME, StringUtils.EMPTY, result,
                qualityGateResult, null, "refId",
                new TreeMap<>(), List.of(), new TreeMap<>(), List.of(), new TreeMap<>(), List.of(), false);
    }
}
