package io.jenkins.plugins.coverage.adapter.util;


import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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


        //TODO solve the xslt 2.0 support
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer(xsl);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            throw new CoverageException(e);
        }

        try {
            transformer.transform(new StreamSource(source), result);
        } catch (TransformerException e) {
            // disable dtd validation then parse it again
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

            try {
                documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
                Document document = builder.parse(source);

                transformer.transform(new DOMSource(document), result);

            }  catch (ParserConfigurationException | TransformerException | IOException | SAXException ignore) {
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
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
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
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();

        DOMResult result = new DOMResult();
        transformer.transform(new StreamSource(file), result);

        return getDocumentFromDomResult(result);
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
}
