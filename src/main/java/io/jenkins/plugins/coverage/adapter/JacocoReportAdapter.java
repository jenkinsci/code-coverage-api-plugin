package io.jenkins.plugins.coverage.adapter;

import hudson.Extension;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

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

    @Symbol("jacoco")
    @Extension
    public static final class JacocoReportAdapterDescriptor extends JavaCoverageReportAdapterDescriptor {

        public JacocoReportAdapterDescriptor() {
            super(JacocoReportAdapter.class);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.JacocoReportAdapter_displayName();
        }
    }
}
