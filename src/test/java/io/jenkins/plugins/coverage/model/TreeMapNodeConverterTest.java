package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageResult;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link TreeMapNodeConverter}.
 *
 * @author Ullrich Hafner
 */
class TreeMapNodeConverterTest extends AbstractCoverageTest {
    @Test
    void shouldConvertCodingStyleToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-codingstyle.xml");

        CoverageNode tree = CoverageNodeConverter.convert(report);
        tree.splitPackages();

        TreeChartNode root = new TreeMapNodeConverter().toTeeChartModel(tree);
        assertThat(root).hasName("Java coding style: jacoco-codingstyle.xml").hasValue(323.0, 294.0);

        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> Assertions.assertThat(node).hasName("edu.hm.hafner.util").hasValue(323.0, 294.0)
        );
    }

    @Test
    void shouldConvertAnalysisModelToTree() throws CoverageException {
        CoverageResult report = readReport("jacoco-analysis-model.xml");

        CoverageNode tree = CoverageNodeConverter.convert(report);
        tree.splitPackages();

        TreeChartNode root = new TreeMapNodeConverter().toTeeChartModel(tree);

        assertThat(root).hasName("Static Analysis Model and Parsers: jacoco-analysis-model.xml").hasValue(6368.0, 6083.0);
        assertThat(root.getChildren()).hasSize(1).element(0).satisfies(
                node -> Assertions.assertThat(node).hasName("edu.hm.hafner").hasValue(6368.0, 6083.0)
        );
    }
}
