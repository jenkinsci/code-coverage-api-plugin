package io.jenkins.plugins.coverage.adapter;

import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Lists;

import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.Symbol;
import hudson.Extension;

import io.jenkins.plugins.coverage.adapter.parser.CoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;

public class IstanbulCoberturaReportAdapter extends XMLCoverageReportAdapter {

    @DataBoundConstructor
    public IstanbulCoberturaReportAdapter(final String path) {
        super(path);
    }

    @CheckForNull
    @Override
    public String getXSL() {
        return "istanbul-cobertura-to-standard.xsl";
    }

    @Nullable
    @Override
    public String getXSD() {
        return null;
    }

    @CheckForNull
    @Override
    protected CoverageResult parseToResult(final Document document, final String reportName) throws CoverageException {
        return new IstanbulCoberturaCoverageParser(reportName).parse(document);
    }


    @Symbol(value = {"istanbulCoberturaAdapter", "istanbulCobertura"})
    @Extension
    public static class IstanbulCoberturaReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public IstanbulCoberturaReportAdapterDescriptor() {
            super(IstanbulCoberturaReportAdapter.class);
        }

        @Override
        public List<CoverageElement> getCoverageElements() {
            return Lists.newArrayList(
                    new CoverageElement("Directory", 1),
                    CoverageElement.FILE,
                    new CoverageElement("Function", 4)
            );
        }

        @Override
        public String getCoverageElementType() {
            return CoverageElement.COVERAGE_ELEMENT_TYPE_JAVASCRIPT;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.IstanbulCoberturaReportAdapter_displayName();
        }
    }

    public static class IstanbulCoberturaCoverageParser extends CoverageParser {

        /**
         * Report name will show in the UI, to differentiate different report.
         *
         * @param reportName name of the report
         */
        public IstanbulCoberturaCoverageParser(final String reportName) {
            super(reportName);
        }

        @Override
        protected CoverageResult processElement(final Element current, final CoverageResult parentResult) {

            CoverageResult result = null;
            switch (current.getLocalName()) {
                case "report":
                    result = new CoverageResult(CoverageElement.REPORT, null,
                            getAttribute(current, "name", "") + ": " + getReportName());
                    break;
                case "directory":
                    String directoryName = getAttribute(current, "name", "<root>")
                            .replaceAll("\\.", "/");

                    result = new CoverageResult(CoverageElement.get("Directory"), parentResult, directoryName);
                    break;
                case "file":
                    result = new CoverageResult(CoverageElement.get("File"), parentResult,
                            getAttribute(current, "name", ""));

                    result.setRelativeSourcePath(getAttribute(current, "name", null));
                    break;
                case "function":
                    String functionName = getAttribute(current, "name", "");

                    result = new CoverageResult(CoverageElement.get("Function"), parentResult, functionName);

                    break;
                case "line":
                    processLine(current, parentResult);
                    break;
                case "additionalProperty":
                    String propertyName = getAttribute(current, "name", "");
                    if (StringUtils.isEmpty(propertyName)) {
                        break;
                    }

                    String propertyValue = getAttribute(current, "value", "");
                    parentResult.addAdditionalProperty(propertyName, propertyValue);
                    break;
                default:
                    break;
            }
            return result;
        }
    }

}
