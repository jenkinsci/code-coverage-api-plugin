package io.jenkins.plugins.coverage.model;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import hudson.util.XStream2;

import io.jenkins.plugins.util.AbstractXmlStream;

/**
 * Configures the XML stream for the coverage tree, which consists of {@link CoverageNode}s.
 */
class CoverageXmlStream extends AbstractXmlStream<CoverageNode> {
    private static final Collector<CharSequence, ?, String> ARRAY_JOINER = Collectors.joining(", ", "[", "]");

    private static String[] toArray(final String value) {
        String cleanInput = StringUtils.removeEnd(StringUtils.removeStart(StringUtils.deleteWhitespace(value), "["), "]");

        return StringUtils.split(cleanInput, ",");
    }

    /**
     * Creates an XML stream for {@link CoverageNode}.
     */
    CoverageXmlStream() {
        super(CoverageNode.class);
    }

    @Override
    protected void configureXStream(final XStream2 xStream) {
        xStream.alias("node", CoverageNode.class);
        xStream.alias("package", PackageCoverageNode.class);
        xStream.alias("file", FileCoverageNode.class);
        xStream.alias("method", MethodCoverageNode.class);
        xStream.alias("leaf", CoverageLeaf.class);
        xStream.alias("coverage", Coverage.class);
        xStream.alias("percentage", CoveragePercentage.class);
        xStream.addImmutableType(CoverageMetric.class, false);
        xStream.addImmutableType(Coverage.class, false);
        xStream.addImmutableType(CoveragePercentageConverter.class, false);
        xStream.registerConverter(new CoverageMetricConverter());
        xStream.registerConverter(new CoverageConverter());
        xStream.registerConverter(new CoveragePercentageConverter());
        xStream.registerLocalConverter(FileCoverageNode.class, "coveragePerLine", new LineMapConverter());
        xStream.registerLocalConverter(FileCoverageNode.class, "fileCoverageDelta", new MetricPercentageMapConverter());
        xStream.registerLocalConverter(FileCoverageNode.class, "indirectCoverageChanges", new HitsMapConverter());
        xStream.registerLocalConverter(FileCoverageNode.class, "changedCodeLines", new IntegerSetConverter());
    }

    @Override
    protected CoverageNode createDefaultValue() {
        return new CoverageNode(CoverageMetric.MODULE, "Empty");
    }

