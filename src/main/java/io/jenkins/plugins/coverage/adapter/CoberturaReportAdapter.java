package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public final class CoberturaReportAdapter extends JavaXMLCoverageReportAdapter {

    @DataBoundConstructor
    public CoberturaReportAdapter(String path) {
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

    @Extension
    public static final class CoverturaReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoberturaReportAdapter> {

        public CoverturaReportAdapterDescriptor() {
            super(CoberturaReportAdapter.class, "Cobertura");
        }
    }
}
