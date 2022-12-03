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
    void shouldSaveAndRestoreTree() {
        CoverageXmlStream xmlStream = new CoverageXmlStream();

        Path saved = createTempFile();
        Node convertedNode = createSerializable();

        xmlStream.write(saved, convertedNode);
        Node restored = xmlStream.read(saved);
        Assertions.assertThat(restored).usingRecursiveComparison().isEqualTo(convertedNode);
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