    /**
     * {@link Converter} for {@link CoverageMetric} instances so that only the string name will be serialized. After
     * reading the values back from the stream, the string representation will be converted to an actual instance
     * again.
     */
    private static final class CoverageMetricConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof CoverageMetric ? ((CoverageMetric) source).getName() : null);
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return CoverageMetric.valueOf(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == CoverageMetric.class;
        }
    }

    /**
     * {@link Converter} for {@link Coverage} instances so that only the values will be serialized. After reading the
     * values back from the stream, the string representation will be converted to an actual instance again.
     */
    private static final class CoverageConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof Coverage ? ((Coverage) source).serializeToString() : null);
        }

        @Override
        public Coverage unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return Coverage.valueOf(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == Coverage.class;
        }
    }

    /**
     * {@link Converter} for {@link CoveragePercentage} instances so that only the values will be serialized. After
     * reading the values back from the stream, the string representation will be converted to an actual instance
     * again.
     */
    private static final class CoveragePercentageConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(
                    source instanceof CoveragePercentage ? ((CoveragePercentage) source).serializeToString() : null);
        }

        @Override
        public CoveragePercentage unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return CoveragePercentage.valueOf(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == CoveragePercentage.class;
        }
    }

    /**
     * {@link Converter} for a {@link TreeSet} of integers that serializes just the values. After
     * reading the values back from the stream, the string representation will be converted to an actual instance
     * again.
     */
    static final class IntegerSetConverter implements Converter {
        @SuppressWarnings({"PMD.NullAssignment", "unchecked"})
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof TreeSet ? marshal((TreeSet<Integer>) source) : null);
        }

        String marshal(final Set<Integer> lines) {
            return lines.stream().map(String::valueOf).collect(ARRAY_JOINER);
        }

        @Override
        public NavigableSet<Integer> unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return unmarshal(reader.getValue());
        }

        NavigableSet<Integer> unmarshal(final String value) {
            return Arrays.stream(toArray(value)).map(Integer::valueOf).collect(Collectors.toCollection(TreeSet::new));
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == TreeSet.class;
        }
    }

    /**
     * {@link Converter} base class for {@link TreeMap} instance. Stores the mappings in a condensed format
     * {@code key1: value1, key2: value2, ...}.
     *
     * @param <K>
     *         the type of keys maintained by this map
     * @param <V>
     *         the type of mapped values
     */
    abstract static class TreeMapConverter<K extends Comparable<K>, V> implements Converter {
        @Override
        @SuppressWarnings({"PMD.NullAssignment", "unchecked"})
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof NavigableMap ? marshal((NavigableMap<K, V>) source) : null);
        }

        String marshal(final SortedMap<K, V> source) {
            return source.entrySet()
                    .stream()
                    .map(createMapEntry())
                    .collect(ARRAY_JOINER);
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == TreeMap.class;
        }

        @Override
        public NavigableMap<K, V> unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return unmarshal(reader.getValue());
        }

        NavigableMap<K, V> unmarshal(final String value) {
            NavigableMap<K, V> map = new TreeMap<>();

            for (String marshalledValue : toArray(value)) {
                if (StringUtils.contains(marshalledValue, ":")) {
                    try {
                        Entry<K, V> entry = createMapping(
                                StringUtils.substringBefore(marshalledValue, ':'),
                                StringUtils.substringAfter(marshalledValue, ':'));
                        map.put(entry.getKey(), entry.getValue());
                    }
                    catch (IllegalArgumentException exception) {
                        // ignore
                    }
                }
            }
            return map;
        }

        protected abstract Function<Entry<K, V>, String> createMapEntry();

        protected abstract Map.Entry<K, V> createMapping(String key, String value);

        protected SimpleEntry<K, V> entry(final K key, final V value) {
            return new SimpleEntry<>(key, value);
        }
    }

    /**
     * {@link Converter} for a {@link SortedMap} of coverages per line. Stores the mapping in the condensed format
     * {@code key1: covered1/missed1, key2: covered2/missed2, ...}.
     */
    static final class LineMapConverter extends TreeMapConverter<Integer, Coverage> {
        @Override
        protected Function<Entry<Integer, Coverage>, String> createMapEntry() {
            return e -> String.format("%d: %s", e.getKey(), e.getValue().serializeToString());
        }

        @Override
        protected Entry<Integer, Coverage> createMapping(final String key, final String value) {
            return entry(Integer.valueOf(key), Coverage.valueOf(value));
        }
    }

    /**
     * {@link Converter} for a {@link SortedMap} of coverage percentages per metric. Stores the mapping in the condensed
     * format {@code metric1: numerator1/denominator1, metric2: numerator2/denominator2, ...}.
     */
    static final class MetricPercentageMapConverter extends TreeMapConverter<CoverageMetric, CoveragePercentage> {
        @Override
        protected Function<Entry<CoverageMetric, CoveragePercentage>, String> createMapEntry() {
            return e -> String.format("%s: %s", e.getKey().getName(), e.getValue().serializeToString());
        }

        @Override
        protected Entry<CoverageMetric, CoveragePercentage> createMapping(final String key, final String value) {
            return entry(CoverageMetric.valueOf(key), CoveragePercentage.valueOf(value));
        }
    }

    /**
     * {@link Converter} for a {@link SortedMap} of coverage hits per line. Stores the mapping in the condensed
     * format {@code line1: hits1, line2: hits2, ...}.
     */
    static final class HitsMapConverter extends TreeMapConverter<Integer, Integer> {
        @Override
        protected Function<Entry<Integer, Integer>, String> createMapEntry() {
            return e -> String.format("%d: %d", e.getKey(), e.getValue());
        }

        @Override
        protected Entry<Integer, Integer> createMapping(final String key, final String value) {
            return entry(Integer.valueOf(key), Integer.valueOf(value));
        }
    }
}
