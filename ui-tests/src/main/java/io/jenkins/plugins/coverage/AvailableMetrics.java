package io.jenkins.plugins.coverage;


import org.openqa.selenium.By;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class AvailableMetrics extends PageObject {
    //TODO: change path?
    private static final String RELATIVE_PATH_BUILD_TO_METRICS = "metrics/available";

    public AvailableMetrics(final Metrics parent) {
        super(parent, parent.url(RELATIVE_PATH_BUILD_TO_METRICS));
    }

}
