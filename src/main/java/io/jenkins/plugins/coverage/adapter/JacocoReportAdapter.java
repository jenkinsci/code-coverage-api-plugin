package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import java.util.List;

public final class JacocoReportAdapter extends JavaXMLCoverageReportAdapter {

    @DataBoundConstructor
    public JacocoReportAdapter(String path) {
        super(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXSL() {
        return "jacoco-to-standard.xsl";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXSD() {
        return null;
    }

    @Override
    public CoverageResult parseToResult(Document document, String reportName) throws CoverageException {
        return new JacocoCoverageParser(reportName).parse(document);
    }

    @Symbol(value = {"jacocoAdapter", "jacoco"})
    @Extension
    public static final class JacocoReportAdapterDescriptor extends JavaCoverageReportAdapterDescriptor {

        public JacocoReportAdapterDescriptor() {
            super(JacocoReportAdapter.class);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.JacocoReportAdapter_displayName();
        }

        @Override
        public List<CoverageElement> getCoverageElements() {
            List<CoverageElement> registerCoverageElements = super.getCoverageElements();
            registerCoverageElements.add(new CoverageElement("Instruction", 5));
            return registerCoverageElements;
        }
    }

    public static final class JacocoCoverageParser extends JavaCoverageParser {

        public JacocoCoverageParser(String reportName) {
            super(reportName);
        }

        @Override
        protected CoverageResult processElement(Element current, CoverageResult parentResult) {
            CoverageResult result = super.processElement(current, parentResult);

            if (getAttribute(current, "attr-mode", null) != null) {
                String lineCoveredAttr = getAttribute(current, "line-covered");
                String lineMissedAttr = getAttribute(current, "line-missed");

                String branchCoveredAttr = getAttribute(current, "br-covered");
                String branchMissedAttr = getAttribute(current, "br-missed");

                String instructionCoveredAttr = getAttribute(current, "instruction-covered");
                String instructionMissedAttr = getAttribute(current, "instruction-missed");

                if (StringUtils.isNumeric(lineCoveredAttr) && StringUtils.isNumeric(lineMissedAttr)) {
                    int covered = Integer.parseInt(lineCoveredAttr);
                    int missed = Integer.parseInt(lineMissedAttr);

                    result.updateCoverage(CoverageElement.LINE, Ratio.create(covered, covered + missed));
                }

                if (StringUtils.isNumeric(branchCoveredAttr) && StringUtils.isNumeric(branchMissedAttr)) {
                    int covered = Integer.parseInt(branchCoveredAttr);
                    int missed = Integer.parseInt(branchMissedAttr);

                    result.updateCoverage(CoverageElement.CONDITIONAL, Ratio.create(covered, covered + missed));
                }

                if(StringUtils.isNumeric(instructionCoveredAttr) && StringUtils.isNumeric(instructionMissedAttr)) {
                    int covered = Integer.parseInt(instructionCoveredAttr);
                    int missed = Integer.parseInt(instructionMissedAttr);

                    result.updateCoverage(CoverageElement.get("Instruction"), Ratio.create(covered, covered + missed));
                }

            }

            return result;
        }
    }
}
