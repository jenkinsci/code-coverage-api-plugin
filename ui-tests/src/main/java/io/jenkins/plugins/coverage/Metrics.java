package io.jenkins.plugins.coverage;

import org.openqa.selenium.By;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class Metrics extends PageObject {
    private static final String RELATIVE_PATH_BUILD_TO_METRICS = "metrics";

    public Metrics(final Build parent) {
        super(parent, parent.url(RELATIVE_PATH_BUILD_TO_METRICS));
    }

    //would it be better to use the path from url in metrics?
    public AvailableMetrics openAvailableMetrics(){
        AvailableMetrics availableMetrics = new AvailableMetrics(this);
        availableMetrics.open();
        return availableMetrics;
    }

}
