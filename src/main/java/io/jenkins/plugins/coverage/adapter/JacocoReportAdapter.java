package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class JacocoReportAdapter extends JavaXMLCoverageReportAdapter {

    @DataBoundConstructor
    public JacocoReportAdapter(String path) {
        super(path);
    }

    @Override
    public String getXSL() {
        return "jacoco-to-standard.xsl";
    }

    @Override
    public String getXSD() {
        return null;
    }

    @Extension
    public static final class JacocoReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public JacocoReportAdapterDescriptor() {
            super(JacocoReportAdapter.class, "jacoco");
        }
    }
}
