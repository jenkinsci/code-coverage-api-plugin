package io.jenkins.plugins.coverage.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.plugins.coverage.adapter.converter.JSONDocumentConverter;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;

public abstract class JSONCoverageReportAdapter extends CoverageReportAdapter {

    /**
     * @param path Ant-style path of report files.
     */
    public JSONCoverageReportAdapter(String path) {
        super(path);
    }

    @Override
    protected Document convert(File source) throws CoverageException {
        try {
            return getConverter().convert(new ObjectMapper().readTree(source));
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoverageException(e);
        }
    }


    /**
     * @return converter which convert JSONObject to DOM Document.
     */
    protected abstract JSONDocumentConverter getConverter();

}
