package io.jenkins.plugins.coverage.adapter;

import java.util.List;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.Symbol;
import hudson.Extension;

import io.jenkins.plugins.coverage.adapter.parser.JavaCoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * Reads JaCoCo results.
 */
public final class JacocoReportAdapter extends JavaXMLCoverageReportAdapter {
    @DataBoundConstructor
    public JacocoReportAdapter(final String path) {
        super(path);
    }

    @Override
    public String getXSL() {
        return "jacoco-to-standard.xsl";
    }

    @Override
    public String getXSD() {
        return null;
    }

    @Override
    public CoverageResult parseToResult(final Document document, final String reportName) throws CoverageException {
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
        public JacocoCoverageParser(final String reportName) {
            super(reportName);
        }

        @Override
        protected CoverageResult processElement(final Element current, final CoverageResult parentResult) {
            CoverageResult result = super.processElement(current, parentResult);
            if (result == null) {
                return null;
            }
            if (JavaCoverageReportAdapterDescriptor.PACKAGE.equals(result.getElement())) {
                result.setName(fixPackageName(result.getName()));
            }
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

                if (StringUtils.isNumeric(instructionCoveredAttr) && StringUtils.isNumeric(instructionMissedAttr)) {
                    int covered = Integer.parseInt(instructionCoveredAttr);
                    int missed = Integer.parseInt(instructionMissedAttr);

                    result.updateCoverage(CoverageElement.get("Instruction"), Ratio.create(covered, covered + missed));
                }

            }

            return result;
        }

        private String fixPackageName(final String name) {
            if (StringUtils.isNotBlank(name)) {
                return name.replaceAll("[\\\\/]", ".");
            }
            return name;
        }
    }
}
