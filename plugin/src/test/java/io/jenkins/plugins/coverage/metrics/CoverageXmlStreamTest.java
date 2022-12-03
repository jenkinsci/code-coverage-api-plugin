package io.jenkins.plugins.coverage.metrics;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.xmlunit.builder.Input;

import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.metric.parser.JacocoParser;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;

import hudson.XmlFile;
import hudson.model.FreeStyleBuild;
import hudson.util.XStream2;

import io.jenkins.plugins.coverage.model.Assertions;

import static org.mockito.Mockito.*;
import static org.xmlunit.assertj.XmlAssert.*;

/**
 * Tests the class {@link CoverageXmlStream}.
 *
 * @author Ullrich Hafner
 */
class CoverageXmlStreamTest extends SerializableTest<Node> {
    private static final String ACTION_QUALIFIED_NAME = "io.jenkins.plugins.coverage.metrics.CoverageBuildAction";

    @Override
    protected Node createSerializable() {
        return new JacocoParser().parse(new InputStreamReader(asInputStream("jacoco-codingstyle.xml")));
    }

    @Test
    void shouldSaveAndRestoreTree() throws IOException {
        CoverageXmlStream xmlStream = new CoverageXmlStream();

        Path saved = createTempFile();
        Node convertedNode = createSerializable();

        xmlStream.write(saved, convertedNode);
        Node restored = xmlStream.read(saved);

        Assertions.assertThat(restored).usingRecursiveComparison().isEqualTo(convertedNode);

        System.out.println(new String(Files.readAllBytes(saved)));

        var xml = Input.from(saved);
        assertThat(xml).nodesByXPath("//module/values/*")
                .hasSize(4).extractingText()
                .containsExactly("INSTRUCTION: 1260/1350",
                        "BRANCH: 109/116",
                        "LINE: 294/323",
                        "COMPLEXITY: 160");
        assertThat(xml).nodesByXPath("//module/values/*")
                .hasSize(4).extractingText()
                .containsExactly("INSTRUCTION: 1260/1350",
                        "BRANCH: 109/116",
                        "LINE: 294/323",
                        "COMPLEXITY: 160");
        assertThat(xml).nodesByXPath("//file[./name = 'TreeStringBuilder.java']/values/*")
                .hasSize(4).extractingText()
                .containsExactly("INSTRUCTION: 229/233", "BRANCH: 17/18", "LINE: 51/53", "COMPLEXITY: 23");
        assertThat(xml).nodesByXPath("//file[./name = 'TreeStringBuilder.java']/coveredPerLine")
                .hasSize(1).extractingText()
                .containsExactly("[19: 1, 20: 1, 31: 1, 43: 1, 50: 1, 51: 1, 54: 1, 57: 1, 61: 0, 62: 0, 70: 1, 72: 1, 73: 1, 74: 1, 85: 2, 86: 1, 89: 1, 90: 2, 91: 1, 92: 2, 93: 2, 95: 1, 96: 1, 97: 1, 100: 1, 101: 1, 103: 1, 106: 1, 109: 1, 112: 1, 113: 1, 114: 1, 115: 1, 117: 1, 125: 2, 126: 1, 128: 1, 140: 1, 142: 1, 143: 1, 144: 1, 146: 1, 160: 1, 162: 2, 163: 2, 164: 1, 167: 1, 177: 1, 178: 2, 179: 1, 180: 1, 181: 1, 184: 1]");
        assertThat(xml).nodesByXPath("//file[./name = 'TreeStringBuilder.java']/missedPerLine")
                .hasSize(1).extractingText()
                .containsExactly("[19: 0, 20: 0, 31: 0, 43: 0, 50: 0, 51: 0, 54: 0, 57: 0, 61: 1, 62: 1, 70: 0, 72: 0, 73: 0, 74: 0, 85: 0, 86: 0, 89: 0, 90: 0, 91: 0, 92: 0, 93: 0, 95: 0, 96: 0, 97: 0, 100: 0, 101: 0, 103: 0, 106: 0, 109: 0, 112: 0, 113: 1, 114: 0, 115: 0, 117: 0, 125: 0, 126: 0, 128: 0, 140: 0, 142: 0, 143: 0, 144: 0, 146: 0, 160: 0, 162: 0, 163: 0, 164: 0, 167: 0, 177: 0, 178: 0, 179: 0, 180: 0, 181: 0, 184: 0]");
    }

    @Test
    void shouldStoreActionCompactly() throws IOException {
        TestXmlStream xmlStream = new TestXmlStream();
        Path saved = createTempFile();
        xmlStream.read(saved);

        var file = new XmlFile(xmlStream.getStream(), saved.toFile());
        file.write(createAction());

        System.out.println(new String(Files.readAllBytes(saved)));
        assertThat(Input.from(saved)).nodesByXPath("//" + ACTION_QUALIFIED_NAME + "/projectValues/*")
                .hasSize(11).extractingText()
                .containsExactly("MODULE: 1/1",
                        "PACKAGE: 1/1",
                        "FILE: 7/10",
                        "CLASS: 15/18",
                        "METHOD: 97/102",
                        "LINE: 294/323",
                        "INSTRUCTION: 1260/1350",
                        "BRANCH: 109/116",
                        "COMPLEXITY: 160",
                        "COMPLEXITY_DENSITY: 160/323",
                        "LOC: 323");

        assertThat(Input.from(saved)).nodesByXPath("//" + ACTION_QUALIFIED_NAME + "/projectValues/coverage")
                .hasSize(8).extractingText()
                .containsExactly("MODULE: 1/1",
                        "PACKAGE: 1/1",
                        "FILE: 7/10",
                        "CLASS: 15/18",
                        "METHOD: 97/102",
                        "LINE: 294/323",
                        "INSTRUCTION: 1260/1350",
                        "BRANCH: 109/116");

        var action = file.read();
        assertThat(action).isNotNull().isInstanceOfSatisfying(CoverageBuildAction.class, a -> {
            Assertions.assertThat(a.getAllValues(Baseline.PROJECT).stream()
                            .map(Value::serialize)
                            .collect(Collectors.toList()))
                    .containsExactly(
                            "MODULE: 1/1",
                            "PACKAGE: 1/1",
                            "FILE: 7/10",
                            "CLASS: 15/18",
                            "METHOD: 97/102",
                            "LINE: 294/323",
                            "INSTRUCTION: 1260/1350",
                            "BRANCH: 109/116",
                            "COMPLEXITY: 160",
                            "COMPLEXITY_DENSITY: 160/323",
                            "LOC: 323"
                    );
        });

    }

    CoverageBuildAction createAction() {
        var tree = createSerializable();

        return new CoverageBuildAction(mock(FreeStyleBuild.class), new FilteredLog("Test"),
                tree, QualityGateStatus.INACTIVE, "-",
                new TreeMap<>(), List.of(),
                new TreeMap<>(), List.of(), false);
    }

    private static class TestXmlStream extends CoverageXmlStream {
        private XStream2 xStream;

        @Override
        protected void configureXStream(final XStream2 xStream2) {
            super.configureXStream(xStream2);

            this.xStream = xStream2;
        }

        public XStream2 getStream() {
            return xStream;
        }
    }
}
