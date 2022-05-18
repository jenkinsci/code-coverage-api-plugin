package io.jenkins.plugins.coverage.model;

import java.nio.file.Path;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.SerializableTest;

import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;
import io.jenkins.plugins.coverage.model.CoverageBuildAction.CoverageXmlStream;
import io.jenkins.plugins.coverage.model.CoverageBuildAction.LineMapConverter;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;
import io.jenkins.plugins.coverage.targets.CoverageResult;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link CoverageXmlStream}.
 *
 * @author Ullrich Hafner
 */
class CoverageXmlStreamTest extends SerializableTest<CoverageNode> {
    private static final CoverageBuilder BUILDER = new CoverageBuilder();

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

    @Test
    void shouldConvertMap2String() {
        TreeMap<Integer, Coverage> map = new TreeMap<>();

        LineMapConverter converter = new LineMapConverter();

        assertThat(converter.marshal(map)).isEmpty();

        map.put(10, BUILDER.setCovered(5).setTotal(8).build());
        assertThat(converter.marshal(map)).isEqualTo("10: 5/8");

        map.put(20, BUILDER.setCovered(6).setTotal(6).build());
        assertThat(converter.marshal(map)).isEqualTo("10: 5/8, 20: 6/6");
    }

    @Test
    void shouldConvertString2Map() {
        LineMapConverter converter = new LineMapConverter();

        assertThat(converter.unmarshal("")).isEmpty();
        Coverage first = BUILDER.setCovered(5).setTotal(8).build();
        assertThat(converter.unmarshal("10: 5/8"))
                .containsExactly(entry(10, first));
        assertThat(converter.unmarshal("10: 5/8, 20: 6/6"))
                .containsExactly(entry(10, first), entry(20, BUILDER.setCovered(6).setTotal(6).build()));
    }
}
