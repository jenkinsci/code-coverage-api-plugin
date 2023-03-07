package io.jenkins.plugins.coverage.metrics.source;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.jenkins.plugins.coverage.metrics.source.SourceCodePainter.PaintedNode;

/**
 * Converts a source code file into an HTML document. The HTML document contains the source code with highlighted
 * coverage information.
 *
 * @author Ullrich Hafner
 */
public class SourceToHtml {
    void paintSourceCodeWithCoverageInformation(final PaintedNode paint, final BufferedWriter output, final List<String> lines)
            throws IOException {
        for (int line = 0; line < lines.size(); line++) {
            paintLine(line + 1, lines.get(line), paint, output);
        }
    }

    // TODO rewrite using j2html and prism.js
    private void paintLine(final int line, final String content, final PaintedNode paint,
            final BufferedWriter output) throws IOException {
        if (paint.isPainted(line)) {
            int covered = paint.getCovered(line);
            int missed = paint.getMissed(line);

            int survived = paint.getSurvived(line);
            int killed = paint.getKilled(line);

            output.write("<tr class=\"" + selectColor(covered, missed, survived) + "\" "
                    + getTooltip(paint, missed, covered, survived, killed) + ">\n");
            output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");

            String display;

            if (survived + killed > 0) {
                display = String.format("%d/%d", killed, survived + killed);
            }
            else if (covered + missed > 1) {
                display = String.format("%d/%d", covered, covered + missed);
            }
            else {
                display = String.valueOf(covered);
            }

            output.write("<td class=\"hits\">" + display + "</td>\n");

        }
        else {
            output.write("<tr class=\"noCover\">\n");
            output.write("<td class=\"line\"><a name='" + line + "'>" + line + "</a></td>\n");
            output.write("<td class=\"hits\"></td>\n");
        }
        output.write("<td class=\"code\">"
                + content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ",
                        "&nbsp;")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "</td>\n");
        output.write("</tr>\n");
    }

    private String selectColor(final int covered, final int missed, final int survived) {
        // TODO: what colors should be used for the different cases: Mutations or Branches?
        if (covered == 0) {
            return "coverNone";
        }
        else if (missed == 0 && survived == 0) {
            return "coverFull";
        }
        else {
            return "coverPart";
        }
    }

    private String getTooltip(final PaintedNode paint,
            final int missed, final int covered, final int survived, final int killed) {
        var tooltip = getTooltipValue(paint, missed, covered, survived, killed);
        if (StringUtils.isBlank(tooltip)) {
            return StringUtils.EMPTY;
        }
        return "tooltip=\"" + tooltip + "\"";
    }

    // TODO: Extract into classes so that we can paint the mutations as well
    private String getTooltipValue(final PaintedNode paint,
            final int missed, final int covered, final int survived, final int killed) {

        if (survived + killed > 1) {
            return String.format("Mutations survived: %d, mutations killed: %d", survived, killed);
        }
        if (survived == 1) {
            return "One survived mutation";
        }
        if (killed == 1) {
            return "One killed mutation";
        }

        if (covered + missed > 1) {
            if (missed == 0) {
                return "All branches covered";
            }
            return String.format("Partially covered, branch coverage: %d/%d", covered, covered + missed);
        }
        else if (covered == 1) {
            return "Covered at least once";
        }
        else {
            return "Not covered";
        }
    }
}
