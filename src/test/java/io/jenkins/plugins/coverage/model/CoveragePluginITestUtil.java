package io.jenkins.plugins.coverage.model;

import java.util.List;
import hudson.model.Run;
import static org.assertj.core.api.Assertions.*;

/**
 * Test util for integration tests.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class CoveragePluginITestUtil {

    /**
     * Jacoco analysis model example file.
     */
    public static final String JACOCO_ANALYSIS_MODEL_FILE_NAME = "jacoco-analysis-model.xml";

    /**
     * Jacoco coding style example file.
     */
    public static final String JACOCO_CODING_STYLE_FILE_NAME = "jacoco-codingstyle.xml";

    /**
     * Jacoco coding style example file with decreased line coverage.
     */
    public static final String JACOCO_CODING_STYLE_DECREASED_FILE_NAME = "jacoco-codingstyle-decreased-line-coverage.xml";

    /**
     * Cobertura example file with lots of data.
     */
    public static final String COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME = "coverage-with-lots-of-data.xml";

    /**
     * Cobertura example file.
     */
    public static final String COBERTURA_COVERAGE_FILE_NAME = "cobertura-coverage.xml";

    /**
     * The number of total lines for file {@link #JACOCO_ANALYSIS_MODEL_FILE_NAME}.
     */
    public static final int JACOCO_ANALYSIS_MODEL_LINES_TOTAL = 6368;

    /**
     * The number of covered lines for file {@link #JACOCO_ANALYSIS_MODEL_FILE_NAME}.
     */
    public static final int JACOCO_ANALYSIS_MODEL_LINES_COVERED = 6083;

    /**
     * The number of total lines for file {@link #JACOCO_CODING_STYLE_FILE_NAME}.
     */
    public static final int JACOCO_CODING_STYLE_LINES_TOTAL = 323;

    /**
     * The number of covered lines for file {@link #JACOCO_CODING_STYLE_FILE_NAME}.
     */
    public static final int JACOCO_CODING_STYLE_LINES_COVERED = 294;

    /**
     * The number of covered lines for file {@link #COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED = 602;

    /**
     * The number of total lines for file {@link #COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL = 958;

    /**
     * The number of covered lines for file {@link #COBERTURA_COVERAGE_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_LINES_COVERED = 2;

    /**
     * The number of total lines for file {@link #COBERTURA_COVERAGE_FILE_NAME}.
     */
    public static final int COBERTURA_COVERAGE_LINES_TOTAL = 2;

    /**
     * Private default constructor avoiding public default constructor.
     */
    private CoveragePluginITestUtil() {

    }

    /**
     * Takes a build and asserts its line coverage.
     * @param totalLines the numbers of total lines of the files processed within the build. The order is important and should match to {@code coveredLines}
     * @param coveredLines the numbers of covered lines of the files processed within the build. The order is important and should match to {@code totalLines}
     * @param build the processed build
     */
    public static void assertLineCoverageResultsOfBuild(final List<Integer> totalLines, final List<Integer> coveredLines,
            final Run<?, ?> build) {
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        int totalCoveredLines = coveredLines.stream().mapToInt(x -> x).sum();
        int totalMissedLines =
                totalLines.stream().mapToInt(x -> x).sum() - coveredLines.stream().mapToInt(x -> x).sum();
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(
                        totalCoveredLines,
                        totalMissedLines
                ));
    }
}
