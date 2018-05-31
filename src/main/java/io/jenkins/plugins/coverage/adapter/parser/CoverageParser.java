package io.jenkins.plugins.coverage.adapter.parser;

import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parse the standard format coverage report and convert it into {@link CoverageResult}.
 */
public abstract class CoverageParser {

    private String reportName;

    /**
     * Report name will show in the UI, to differentiate different report.
     *
     * @param reportName name of the report
     */
    public CoverageParser(String reportName) {
        this.reportName = reportName;
    }

    /**
     * Parse coverage report {@link Document} to {@link CoverageResult}.
     *
     * @param document DOM document of coverage report
     * @return Coverage result of specified report
     */
    public CoverageResult parse(Document document) {
        CoverageResult result = processElement(document.getDocumentElement(), null);
        parse(document.getDocumentElement(), result);
        return result;
    }

    /**
     * Iterate the child elements of parent node, and parse the element to {@link CoverageResult}, then make the CoverageResult.
     * be the child of parent CoverageResult.
     *
     * @param parent       parent Node
     * @param parentResult parent coverage result
     */
    public void parse(final Node parent, final CoverageResult parentResult) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                CoverageResult r = processElement((Element) n, parentResult);
                parse(n, r);
            }
        }
    }

    /**
     * Process DOM {@link Element} and convert it to {@link CoverageResult}.
     *
     * @param current      current element
     * @param parentResult parent coverage result
     * @return coverage result converted from Element
     */
    protected abstract CoverageResult processElement(Element current, CoverageResult parentResult);


    /**
     * Getter for property 'reportName'.
     *
     * @return value for property 'reportName'
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Setter for property 'reportName'.
     *
     * @param reportName value to set for property 'reportName'
     */
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }
}
