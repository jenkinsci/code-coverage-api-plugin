package io.jenkins.plugins.coverage;

import java.net.URL;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

public class SourceCodeView extends PageObject {
    private static final String COVERAGE_OVERVIEW_CHART = "coverage-overview";

    public SourceCodeView(Build parent, String rel) {
        super(parent, parent.url(rel));

    }

}
