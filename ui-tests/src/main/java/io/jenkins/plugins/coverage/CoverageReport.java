package io.jenkins.plugins.coverage;

import java.net.URL;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.google.inject.Injector;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * Tabs of File Coverage Overview Table.
 */
enum Tab {
    PACKAGE_OVERVIEW("coverageTree"),
    FILE_OVERVIEW("coverageTable");

    private final String id;

    /**
     * Constructor for Tab.
     * @param id
     */
    Tab(final String id) {
        this.id = id;
    }

    /**
     * Returns the enum element that has the specified href property.
     * @param href to select the tab
     * @return the tab
     * @throws NoSuchElementException if the tab could not be found
     */
    static Tab valueWithHref(final String href) {
        for (Tab tab : Tab.values()) {
            if (tab.id.equals(href.substring(1))) {
                return tab;
            }
        }
        throw new NoSuchElementException("No such tab with href " + href);
    }

    /**
     * Returns the selenium {@link By} selector to find the specific tab.
     * @return the selenium filter rule
     */
    By getXpath() {
        return By.xpath("//a[@href='#" + id + "']");
    }
}

/**
 * {@link PageObject} representing the Coverage Report.
 */
public class CoverageReport extends PageObject {
    private static final String RELATIVE_PATH_BUILD_TO_REPORT = "coverage";

    private static final String COVERAGE_OVERVIEW_CHART = "coverage-overview";
    private static final String COVERAGE_TREND_CHART = "coverage-trend";
    private static final String COVERAGE_TREE_CHART = "coverage-details";

    //TODO: add warning; is used
    public CoverageReport(final Injector injector, final URL url) {
        super(injector, url);
    }

    /**
     * Constructor to create CoverageReport-PageObject out of a build.
     * @param parent build of wanted CoverageReport.
     */
    public CoverageReport(final Build parent) {
        super(parent, parent.url(RELATIVE_PATH_BUILD_TO_REPORT));
    }

    /**
     * Getter for Coverage-Trend-Chart Data.
     * @return Json Value of Coverage-Trend Chart
     */
    public String getCoverageTrend() {
        ensureCoverageReportPageIsOpen();
        return getChartById(COVERAGE_TREND_CHART);
    }

    /**
     * Getter for Coverage-Overview-Chart Data.
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageOverview() {
        ensureCoverageReportPageIsOpen();
        return getChartById(COVERAGE_OVERVIEW_CHART);
    }

    /**
     * Getter for Coverage-Tree-Chart Data.
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageTree(){
        ensureCoverageReportPageIsOpen();
        return getChartById(COVERAGE_TREE_CHART);
    }

    /**
     * Opens and returns FileCoverageTable.
     * @return
     */
    public FileCoverageTable getCoverageTable(){
        return openFileCoverageTable();
    }

    /**
     * Returns if chart is displayed.
     * @param elementId of chart
     * @return if chart is displayed
     */
    private boolean isChartDisplayed(final String elementId){
        return find(By.id(elementId))!=null;
    }

    /**
     * Returns a chart's data by its id.
     * @param elementId of chart.
     * @return data as json.
     */
    private String getChartById(final String elementId) {
        waitFor().until(() -> isChartDisplayed(elementId));
        Object result = executeScript(String.format(
                "delete(window.Array.prototype.toJSON) %n"
                        + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\")).getOption())",
                elementId));
        ScriptResult scriptResult = new ScriptResult(result);

        return scriptResult.getJavaScriptResult().toString();
    }

    /**
     * Ensures the tab is 'File Overview'/FileCoverageTable is active.
     */
    private void ensureCoverageTableTabIsActive() {
        if(!getActiveTab().equals(Tab.FILE_OVERVIEW)){
            openTabCoverageTable();
        }
    }

    /**
     * Ensures CoverageReport Page is opened.
     */
    private void ensureCoverageReportPageIsOpen() {
        //TODO
        MatcherAssert.assertThat("coverage report page is open", this.driver.getCurrentUrl(), CoreMatchers.is(this.url));
    }

    /**
     * Opens File Coverage Table
     * @return
     */
    public FileCoverageTable openFileCoverageTable() {
        ensureCoverageReportPageIsOpen();
        ensureCoverageTableTabIsActive();
        return new FileCoverageTable( this);
    }

    /**
     * Opens tab in CoverageReport which contains the CoverageTree, aka Package Overview.
     */
    public void openTabCoverageTree() {
        openTab(Tab.PACKAGE_OVERVIEW);
    }

    /**
     * Opens tab in CoverageReport which contains the CoverageTable, aka File Overview
     */
    public void openTabCoverageTable() {
        openTab(Tab.FILE_OVERVIEW);
    }

    /**
     * Returns the active tab.
     * @return the active tab
     */
    public Tab getActiveTab() {
        WebElement activeTab = find(By.xpath("//a[@role='tab' and contains(@class, 'active')]"));
        return Tab.valueWithHref(extractRelativeUrl(activeTab.getAttribute("href")));
    }


    /**
     * Opens the specified tab.
     * @param tab that should be selected
     */
    private void openTab(final Tab tab) {
        WebElement tabElement = getElement(By.id("tab-details")).findElement(tab.getXpath());
        tabElement.click();
    }

    /**
     * Extract the relative url of a href (of a tab).
     * @param absoluteUrl of href of tab
     * @return relative url.
     */
    private String extractRelativeUrl(final String absoluteUrl) {
        return "#" + StringUtils.substringAfterLast(absoluteUrl, "#");
    }

}

