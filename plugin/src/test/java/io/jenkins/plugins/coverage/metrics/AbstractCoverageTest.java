package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.nio.file.Files;

import org.junitpioneer.jupiter.DefaultLocale;

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
}
