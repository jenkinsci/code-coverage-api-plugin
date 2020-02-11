package io.jenkins.plugins.coverage.adapter;

import com.google.common.collect.Lists;
import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.parser.CoverageParser;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class IstanbulCoberturaReportAdapter extends XMLCoverageReportAdapter {

    @DataBoundConstructor
    public IstanbulCoberturaReportAdapter(String path) {
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
    protected CoverageResult parseToResult(Document document, String reportName) throws CoverageException {
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
                    new CoverageElement("File", 2),
                    new CoverageElement("Function", 4)
            );
        }

        @Override
        public String getCoverageElementType() {
            return CoverageElement.COVERAGE_ELEMENT_TYPE_JAVASCRIPT;
        }

        @Nonnull
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
        public IstanbulCoberturaCoverageParser(String reportName) {
            super(reportName);
        }

        @Override
        protected CoverageResult processElement(Element current, CoverageResult parentResult) {

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
