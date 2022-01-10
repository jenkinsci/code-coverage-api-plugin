package io.jenkins.plugins.coverage;

import java.net.URL;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.google.inject.Injector;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

enum Tab {
    PACKAGE_OVERVIEW("coverageTree"),
    FILE_OVERVIEW("coverageTable");

    private final String property;

    Tab(final String property) {
        this.property = property;
    }

    /**
     * Returns the enum element that has the specified href property.
     *
     * @param href
     *         the href to select the tab
     *
     * @return the tab
     * @throws NoSuchElementException
     *         if the tab could not be found
     */
    static Tab valueWithHref(final String href) {
        for (Tab tab : Tab.values()) {
            if (tab.property.equals(href.substring(1))) {
                return tab;
            }
        }
        throw new NoSuchElementException("No such tab with href " + href);
    }

    /**
     * Returns the selenium {@link By} selector to find the specific tab.
     *
     * @return the selenium filter rule
     */
    By getXpath() {
        return By.xpath("//a[@href='#" + property + "']");
    }
}

/**
 * {@link PageObject} representing the Coverage Report.
 */
public class CoverageReport extends PageObject {
    private static final String COVERAGE_OVERVIEW_CHART = "coverage-overview";
    private static final String COVERAGE_TREND_CHART = "coverage-trend";
    private static final String COVERAGE_TREE_CHART = "coverage-details";
    private static final String RELATIVE_PATH_BUILD_TO_REPORT = "coverage";

    //is currently used //TODO: add warning
    public CoverageReport(final Injector injector, final URL url) {
        super(injector, url);
    }

    public CoverageReport(final Build parent) {
        super(parent, parent.url(RELATIVE_PATH_BUILD_TO_REPORT));
    }

    /**
     * Getter for Coverage-Trend-Chart Data.
     *
     * @return Json Value of Coverage-Trend Chart
     */
    public String getCoverageTrend() {
        return getChartById(COVERAGE_TREND_CHART);
    }

    /**
     * Getter for Coverage-Overview-Chart Data.
     *
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageOverview() {
        return getChartById(COVERAGE_OVERVIEW_CHART);
    }

    private boolean isChartDisplayed(final String elementId){
        return find(By.id(elementId))!=null;
    }

    private String getChartById(final String elementId) {
        waitFor().until(() -> isChartDisplayed(elementId));
        Object result = executeScript(String.format(
                "delete(window.Array.prototype.toJSON) %n"
                        + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\")).getOption())",
                elementId));
        ScriptResult scriptResult = new ScriptResult(result);

        return scriptResult.getJavaScriptResult().toString();
    }


    public String getCoverageTree(){
        return getChartById(COVERAGE_TREE_CHART);
    }


    /*
    //aka file overview
    public String getCoverageTable(){
        ensureFileOverviewIsActive();
        WebElement table = driver.findElement(By.tagName("coverage-details_wrapper"));
        return getRecordsFromTable("c");
    }
*/
    private void ensureCoverageTableTabIsActive() {
        if(!getActiveTab().equals(Tab.FILE_OVERVIEW)){
            openTabCoverageTable();
        }
    }

    private void ensureCoverTreeTabIsActive() {
        if(!getActiveTab().equals(Tab.PACKAGE_OVERVIEW)){
            openTabCoverageTree();
        }
    }


    public FileCoverageTable openFileCoverageTable() {
        ensureCoverageTableTabIsActive();
        return new FileCoverageTable( this);
    }



    /**
     * Returns the active and visible tab that has the focus in the tab bar.
     *
     * @return the active tab
     */
    public Tab getActiveTab() {
        WebElement activeTab = find(By.xpath("//a[@role='tab' and contains(@class, 'active')]"));
        return Tab.valueWithHref(extractRelativeUrl(activeTab.getAttribute("href")));
    }

    /**
     * Opens the analysis details page and selects the specified tab.
     *
     * @param tab
     *         the tab that should be selected
     */
    private void openTab(final Tab tab) {
        open();
        WebElement tabElement = getElement(By.id("tab-details")).findElement(tab.getXpath());
        tabElement.click();
    }


    public void openTabCoverageTree() {
        openTab(Tab.PACKAGE_OVERVIEW);
    }

    public void openTabCoverageTable() {
        openTab(Tab.FILE_OVERVIEW);
    }

    /**
     * Reloads the {@link PageObject}.
     */
    public void reload() {
        open();
    }

    private String extractRelativeUrl(final String absoluteUrl) {
        return "#" + StringUtils.substringAfterLast(absoluteUrl, "#");
    }

}

