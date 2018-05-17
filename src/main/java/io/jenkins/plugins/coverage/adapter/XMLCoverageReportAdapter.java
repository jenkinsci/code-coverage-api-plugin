package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.util.XMLUtils;
import io.jenkins.plugins.coverage.exception.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

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
    public Document convert(File source) throws ConversionException {
        File xsl;
        try {
            xsl = getRealXSL();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new ConversionException(e);
        }
        try {
            return XMLUtils.getInstance().convertToDocumentWithXSL(xsl, source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new ConversionException(e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected File getRealXSL() throws ConversionException, FileNotFoundException {
        try {
            String xsl = getXSL();
            if (StringUtils.isEmpty(xsl)) {
                throw new FileNotFoundException("xsl path must be no-empty");
            }

            File realXSL = new File(getClass().getResource(xsl).toURI());
            if (!realXSL.exists() || !realXSL.isFile()) {
                throw new FileNotFoundException("Cannot found xsl file");
            } else {
                return realXSL;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ConversionException(e);
        }
    }
}
