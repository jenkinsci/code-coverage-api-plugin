package io.jenkins.plugins.coverage;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageObject;

import io.jenkins.plugins.coverage.util.ChartUtil;

/**
 * {@link PageObject} representing the Job status on the build page of a job.
 */
public class MainPanel extends PageObject {
    private static final String VALUE_OF_TOOL_ATTRIBUTE_IN_CHART = "coverage";

    /**
     * Constructor to create MainPanel-PageObject out of a job.
     *
     * @param parent
     *         job of wanted MainPanel.
     */
    public MainPanel(Job parent) {
        super(parent, parent.url);
    }

    /**
     * Getter for Coverage-Overview-Chart Data.
     *
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageTrendChart() {
        waitFor().until(()-> isChartDisplayed());
        return ChartUtil.getDataOfOnlyChartOnPageWithGivenToolAttribute(this, VALUE_OF_TOOL_ATTRIBUTE_IN_CHART);
    }

    /**
     * Returns if TrendChart is displayed in MainPanel.
     * @return if TrendChart is displayed
     */
    public boolean isChartDisplayed() {
        ensureMainPanelPageIsOpen();
        return ChartUtil.isChartDisplayedByDivToolAttribute(this, VALUE_OF_TOOL_ATTRIBUTE_IN_CHART);
    }

    /**
     * Ensures MainPanel Page is opened.
     */
    private void ensureMainPanelPageIsOpen() {
        MatcherAssert.assertThat("main panel page was not opened", this.driver.getCurrentUrl(),
                CoreMatchers.anyOf(CoreMatchers.containsString(this.url.toString()),
                        CoreMatchers.containsString(this.url + "/")));
    }

}
