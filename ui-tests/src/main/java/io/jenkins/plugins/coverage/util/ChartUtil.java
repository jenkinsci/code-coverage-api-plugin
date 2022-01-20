package io.jenkins.plugins.coverage.util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.PageObject;


/**
 * Charts are displayed one multiple PageObjects.
 * This util provides some helper methods to deal with charts.
 */
@SuppressWarnings("hideutilityclassconstructor")
public class ChartUtil {

    /**
     * Returns a chart's data by its id.
     * @param pageObject which contains chart
     * @param elementId of chart
     * @return data as json
     */
    public static String getChartsDataById(PageObject pageObject, String elementId) {
        if (isChartDisplayed(pageObject, elementId)) {
            Object result = pageObject.executeScript(String.format(
                    "delete(window.Array.prototype.toJSON) %n"
                            + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\")).getOption())",
                    elementId));
            ScriptResult scriptResult = new ScriptResult(result);

            return scriptResult.getJavaScriptResult().toString();
        }
        return null;
    }

    /**
     * Returns if chart is displayed.
     * @param pageObject which contains chart
     * @param elementId of chart
     * @return if chart is displayed
     */
    public static boolean isChartDisplayed(PageObject pageObject, String elementId) {
        try {
            WebElement chart = pageObject.find(By.id(elementId));
            return chart != null && chart.isDisplayed();
        }
        catch (NoSuchElementException exception) {
            return false;
        }
    }
}
