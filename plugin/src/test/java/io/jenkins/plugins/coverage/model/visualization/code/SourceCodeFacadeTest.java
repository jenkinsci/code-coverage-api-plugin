package io.jenkins.plugins.coverage.model.visualization.code;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.ResourceTest;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;
import io.jenkins.plugins.coverage.model.FileCoverageNode;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link SourceCodeFacade}.
 *
 * @author Florian Orendi
 */
class SourceCodeFacadeTest extends ResourceTest {

    /**
     * The original sourcecode view which shows the whole file.
     */
    private static final String SOURCECODE = "SourcecodeTest.html";
    /**
     * The sourcecode view for the change coverage.
     */
    private static final String SOURCECODE_CC = "SourcecodeTestCC.html";
    /**
     * The sourcecode view for the indirect coverage changes.
     */
    private static final String SOURCECODE_ICC = "SourcecodeTestICC.html";

    /**
     * The lines which contain code changes.
     */
    private static final SortedSet<Integer> CODE_CHANGES = new TreeSet<>();
    /**
     * The used indirect coverage changes.
     */
    private static final SortedMap<Integer, Integer> INDIRECT_COVERAGE_CHANGES = new TreeMap<>();
    /**
     * A dummy coverage per line mapping.
     */
    private static final SortedMap<Integer, Coverage> COVERAGE_PER_LINE = new TreeMap<>();

    @BeforeAll
    static void init() {
        CODE_CHANGES.addAll(Arrays.asList(10, 11, 12, 16, 17, 18, 19));
        INDIRECT_COVERAGE_CHANGES.put(6, -1);
        INDIRECT_COVERAGE_CHANGES.put(7, -1);
        INDIRECT_COVERAGE_CHANGES.put(14, 1);
        INDIRECT_COVERAGE_CHANGES.put(15, 1);
        for (int i = 1; i <= 25; i++) {
            COVERAGE_PER_LINE.put(i, CoverageBuilder.NO_COVERAGE);
        }
    }

    @Test
    void shouldCalculateSourcecodeForChangeCoverage() throws IOException {
        SourceCodeFacade sourceCodeFacade = createSourceCodeFacade();
        String originalHtml = readHtml(SOURCECODE);
        FileCoverageNode node = createFileCoverageNode();

        String requiredHtml = Jsoup.parse(readHtml(SOURCECODE_CC), Parser.xmlParser()).html();

        String changeCoverageHtml = sourceCodeFacade.calculateChangeCoverageSourceCode(originalHtml, node);
        assertThat(changeCoverageHtml).isEqualTo(requiredHtml);
    }

    @Test
    void shouldCalculateSourcecodeForIndirectCoverageChanges() throws IOException {
        SourceCodeFacade sourceCodeFacade = createSourceCodeFacade();
        String originalHtml = readHtml(SOURCECODE);
        FileCoverageNode node = createFileCoverageNode();

        String requiredHtml = Jsoup.parse(readHtml(SOURCECODE_ICC), Parser.xmlParser()).html();

        String changeCoverageHtml = sourceCodeFacade.calculateIndirectCoverageChangesSourceCode(originalHtml, node);
        assertThat(changeCoverageHtml).isEqualTo(requiredHtml);
    }

    /**
     * Creates an instance of {@link SourceCodeFacade}.
     *
     * @return the created instance
     */
    private SourceCodeFacade createSourceCodeFacade() {
        return new SourceCodeFacade();
    }

    /**
     * Creates a {@link FileCoverageNode} which contains {@link #CODE_CHANGES}, {@link #INDIRECT_COVERAGE_CHANGES} and
     * {@link #COVERAGE_PER_LINE}.
     *
     * @return the created node
     */
    private FileCoverageNode createFileCoverageNode() {
        FileCoverageNode fileCoverageNode = new FileCoverageNode("", "");
        fileCoverageNode.setCoveragePerLine(COVERAGE_PER_LINE);
        fileCoverageNode.setChangedCodeLines(CODE_CHANGES);
        fileCoverageNode.setIndirectCoverageChanges(INDIRECT_COVERAGE_CHANGES);
        return fileCoverageNode;
    }

    /**
     * Reads a sourcecode HTML file for testing.
     *
     * @param name
     *         The name of the file
     *
     * @return the file content
     * @throws IOException
     *         if reading failed
     */
    private String readHtml(final String name) throws IOException {
        return new String(Files.readAllBytes(getResourceAsFile(name)));
    }
}
