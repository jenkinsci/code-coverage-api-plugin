package io.jenkins.plugins.coverage.model;

import java.nio.file.Path;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.SerializableTest;

import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;
import io.jenkins.plugins.coverage.model.CoverageXmlStream.HitsMapConverter;
import io.jenkins.plugins.coverage.model.CoverageXmlStream.IntegerSetConverter;
import io.jenkins.plugins.coverage.model.CoverageXmlStream.LineMapConverter;
import io.jenkins.plugins.coverage.model.CoverageXmlStream.MetricPercentageMapConverter;
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
    private static final String EMPTY = "[]";

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
        NavigableMap<Integer, Coverage> map = new TreeMap<>();

        LineMapConverter converter = new LineMapConverter();

        assertThat(converter.marshal(map)).isEqualTo(EMPTY);

        map.put(10, BUILDER.setCovered(5).setTotal(8).build());
        assertThat(converter.marshal(map)).isEqualTo("[10: 5/8]");

        map.put(20, BUILDER.setCovered(6).setTotal(6).build());
        assertThat(converter.marshal(map)).isEqualTo("[10: 5/8, 20: 6/6]");
    }

    @Test
    void shouldConvertString2Map() {
        LineMapConverter converter = new LineMapConverter();

        assertThat(converter.unmarshal(EMPTY)).isEmpty();
        Coverage first = BUILDER.setCovered(5).setTotal(8).build();
        assertThat(converter.unmarshal("[10: 5/8]"))
                .containsExactly(entry(10, first));
        assertThat(converter.unmarshal("[10: 5/8, 20: 6/6]"))
                .containsExactly(entry(10, first), entry(20, BUILDER.setCovered(6).setTotal(6).build()));
    }

    @Test
    void shouldConvertMetricMap2String() {
        NavigableMap<CoverageMetric, CoveragePercentage> map = new TreeMap<>();

        MetricPercentageMapConverter converter = new MetricPercentageMapConverter();

        assertThat(converter.marshal(map)).isEqualTo(EMPTY);

        map.put(CoverageMetric.BRANCH, CoveragePercentage.valueOf(50, 1));
        assertThat(converter.marshal(map)).isEqualTo("[Branch: 50/1]");

        map.put(CoverageMetric.LINE, CoveragePercentage.valueOf(3, 4));
        assertThat(converter.marshal(map)).isEqualTo("[Line: 3/4, Branch: 50/1]");
    }

    @Test
    void shouldConvertString2MetricMap() {
        MetricPercentageMapConverter converter = new MetricPercentageMapConverter();

        assertThat(converter.unmarshal(EMPTY)).isEmpty();
        CoveragePercentage first = CoveragePercentage.valueOf(50, 1);
        assertThat(converter.unmarshal("[Branch: 50/1]"))
                .containsExactly(entry(CoverageMetric.BRANCH, first));
        assertThat(converter.unmarshal("[Line: 3/4, Branch: 50/1]"))
                .containsExactly(entry(CoverageMetric.LINE, CoveragePercentage.valueOf(3, 4)), entry(CoverageMetric.BRANCH, first));
    }

    @Test
    void shouldConvertIntegerMap2String() {
        NavigableMap<Integer, Integer> map = new TreeMap<>();

        HitsMapConverter converter = new HitsMapConverter();

        assertThat(converter.marshal(map)).isEqualTo(EMPTY);

        map.put(10, 20);
        assertThat(converter.marshal(map)).isEqualTo("[10: 20]");

        map.put(15, 25);
        assertThat(converter.marshal(map)).isEqualTo("[10: 20, 15: 25]");
    }

    @Test
    void shouldConvertString2IntegerMap() {
        HitsMapConverter converter = new HitsMapConverter();

        assertThat(converter.unmarshal(EMPTY)).isEmpty();
        assertThat(converter.unmarshal("[15: 25]")).containsExactly(entry(15, 25));
        assertThat(converter.unmarshal("[15:25, 10: 20]")).containsExactly(entry(10, 20), entry(15, 25));
    }

    @Test
    void shouldConvertIntegerSet2String() {
        NavigableSet<Integer> set = new TreeSet<>();

        IntegerSetConverter converter = new IntegerSetConverter();

        assertThat(converter.marshal(set)).isEqualTo(EMPTY);

        set.add(10);
        assertThat(converter.marshal(set)).isEqualTo("[10]");

        set.add(15);
        assertThat(converter.marshal(set)).isEqualTo("[10, 15]");
    }

    @Test
    void shouldConvertString2IntegerSet() {
        IntegerSetConverter converter = new IntegerSetConverter();

        assertThat(converter.unmarshal(EMPTY)).isEmpty();
        assertThat(converter.unmarshal("[15]")).containsExactly(15);
        assertThat(converter.unmarshal("[15, 20]")).containsExactly(15, 20);
    }
}
