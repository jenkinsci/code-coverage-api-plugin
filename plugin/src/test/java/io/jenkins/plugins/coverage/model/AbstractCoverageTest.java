package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.nio.file.Files;

import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
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
    static final Metric MODULE = Metric.MODULE;
    static final Metric PACKAGE = Metric.PACKAGE;
    static final Metric FILE = Metric.FILE;
    static final Metric CLASS = Metric.CLASS;
    static final Metric METHOD = Metric.METHOD;
    static final Metric LINE = Metric.LINE;
    static final Metric INSTRUCTION = Metric.INSTRUCTION;
    static final Metric BRANCH = Metric.BRANCH;

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
            return new JacocoParser().parse(Files.newBufferedReader(getResourceAsFile(fileName)));
        }
        catch (ParsingException | IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
