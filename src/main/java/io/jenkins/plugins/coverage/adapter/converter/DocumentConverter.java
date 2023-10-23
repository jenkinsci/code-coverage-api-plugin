package io.jenkins.plugins.coverage.adapter.converter;

import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public abstract class DocumentConverter<T> {

    /**
     * Convert other format report to standard format Document.
     *
     * @param report other format report
     * @return document converted by other report
     */
    public Document convert(T report) throws CoverageException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new CoverageException(e);
        }

        Document document = builder.newDocument();

        convert(report, document);

        return document;
    }

    /**
     * Convert other format report to standard format Document.
     * @param report other format report
     * @param document document that the report will convert to
     * @return document converted by other report
     */
    protected abstract Document convert(T report, Document document) throws CoverageException;


}
