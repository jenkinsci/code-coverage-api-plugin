package io.jenkins.plugins.coverage.metrics.visualization.code;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.util.ResourceTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link SourceCodeFacade}.
 *
 * @author Florian Orendi
 */
class SourceCodeFacadeTest extends ResourceTest {
    private static final String WHOLE_SOURCE_CODE = "SourcecodeTest.html";
    private static final String CHANGE_COVERAGE_SOURCE_CODE = "SourcecodeTestCC.html";
    private static final String INDIRECT_COVERAGE_SOURCE_CODE = "SourcecodeTestICC.html";

    @Test
    void shouldCalculateSourcecodeForChangeCoverage() throws IOException {
        SourceCodeFacade sourceCodeFacade = createSourceCodeFacade();
        String originalHtml = readHtml(WHOLE_SOURCE_CODE);
        FileNode node = createFileCoverageNode();

        String requiredHtml = Jsoup.parse(readHtml(CHANGE_COVERAGE_SOURCE_CODE), Parser.xmlParser()).html();

        String changeCoverageHtml = sourceCodeFacade.calculateChangeCoverageSourceCode(originalHtml, node);
        assertThat(changeCoverageHtml).isEqualTo(requiredHtml);
    }

    @Test
    void shouldCalculateSourcecodeForIndirectCoverageChanges() throws IOException {
        SourceCodeFacade sourceCodeFacade = createSourceCodeFacade();
        String originalHtml = readHtml(WHOLE_SOURCE_CODE);
        FileNode node = createFileCoverageNode();

        String requiredHtml = Jsoup.parse(readHtml(INDIRECT_COVERAGE_SOURCE_CODE), Parser.xmlParser()).html();

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
     * Creates a {@link FileNode} which contains {@link #CODE_CHANGES}, {@link #INDIRECT_COVERAGE_CHANGES} and
     * {@link #COVERAGE_PER_LINE}.
     *
     * @return the created node
     */
    private FileNode createFileCoverageNode() {
        FileNode file = new FileNode("");
        List<Integer> lines = Arrays.asList(10, 11, 12, 16, 17, 18, 19);
        for (Integer line : lines) {
            file.addChangedCodeLine(line);
        }
        file.addIndirectCoverageChange(6, -1);
        file.addIndirectCoverageChange(7, -1);
        file.addIndirectCoverageChange(14, 1);
        file.addIndirectCoverageChange(15, 1);
        for (int i = 1; i <= 25; i++) {
            file.addCounters(i, 1, 0);
        }
        return file;
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
