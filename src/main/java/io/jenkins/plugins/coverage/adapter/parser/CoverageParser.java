package io.jenkins.plugins.coverage.adapter.parser;

import io.jenkins.plugins.coverage.targets.CoverageResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class CoverageParser {

    public CoverageResult parse(Document document) {
        CoverageResult result = processElement(document.getDocumentElement(), null);
        parse(document.getDocumentElement(), result);
        return result;
    }

    public void parse(Node parent, CoverageResult parentResult) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                CoverageResult r = processElement((Element) n, parentResult);
                parse(n, r);
            }
        }
    }

    protected abstract CoverageResult processElement(Element current, CoverageResult parentResult);

}
