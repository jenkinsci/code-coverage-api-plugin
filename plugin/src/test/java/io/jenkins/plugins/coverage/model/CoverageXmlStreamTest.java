package io.jenkins.plugins.coverage.model;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.SerializableTest;

import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.model.CoverageBuildAction.CoverageXmlStream;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;
import io.jenkins.plugins.coverage.targets.CoverageResult;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageXmlStream}.
 *
 * @author Ullrich Hafner
 */
class CoverageXmlStreamTest extends SerializableTest<CoverageNode> {
    @Override
    protected CoverageNode createSerializable() {
        // TODO: replace with actual result
        try {
            JacocoReportAdapter parser = new JacocoReportAdapter("unused");
            CoverageElementRegister.addCoverageElements(new JacocoReportAdapterDescriptor().getCoverageElements());
            CoverageResult result = parser.getResult(getResourceAsFile("jacoco-codingstyle.xml").toFile());
            result.stripGroup();

            return new CoverageNodeConverter().convert(result);
        }
        catch (CoverageException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void shouldSaveAndRestoreTree() {
        CoverageXmlStream xmlStream = new CoverageXmlStream();

        Path saved = createTempFile();
        CoverageNode convertedNode = createSerializable();

        xmlStream.write(saved, convertedNode);
        CoverageNode restored = xmlStream.read(saved);
        assertThat(restored).isEqualTo(convertedNode);
    }
}
