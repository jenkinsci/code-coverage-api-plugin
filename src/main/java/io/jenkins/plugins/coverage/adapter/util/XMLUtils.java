package io.jenkins.plugins.coverage.adapter.util;


import io.jenkins.plugins.coverage.exception.ConversionException;
import org.dom4j.io.DocumentSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class XMLUtils {

    private static XMLUtils converter = new XMLUtils();

    public static XMLUtils getInstance() {
        return converter;
    }

    private XMLUtils() {
    }

    /**
     * Converting source xml file to target file according the XSLT file
     *
     * @param xsl    XSLT file
     * @param source Source xml file
     * @return Converted document
     */
    public Document convertToDocumentWithXSL(File xsl, File source) {
        Node node = convertToDOMResultWithXSL(xsl, source).getNode();
        if (node == null)
            return null;
        return node.getNodeType() == Node.DOCUMENT_NODE ? ((Document) node) : node.getOwnerDocument();
    }

    /**
     * Converting source xml file to target result according the XSLT file
     *
     * @param xsl    XSLT file
     * @param source Source file
     * @param result Result that want to be written in
     */
    private void convertWithXSL(File xsl, File source, Result result) {
        if (!xsl.exists()) {
            throw new ConversionException("XSL File not exist!");
        }

        if (!source.exists()) {
            throw new ConversionException("source File not exist!");
        }


        //TODO solve the xslt 2.0 support
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer(new StreamSource(xsl));
            transformer.transform(new StreamSource(source), result);
        } catch (TransformerException e) {
            e.printStackTrace();
            throw new ConversionException(e);
        }
    }


    /**
     * Converting source xml file to {@link DOMResult}
     *
     * @param xsl    XSLT file
     * @param source Source xml file
     * @return DOMResult
     */
    public DOMResult convertToDOMResultWithXSL(File xsl, File source) {
        DOMResult result = new DOMResult();
        convertWithXSL(xsl, source, result);
        return result;
    }

    /**
     * Converting source xml file to {@link SAXResult}
     *
     * @param xsl    XSLT file
     * @param source Source xml file
     * @return SAXResult
     */
    public SAXResult convertToSAXResultWithXSL(File xsl, File source) {
        SAXResult result = new SAXResult();
        convertWithXSL(xsl, source, result);
        return result;
    }


    public void writeToXMLFile(Document document, File target) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(target));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
