package io.jenkins.plugins.coverage.adapter.util;


import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import net.sf.saxon.TransformerFactoryImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Utils class used for XML related operations.
 */
public class XMLUtils {

    // TODO delete the single-instance pattern
    private static XMLUtils converter = new XMLUtils();

    public static XMLUtils getInstance() {
        return converter;
    }

    private XMLUtils() {
    }

    /**
     * Use XSL to transform source xml file to {@link Document}.
     *
     * @param xsl    XSL source
     * @param source source xml file
     * @return document transformed from source file
     */
    public Document convertToDocumentWithXSL(StreamSource xsl, File source)
            throws FileNotFoundException, CoverageException {
        DOMResult result = convertToDOMResultWithXSL(xsl, source);

        return getDocumentFromDomResult(result);
    }

    /**
     * Use XSL to transform source xml file to {@link Result}.
     *
     * @param xsl    XSL source
     * @param source source xml file
     * @param result result transformed from source file
     */
    private void convertWithXSL(StreamSource xsl, File source, Result result)
            throws FileNotFoundException, CoverageException {

        if (!source.exists()) {
            throw new FileNotFoundException("source File does not exist!");
        }

        TransformerFactory transformerFactory = newSecureTransformerFactory();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer(xsl);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            throw new CoverageException(e);
        }

        try {
            transformer.transform(new StreamSource(source), result);

            // xml parser implementation sometimes may not throw exception, so we manually check it.
            // TODO replace it by transformer ErrorListener
            if (result instanceof DOMResult) {
                Document d = getDocumentFromDomResult((DOMResult) result);
                if (d == null || d.getDocumentElement() == null) {
                    throw new TransformerException("Transform failed");
                }
            }
        } catch (TransformerException e) {
            // disable dtd validation then parse it again
            try {
                transformer.transform(new DOMSource(readXMLtoDocumentWithoutXSD(source)), result);
            } catch (TransformerException | ParserConfigurationException | IOException | SAXException ignore) {
                throw new CoverageException(e);
            }
        }
    }


    /**
     * Use XSL to transform source xml file to {@link DOMResult}.
     *
     * @param xsl    XSL source
     * @param source source xml file
     * @return DOMResult transformed from source file
     */
    public DOMResult convertToDOMResultWithXSL(StreamSource xsl, File source)
            throws FileNotFoundException, CoverageException {
        DOMResult result = new DOMResult();
        convertWithXSL(xsl, source, result);
        return result;
    }

    /**
     * Use XSL to transform source xml file to {@link SAXResult}.
     *
     * @param xsl    XSL source
     * @param source source xml file
     * @return SAXResult transformed from source file
     */
    public SAXResult convertToSAXResultWithXSL(StreamSource xsl, File source)
            throws FileNotFoundException, CoverageException {
        SAXResult result = new SAXResult();
        convertWithXSL(xsl, source, result);
        return result;
    }


    /**
     * Write {@link Document} to target file.
     *
     * @param document document be written
     * @param target   target file written to
     */
    public void writeDocumentToXML(Document document, File target) {
        TransformerFactory transformerFactory = newSecureTransformerFactory();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(target));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read xml file and return it as {@link Document} format.
     *
     * @param file xml file be read
     * @return document converted by xml
     * @throws TransformerException file cannot be convert to {@link Document}
     */
    public Document readXMLtoDocument(File file) throws TransformerException {
        TransformerFactory factory = newSecureTransformerFactory();
        Transformer transformer = factory.newTransformer();

        DOMResult result = new DOMResult();
        try {
            transformer.transform(new StreamSource(file), result);
        } catch (TransformerException e) {
            try {
                transformer.transform(new DOMSource(readXMLtoDocumentWithoutXSD(file)), result);
            } catch (ParserConfigurationException | IOException | SAXException ignore) {
                throw e;
            }
        }
        return getDocumentFromDomResult(result);
    }

    /**
     * Read xml file without loading external dtd.
     *
     * @param file xml file be read
     * @return document converted by xml
     */
    private Document readXMLtoDocumentWithoutXSD(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);

        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

        return builder.parse(file);
    }

    /**
     * Get document from {@link DOMResult}.
     *
     * @param domResult DOMResult
     * @return Document from {@link DOMResult}
     */
    private Document getDocumentFromDomResult(DOMResult domResult) {
        Node node = domResult.getNode();
        if (node == null) {
            return null;
        }

        return node.getNodeType() == Node.DOCUMENT_NODE ? ((Document) node) : node.getOwnerDocument();
    }

    private TransformerFactory newSecureTransformerFactory() {
        TransformerFactory transformerFactory = new TransformerFactoryImpl();
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute("http://saxon.sf.net/feature/parserFeature?uri=http://apache.org/xml/features/disallow-doctype-decl", true);
            transformerFactory.setAttribute("http://saxon.sf.net/feature/parserFeature?uri=http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            transformerFactory.setAttribute("http://saxon.sf.net/feature/parserFeature?uri=http://xml.org/sax/features/external-general-entities", false);
            transformerFactory.setAttribute("http://saxon.sf.net/feature/parserFeature?uri=http://xml.org/sax/features/external-parameter-entities", false);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        return transformerFactory;
    }
}
