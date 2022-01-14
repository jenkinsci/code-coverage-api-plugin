package io.jenkins.plugins.coverage;


import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.openqa.selenium.NoSuchElementException;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.Job;
import org.jenkinsci.test.acceptance.po.PageObject;

import io.jenkins.plugins.coverage.utils.TrendchartUtil;

/**
 * {@link PageObject} representing the Job status on the build page of a job.
 */
public class MainPanel extends PageObject {
    private static final String COVERAGE_TREND_CHART = "coverage-trendchart";

    public MainPanel(final Job parent) {
        super(parent, parent.url);
    }

    /**
     * Getter for Coverage-Overview-Chart Data.
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageTrendChart() {
        ensureMainPanelPageIsOpen();
        return getTrendChart();
    }


    public String getTrendChart() {
        waitFor().until(() -> (TrendchartUtil.isChartDisplayed(this, COVERAGE_TREND_CHART)));
        Object result = executeScript(String.format(
                "delete(window.Array.prototype.toJSON) %n"
                        + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\").getElementsByClassName(\"echarts-trend\")[0]).getOption())",COVERAGE_TREND_CHART ));

        ScriptResult scriptResult = new ScriptResult(result);

        return scriptResult.getJavaScriptResult().toString();
    }


    public boolean isChartAvailable() {
        try {
            return waitFor().until(() -> (TrendchartUtil.isChartDisplayed(this, COVERAGE_TREND_CHART)));

        }
        catch(NoSuchElementException exception){
            return false;
        }
    }


    /**
     * Ensures MainPanel Page is opened.
     */
    private void ensureMainPanelPageIsOpen() {
        MatcherAssert.assertThat("main panel page was not opened", this.driver.getCurrentUrl(), CoreMatchers.anyOf(CoreMatchers.containsString(this.url.toString()), CoreMatchers.containsString(this.url+"/")));
    }

}
