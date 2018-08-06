package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import io.jenkins.plugins.coverage.adapter.util.XMLUtils;
import io.jenkins.plugins.coverage.detector.Detectable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.xml.transform.TransformerException;
import java.io.File;

/**
 * Coverage report adapter for Cobertura.
 */
public final class CoberturaReportAdapter extends JavaXMLCoverageReportAdapter {

    @DataBoundConstructor
    public CoberturaReportAdapter(final String path) {
        super(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXSL() {
        return "cobertura-to-standard.xsl";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXSD() {
        return null;
    }

    @Symbol("cobertura")
    @Extension
    public static final class CoberturaReportAdapterDescriptor extends JavaCoverageReportAdapterDescriptor
            implements Detectable {

        public CoberturaReportAdapterDescriptor() {
            super(CoberturaReportAdapter.class);
        }

        /**
         * @param file file be detect
         * @return <code>true</code> is file is a cobertura report
         */
        @Override
        public boolean detect(final File file) {
            if (!file.exists()) {
                return false;
            }

            Document d;
            try {
                d = XMLUtils.getInstance().readXMLtoDocument(file);
            } catch (TransformerException ignore) {
                return false;
            }

            Element e = d.getDocumentElement();
            if (e == null) {
                return false;
            }

            return "coverage".equals(e.getLocalName());
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.CoberturaReportAdapter_displayName();
        }
    }
}
