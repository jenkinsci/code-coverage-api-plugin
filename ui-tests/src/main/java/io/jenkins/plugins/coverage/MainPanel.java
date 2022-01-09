package io.jenkins.plugins.coverage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class MainPanel extends PageObject {
    private static final String COVERAGE_TREND_CHART = "coverage-trendchart";
    public MainPanel(final Build parent, final String id) {
        super(parent, parent.url(id));
    }

    public MainPanel(final Build parent) {
        super(parent, parent.url);
    }

    /**
     * Checks if the trendChart is visible on the Page.
     *
     * @param chartName
     *         id of the Chart we want to evaluate.
     *
     * @return boolean value, that describes the visibility of the Trendchart.
     */
    public boolean trendChartIsDisplayed() {
        return driver.findElement(By.id(COVERAGE_TREND_CHART)).isDisplayed(); }


    public String getTrendChart(){
        return getChartById(COVERAGE_TREND_CHART);
    }

    private String getChartById(final String elementId) {
        //String em="history-chart-id138";
        Object result = executeScript(String.format(
                "delete(window.Array.prototype.toJSON) %n"
                        + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\").getElementsByClassName(\"echarts-trend\")[0]).getOption())",
                elementId));



        ScriptResult scriptResult = new ScriptResult(result);

        return scriptResult.getJavaScriptResult().toString();
    }

}
