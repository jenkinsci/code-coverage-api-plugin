package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class CoberturaReportAdapter extends JavaXMLCoverageReportAdapter {

    @DataBoundConstructor
    public CoberturaReportAdapter(String path) {
        super(path);
    }

    @Override
    public String getXSL() {
        return "cobertura-to-standard.xsl";
    }

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
