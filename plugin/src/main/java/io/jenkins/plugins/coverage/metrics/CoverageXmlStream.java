package io.jenkins.plugins.coverage.metrics;

import java.util.function.Function;

import org.apache.commons.lang3.math.Fraction;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.ContainerNode;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.FractionValue;
import edu.hm.hafner.metric.LinesOfCode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.MutationValue;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
import edu.hm.hafner.metric.Value;

import hudson.util.XStream2;

import io.jenkins.plugins.util.AbstractXmlStream;

/**
 * Configures the XML stream for the coverage tree, which consists of {@link Node}s.
 */
class CoverageXmlStream extends AbstractXmlStream<Node> {
    /**
     * Creates an XML stream for {@link Node}.
     */
    CoverageXmlStream() {
        super(Node.class);
    }

    @Override
    protected void configureXStream(final XStream2 xStream) {
        xStream.alias("container", ContainerNode.class);
        xStream.alias("module", ModuleNode.class);
        xStream.alias("package", PackageNode.class);
        xStream.alias("file", FileNode.class);
        xStream.alias("class", ClassNode.class);
        xStream.alias("method", MethodNode.class);

        xStream.alias("metric", Metric.class);

        xStream.alias("coverage", Coverage.class);
        xStream.addImmutableType(Coverage.class, false);
        xStream.alias("mutation", MutationValue.class);
        xStream.addImmutableType(MutationValue.class, false);
        xStream.alias("complexity", CyclomaticComplexity.class);
        xStream.addImmutableType(CyclomaticComplexity.class, false);
        xStream.alias("loc", LinesOfCode.class);
        xStream.addImmutableType(LinesOfCode.class, false);
        xStream.alias("fraction", FractionValue.class);
        xStream.addImmutableType(FractionValue.class, false);

        xStream.registerConverter(new FractionConverter());
        xStream.registerConverter(new SimpleConverter<>(Value.class, Value::serialize, Value::valueOf));
        xStream.registerConverter(new SimpleConverter<>(Metric.class, Metric::name, Metric::valueOf));

        /* FIXME: restore converters
        xStream.addImmutableType(CoveragePercentageConverter.class, false);
        xStream.registerConverter(new CoverageConverter(xStream));
        xStream.registerConverter(new CoveragePercentageConverter());
        xStream.registerLocalConverter(FileNode.class, "coveragePerLine", new LineMapConverter());
        xStream.registerLocalConverter(FileNode.class, "fileCoverageDelta", new MetricPercentageMapConverter());
        xStream.registerLocalConverter(FileNode.class, "indirectCoverageChanges", new HitsMapConverter());
        xStream.registerLocalConverter(FileNode.class, "changedCodeLines", new IntegerSetConverter());
         */
    }

    @Override
    protected Node createDefaultValue() {
        return new ModuleNode("Empty");
    }

    /**
     * {@link Converter} for {@link Fraction} instances so that only the values will be serialized. After reading the
     * values back from the stream, the string representation will be converted to an actual instance again.
     */
    static final class FractionConverter implements Converter {
        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(source instanceof Fraction ? ((Fraction) source).toProperString() : null);
        }

        @Override
        public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return Fraction.getFraction(reader.getValue());
        }

        @Override
        public boolean canConvert(final Class type) {
            return type == Fraction.class;
        }
    }

    /**
     * {@link Converter} for {@link Coverage} instances so that only the values will be serialized. After reading the
     * values back from the stream, the string representation will be converted to an actual instance again.
     *
     * @param <T> type of the objects that will be marshalled and unmarshalled
     */
    public static class SimpleConverter<T> implements Converter {
        private final Class<T> type;
        private final Function<T, String> marshaller;
        private final Function<String, Object> unmarshaller;

        protected SimpleConverter(final Class<T> type, final Function<T, String> marshaller, final Function<String, Object> unmarshaller) {
            this.type = type;
            this.marshaller = marshaller;
            this.unmarshaller = unmarshaller;
        }

        @SuppressWarnings("PMD.NullAssignment")
        @Override
        public void marshal(final Object source, final HierarchicalStreamWriter writer,
                final MarshallingContext context) {
            writer.setValue(type.isInstance(source) ? marshaller.apply(type.cast(source)) : null);
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return unmarshaller.apply(reader.getValue());
        }

        @Override
        public final boolean canConvert(final Class clazz) {
            return type.isAssignableFrom(clazz);
        }
    }
}
