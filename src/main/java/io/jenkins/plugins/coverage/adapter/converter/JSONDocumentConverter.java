package io.jenkins.plugins.coverage.adapter.converter;

import com.alibaba.fastjson.JSONObject;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;

public abstract class JSONDocumentConverter extends DocumentConverter<JSONObject> {


    /**
     *
     * @param report JSON format report
     * @param document document that the report will convert to
     * @return standard format document converted by JSON format report
     */
    @Override
    protected abstract Document convert(JSONObject report, Document document) throws CoverageException;

}
