package io.jenkins.plugins.coverage.metrics.source;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj.XmlAssert;

import edu.hm.hafner.coverage.parser.JacocoParser;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;

import static org.assertj.core.api.Assertions.*;

class CoverageSourcePrinterTest extends AbstractCoverageTest {
    static final String CLASS = "class";
    static final String RENDERED_CODE = "                    "
            + "for (int line = 0; line < lines.size(); line++) {";

    @Test
    void shouldRenderLinesWithVariousCoverages() {
        var tree = readResult("../steps/jacoco-codingstyle.xml", new JacocoParser());

        var file = new CoverageSourcePrinter(tree.findFile("TreeStringBuilder.java").get());

        assertThat(file.getColorClass(0)).isEqualTo(CoverageSourcePrinter.NO_COVERAGE);
        assertThat(file.getSummaryColumn(0)).isEqualTo("0");
        assertThat(file.getTooltip(0)).isEqualTo("Not covered");

        assertThat(file.getColorClass(113)).isEqualTo(CoverageSourcePrinter.PARTIAL_COVERAGE);
        assertThat(file.getSummaryColumn(113)).isEqualTo("1/2");
        assertThat(file.getTooltip(113)).isEqualToIgnoringWhitespace("Partially covered, branch coverage: 1/2");

        assertThat(file.getColorClass(61)).isEqualTo(CoverageSourcePrinter.NO_COVERAGE);
        assertThat(file.getSummaryColumn(61)).isEqualTo("0");
        assertThat(file.getTooltip(61)).isEqualTo("Not covered");

        assertThat(file.getColorClass(19)).isEqualTo(CoverageSourcePrinter.FULL_COVERAGE);
        assertThat(file.getSummaryColumn(19)).isEqualTo("1");
        assertThat(file.getTooltip(19)).isEqualTo("Covered at least once");

        var anotherFile = new CoverageSourcePrinter(tree.findFile("StringContainsUtils.java").get());

        assertThat(anotherFile.getColorClass(43)).isEqualTo(CoverageSourcePrinter.FULL_COVERAGE);
        assertThat(anotherFile.getSummaryColumn(43)).isEqualTo("2/2");
        assertThat(anotherFile.getTooltip(43)).isEqualTo("All branches covered");
    }

    @Test
    void shouldRenderNoBrnachCoverage() {
        var tree = readResult("../steps/jacoco-analysis-model.xml", new JacocoParser());

        var file = new CoverageSourcePrinter(tree.findFile("LineRangeList.java").get());

        assertThat(file.getSummaryColumn(265)).isEqualTo("0/2");
        assertThat(file.getTooltip(265)).isEqualTo("No branches covered");
        assertThat(file.getColorClass(265)).isEqualTo(CoverageSourcePrinter.NO_COVERAGE);
    }

    @Test
    void shouldRenderWholeLine() {
        var tree = readResult("../steps/jacoco-codingstyle.xml", new JacocoParser());

        var file = new CoverageSourcePrinter(tree.findFile("TreeStringBuilder.java").get());

        var renderedLine = file.renderLine(61,
                "                    for (int line = 0; line < lines.size(); line++) {\n");

        XmlAssert.assertThat(renderedLine)
                .nodesByXPath("/tr").exist().hasSize(1)
                .singleElement()
                .hasAttribute(CLASS, CoverageSourcePrinter.NO_COVERAGE)
                .hasAttribute("data-html-tooltip", "Not covered");
        var assertThatColumns = XmlAssert.assertThat(renderedLine).nodesByXPath("/tr/td").exist().hasSize(3);
        assertThatColumns.extractingAttribute("class").containsExactly("line", "hits", "code");

        XmlAssert.assertThat(renderedLine).nodesByXPath("/tr/td[1]/a").exist().hasSize(1)
                .extractingAttribute("name").containsExactly("61");
        XmlAssert.assertThat(renderedLine).nodesByXPath("/tr/td[2]")
                .extractingText().containsExactly("0");
        XmlAssert.assertThat(renderedLine).nodesByXPath("/tr/td[3]")
                .extractingText().containsExactly(RENDERED_CODE);

        var skippedLine = file.renderLine(1, "package io.jenkins.plugins.coverage.metrics.source;");

        var assertThatSkippedColumns = XmlAssert.assertThat(renderedLine).nodesByXPath("/tr/td").exist().hasSize(3);
        assertThatSkippedColumns.extractingAttribute("class").containsExactly("line", "hits", "code");

        XmlAssert.assertThat(skippedLine)
                .nodesByXPath("/tr").exist().hasSize(1)
                .singleElement()
                .hasAttribute(CLASS, CoverageSourcePrinter.UNDEFINED)
                .doesNotHaveAttribute("data-html-tooltip");

        XmlAssert.assertThat(skippedLine).nodesByXPath("/tr/td[1]/a").exist().hasSize(1)
                .extractingAttribute("name").containsExactly("1");
        XmlAssert.assertThat(skippedLine).nodesByXPath("/tr/td[2]")
                .extractingText().containsExactly(StringUtils.EMPTY);
        XmlAssert.assertThat(skippedLine).nodesByXPath("/tr/td[3]")
                .extractingText().containsExactly("package io.jenkins.plugins.coverage.metrics.source;");

    }
}
