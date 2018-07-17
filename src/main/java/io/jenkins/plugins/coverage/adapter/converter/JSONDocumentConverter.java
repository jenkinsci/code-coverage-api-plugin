package io.jenkins.plugins.coverage.adapter.converter;

import com.fasterxml.jackson.databind.JsonNode;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;

public abstract class JSONDocumentConverter extends DocumentConverter<JsonNode> {


    /**
     *
     * @param report JSON format report
     * @param document document that the report will convert to
     * @return standard format document converted by JSON format report
     */
    @Override
    protected abstract Document convert(JsonNode report, Document document) throws CoverageException;

}
