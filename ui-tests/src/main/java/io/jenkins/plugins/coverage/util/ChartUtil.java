package io.jenkins.plugins.coverage.util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * Charts are displayed one multiple PageObjects. This util provides some helper methods to deal with charts.
 */
@SuppressWarnings("hideutilityclassconstructor")
// TODO: Move this code to ATH so we can reuse it from other plugins
public class ChartUtil {
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Returns a chart's data by its id.
     *
     * @param pageObject
     *         which contains chart
     * @param elementId
     *         of chart
     *
     * @return data as json
     */
    public static String getChartDataById(final PageObject pageObject, final String elementId) {
        if (isChartDisplayedByElementId(pageObject, elementId)) {
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
     * Returns data of only chart with given tool attribute value on page.
     *
     * @param pageObject
     *         which contains only one chart with given tool attribute value
     * @param toolAttribute
     *         value in div tag of chart
     *
     * @return data as json
     */
    public static String getDataOfOnlyChartOnPageWithGivenToolAttribute(final PageObject pageObject,
            final String toolAttribute) {
        if (isChartDisplayedByDivToolAttribute(pageObject, toolAttribute)) {
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                Object result = pageObject.executeScript(String.format(
                        "delete(window.Array.prototype.toJSON) %n"
                                + "return JSON.stringify(echarts.getInstanceByDom(document.querySelector(\"div [tool='%s']\")).getOption())",
                        toolAttribute));

                Object scriptResult = new ScriptResult(result).getJavaScriptResult();
                if (scriptResult != null) {
                    return scriptResult.toString();
                }
                pageObject.elasticSleep(1000);
            }
        }
        throw new java.util.NoSuchElementException("Found no trend chart with ID '%s''" + toolAttribute);
    }

    /**
     * Returns if chart is displayed.
     *
     * @param pageObject
     *         which contains chart
     * @param elementId
     *         of chart
     *
     * @return if chart is displayed
     */
    public static boolean isChartDisplayedByElementId(final PageObject pageObject, final String elementId) {
        try {
            WebElement chart = pageObject.find(By.id(elementId));
            return chart != null && chart.isDisplayed();
        }
        catch (NoSuchElementException exception) {
            return false;
        }
    }

    /**
     * Returns if a chart with given tool attribute in div tag is displayed.
     *
     * @param pageObject
     *         which contains chart
     * @param toolAttribute
     *         of div tag of chart
     *
     * @return if chart is displayed
     */
    public static boolean isChartDisplayedByDivToolAttribute(final PageObject pageObject, final String toolAttribute) {
        try {
            WebElement chart = pageObject.find(By.cssSelector("div[tool='" + toolAttribute + "']"));
            return chart != null && chart.isDisplayed();
        }
        catch (NoSuchElementException exception) {
            return false;
        }

    }

}
