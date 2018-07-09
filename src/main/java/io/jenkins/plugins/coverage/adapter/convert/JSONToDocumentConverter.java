package io.jenkins.plugins.coverage.adapter.convert;

import com.alibaba.fastjson.JSONObject;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public abstract class JSONToDocumentConverter {

    public Document convert(JSONObject jsonObject) throws CoverageException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new CoverageException(e);
        }

        Document document = builder.newDocument();

        convert(jsonObject, document);

        return document;
    }


    protected abstract Document convert(JSONObject jsonObject, Document document) throws CoverageException;

}
