package io.jenkins.plugins.coverage.adapter;

import io.jenkins.plugins.coverage.adapter.util.XMLUtils;
import io.jenkins.plugins.coverage.exception.ConversionException;
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
     *
     * @return XSL file that convert report into standard format
     */
    @CheckForNull
    public abstract String getXSL();

    /**
     * If return null, report will not be validate.
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
    public Document convert(File source) {
        File xsl;
        try {
            xsl = getRealXSL();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new ConversionException(e);
        }
        return XMLUtils.getInstance().convertToDocumentWithXSL(xsl, source);
    }

    @SuppressWarnings("WeakerAccess")
    protected File getRealXSL() throws FileNotFoundException {
        try {
            File realXSL = new File(getClass().getResource(getXSL()).toURI());
            if(!realXSL.exists()) {
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
