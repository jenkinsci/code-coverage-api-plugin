package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.ContainerNode;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.LinesOfCode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.MutationValue;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;

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
        xStream.alias("coverage", Coverage.class);
        xStream.alias("mutation", MutationValue.class);
        xStream.alias("complexity", CyclomaticComplexity.class);
        xStream.alias("loc", LinesOfCode.class);
        xStream.alias("percentage", CoveragePercentage.class);
        xStream.addImmutableType(Metric.class, false);
        xStream.addImmutableType(Coverage.class, false);
        xStream.addImmutableType(LinesOfCode.class, false);
        xStream.addImmutableType(CyclomaticComplexity.class, false);
        /* FIXME: restore converters
        xStream.addImmutableType(CoveragePercentageConverter.class, false);
        xStream.registerConverter(new CoverageMetricConverter());
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
}
