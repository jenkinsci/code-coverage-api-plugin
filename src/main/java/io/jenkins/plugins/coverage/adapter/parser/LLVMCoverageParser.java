package io.jenkins.plugins.coverage.adapter.parser;

import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.w3c.dom.Element;

public class LLVMCoverageParser extends CoverageParser {
    /**
     * Report name will show in the UI, to differentiate different report.
     *
     * @param reportName name of the report
     */
    public LLVMCoverageParser(String reportName) {
        super(reportName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CoverageResult processElement(Element current, CoverageResult parentResult) {
        CoverageResult result = null;

        String name = current.getLocalName();
        if (name == null) {
            name = current.getTagName();
        }

        switch (name) {
            case "report":
                result = new CoverageResult(CoverageElement.REPORT, null,
                        getAttribute(current, "name", "") + ": " + getReportName());
                break;
            case "data":
                result = new CoverageResult(CoverageElement.LLVM_DATA, parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "directory":
                result = new CoverageResult(CoverageElement.LLVM_DIRECTORY, parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "file":
                result = new CoverageResult(CoverageElement.LLVM_FILE, parentResult,
                        getAttribute(current, "filename", ""));

                result.setRelativeSourcePath(getAttribute(current, "name", null));
                break;
            case "function":
                result = new CoverageResult(CoverageElement.LLVM_FUNCTION, parentResult,
                        getAttribute(current, "name", ""));
                break;
            case "line":
                processLine(current, parentResult);
                break;
            default:
                break;
        }
        return result;
    }

}
