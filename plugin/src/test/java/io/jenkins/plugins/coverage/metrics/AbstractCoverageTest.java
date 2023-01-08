package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.metric.parser.JacocoParser;
import edu.hm.hafner.util.ResourceTest;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * Base class for coverage tests that work on real coverage reports.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
public abstract class AbstractCoverageTest extends ResourceTest {
    static final String JACOCO_CODING_STYLE_FILE = "jacoco-codingstyle.xml";

    /**
     * Reads and parses a JaCoCo coverage report.
     *
     * @param fileName
     *         the name of the coverage report file
     *
     * @return the parsed coverage tree
     */
    public Node readJacocoResult(final String fileName) {
        try {
            var node = new JacocoParser().parse(Files.newBufferedReader(getResourceAsFile(fileName)));
            node.splitPackages();
            return node;
        }
        catch (ParsingException | IOException exception) {
            throw new AssertionError(exception);
        }
    }

    static CoverageStatistics createStatistics() {
        return new CoverageStatistics(fillValues(), fillDeltas(),
                fillValues(), fillDeltas(),
                fillValues(), fillDeltas());
    }

    private static List<Value> fillValues() {
        var builder = new CoverageBuilder();
        return List.of(builder.setMetric(Metric.FILE).setCovered(3).setMissed(1).build(),
                builder.setMetric(Metric.LINE).setCovered(2).setMissed(2).build());
    }

    private static NavigableMap<Metric, Fraction> fillDeltas() {
        final NavigableMap<Metric, Fraction> deltaMapping = new TreeMap<>();
        deltaMapping.put(Metric.FILE, Fraction.getFraction(-10, 100));
        deltaMapping.put(Metric.LINE, Fraction.getFraction(5, 100));
        return deltaMapping;
    }
}
