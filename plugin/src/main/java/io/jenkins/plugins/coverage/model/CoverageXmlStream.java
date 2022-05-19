package io.jenkins.plugins.coverage.model;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
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
     * {@link Converter} for a {@link SortedMap} of coverages per line. Stores the mapping in the condensed format
     * {@code key1: covered1/missed1, key2: covered2/missed2, ...}.
     */
    static final class LineMapConverter implements Converter {
        @SuppressWarnings({"PMD.NullAssignment", "unchecked"})
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof SortedMap ? marshal((NavigableMap<Integer, Coverage>) source) : null);
        }

        String marshal(final SortedMap<Integer, Coverage> source) {
            return source.entrySet()
                    .stream()
                    .map(e -> String.format("%d: %s", e.getKey(), e.getValue().serializeToString()))
                    .collect(Collectors.joining(", "));
        }

        @Override
        public NavigableMap<Integer, Coverage> unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return unmarshal(reader.getValue());
        }

        NavigableMap<Integer, Coverage> unmarshal(final String value) {
            NavigableMap<Integer, Coverage> map = new TreeMap<>();

            String cleanInput = StringUtils.deleteWhitespace(value);

            if (!StringUtils.containsAny(cleanInput, ',', ':')) {
                return map;
            }

            String[] entries = StringUtils.split(cleanInput, ",");
            for (String entry : entries) {
                if (StringUtils.contains(entry, ":")) {
                    addMapping(map, entry);
                }
            }
            return map;
        }

        private void addMapping(final NavigableMap<Integer, Coverage> map,
                final String entry) {
            try {
                Integer key = Integer.valueOf(StringUtils.substringBefore(entry, ':'));
                Coverage coverage = Coverage.valueOf(StringUtils.substringAfter(entry, ':'));
                map.put(key, coverage);
            }
            catch (IllegalArgumentException exception) {
                // ignore
            }
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == TreeMap.class;
        }
    }

    /**
     * {@link Converter} for a {@link SortedMap} of coverage percentages per metric. Stores the mapping in the condensed format
     * {@code metric1: numerator1/denominator1, metric2: numerator2/denominator2, ...}.
     */
    static final class MetricPercentageMapConverter implements Converter {
        @SuppressWarnings({"PMD.NullAssignment", "unchecked"})
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof SortedMap ? marshal((NavigableMap<CoverageMetric, CoveragePercentage>) source) : null);
        }

        String marshal(final NavigableMap<CoverageMetric, CoveragePercentage> source) {
            return source.entrySet()
                    .stream()
                    .map(e -> String.format("%s: %s", e.getKey().getName(), e.getValue().serializeToString()))
                    .collect(Collectors.joining(", "));
        }

        @Override
        public NavigableMap<CoverageMetric, CoveragePercentage> unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return unmarshal(reader.getValue());
        }

        NavigableMap<CoverageMetric, CoveragePercentage> unmarshal(final String value) {
            TreeMap<CoverageMetric, CoveragePercentage> map = new TreeMap<>();

            String cleanInput = StringUtils.deleteWhitespace(value);

            if (!StringUtils.containsAny(cleanInput, ',', ':')) {
                return map;
            }

            String[] entries = StringUtils.split(cleanInput, ",");
            for (String entry : entries) {
                if (StringUtils.contains(entry, ":")) {
                    addMapping(map, entry);
                }
            }
            return map;
        }

        private void addMapping(final NavigableMap<CoverageMetric, CoveragePercentage> map,
                final String entry) {
            try {
                CoverageMetric key = CoverageMetric.valueOf(StringUtils.substringBefore(entry, ':'));
                CoveragePercentage coverage = CoveragePercentage.valueOf(StringUtils.substringAfter(entry, ':'));
                map.put(key, coverage);
            }
            catch (IllegalArgumentException exception) {
                // ignore
            }
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == TreeMap.class;
        }
    }
}
