package io.jenkins.plugins.coverage.utils;


import org.openqa.selenium.By;

import com.gargoylesoftware.htmlunit.ScriptResult;

import org.jenkinsci.test.acceptance.po.PageObject;

public class TrendchartUtil  {


    /**
     * Returns a chart's data by its id.
     * @param elementId of chart.
     * @return data as json.
     */
    public static String getChartsDataById(final PageObject pageObject, final String elementId) {
        pageObject.waitFor().until(() -> isChartDisplayed(pageObject, elementId));
        Object result = pageObject.executeScript(String.format(
                "delete(window.Array.prototype.toJSON) %n"
                        + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\")).getOption())",
                elementId));
        ScriptResult scriptResult = new ScriptResult(result);

        return scriptResult.getJavaScriptResult().toString();
    }

    /**
     * Returns if chart is displayed.
     * @param elementId of chart
     * @return if chart is displayed
     */
    public static boolean isChartDisplayed(final PageObject pageObject, final String elementId){
        return pageObject.find(By.id(elementId))!=null;
    }

}
