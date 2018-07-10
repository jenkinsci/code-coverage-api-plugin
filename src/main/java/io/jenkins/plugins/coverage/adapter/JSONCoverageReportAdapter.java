package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.converter.JSONDocumentConverter;
import io.jenkins.plugins.coverage.adapter.util.JSONUtils;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;

import java.io.File;

public abstract class JSONCoverageReportAdapter extends CoverageReportAdapter {

    /**
     * @param path Ant-style path of report files.
     */
    public JSONCoverageReportAdapter(String path) {
        super(path);
    }

    @Override
    protected Document convert(File source) throws CoverageException {
        return getConverter().convert(JSONUtils.readToJSONObject(source));
    }


    /**
     * @return converter which convert JSONObject to DOM Document.
     */
    protected abstract JSONDocumentConverter getConverter();

}
