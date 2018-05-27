package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public final class JacocoReportAdapter extends JavaXMLCoverageReportAdapter {

    @DataBoundConstructor
    public JacocoReportAdapter(String path) {
        super(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXSL() {
        return "jacoco-to-standard.xsl";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXSD() {
        return null;
    }

    @Symbol("jacocoAdapter")
    @Extension
    public static final class JacocoReportAdapterDescriptor extends CoverageReportAdapterDescriptor<CoverageReportAdapter> {

        public JacocoReportAdapterDescriptor() {
            super(JacocoReportAdapter.class, "jacoco");
        }
    }
}
