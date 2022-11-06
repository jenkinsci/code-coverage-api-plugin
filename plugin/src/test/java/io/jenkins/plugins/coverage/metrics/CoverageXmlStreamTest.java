package io.jenkins.plugins.coverage.metrics;

import java.io.InputStreamReader;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.parser.JacocoParser;
import edu.hm.hafner.util.SerializableTest;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageXmlStream}.
 *
 * @author Ullrich Hafner
 */
class CoverageXmlStreamTest extends SerializableTest<Node> {
    @Override
    protected Node createSerializable() {
        return new JacocoParser().parse(new InputStreamReader(asInputStream("jacoco-codingstyle.xml")));
    }

    @Test
    void shouldSaveAndRestoreTree() {
        CoverageXmlStream xmlStream = new CoverageXmlStream();

        Path saved = createTempFile();
        Node convertedNode = createSerializable();

        xmlStream.write(saved, convertedNode);
        Node restored = xmlStream.read(saved);
        assertThat(restored).usingRecursiveComparison().isEqualTo(convertedNode);
    }
}
