package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.util.XMLUtils;
import io.jenkins.plugins.coverage.exception.CoverageException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;

public abstract class XMLCoverageReportAdapter extends CoverageReportAdapter {


    public XMLCoverageReportAdapter(String path) {
        super(path);
    }

    /**
     * @return XSL file that convert report into standard format
     */
    @CheckForNull
    public abstract String getXSL();

    /**
     * If return null, report will not be validate.
     *
     * @return XSD file to validate report
     */
    @Nullable
    public abstract String getXSD();

    /**
     * convert source xml file according to xsl file
     *
     * @param source source xml file
     */
    @Override
    public Document convert(File source) throws CoverageException {
        try {
            StreamSource xsl = getRealXSL();
            return XMLUtils.getInstance().convertToDocumentWithXSL(xsl, source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new CoverageException(e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected StreamSource getRealXSL() throws FileNotFoundException {
        String xsl = getXSL();
        if (StringUtils.isEmpty(xsl)) {
            throw new FileNotFoundException("Cannot found xsl file, xsl path must be no-empty");
        }
        return new StreamSource(getXSLResourceClass().getResourceAsStream(xsl));
    }

    private Class getXSLResourceClass() {
        return this.getClass();
    }

}
