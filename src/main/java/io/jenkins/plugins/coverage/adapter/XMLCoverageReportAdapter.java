package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.util.XMLUtils;
import io.jenkins.plugins.coverage.exception.ConversionException;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URISyntaxException;

public abstract class XMLCoverageReportAdapter extends CoverageReportAdapter {

    public XMLCoverageReportAdapter(String path) {
        super(path);
    }

    public abstract String getXSL();

    public abstract String getXSD();

    /**
     * convertWithXSL source xml file according to the xsl, and output the result to target file
     *
     * @param source source xml file
     */
    @Override
    public Document convert(File source) {
        File xsl = getRealXSL();
        return XMLUtils.getInstance().convertToDocumentWithXSL(xsl, source);
    }

    @SuppressWarnings("WeakerAccess")
    protected File getRealXSL() {
        try {
            return new File(getXSLResourceClass().getResource(getXSL()).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ConversionException(e);
        }
    }

    private Class<? extends CoverageReportAdapter> getXSLResourceClass() {
        return this.getClass();
    }
}
