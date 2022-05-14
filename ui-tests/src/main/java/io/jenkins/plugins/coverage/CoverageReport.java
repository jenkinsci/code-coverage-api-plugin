package io.jenkins.plugins.coverage;

import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

import static io.jenkins.plugins.coverage.util.ChartUtil.*;

/**
 * {@link PageObject} representing the Coverage Report.
 */
public class CoverageReport extends PageObject {
    private static final String RELATIVE_PATH_BUILD_TO_REPORT = "coverage";

    private static final String REPORT_OVERVIEW_TOGGLE = "overviewToggleLabel";
    private static final String REPORT_DETAILS_TOGGLE = "detailToggleLabel";

    private static final String TAB_NAV_TREE = "tab-tree";
    private static final String TAB_NAV_DETAILS = "tab-details";

    private static final String COVERAGE_OVERVIEW_CHART = "coverage-overview";
    private static final String COVERAGE_TREE_CHART = "project-coverage";

    /**
     * Constructor to create CoverageReport-PageObject out of a build.
     *
     * @param parent
     *         build of wanted CoverageReport.
     */
    public CoverageReport(Build parent) {
        super(parent, parent.url(RELATIVE_PATH_BUILD_TO_REPORT));
    }

    /**
     * Getter for Coverage-Overview-Chart Data.
     *
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageOverview() {
        ensureCoverageReportPageIsOpen();
        ensureCoverageTreeTabIsActive();
        return getChartDataById(this, COVERAGE_OVERVIEW_CHART);
    }

    /**
     * Getter for Coverage-Tree-Chart Data.
     *
     * @return Json Value of Coverage-Overview Chart
     */
    public String getCoverageTree() {
        ensureCoverageReportPageIsOpen();
        ensureCoverageTreeTabIsActive();
        return getChartDataById(this, COVERAGE_TREE_CHART);
    }

    /**
     * Opens tab containing {@link FileCoverageTable} and returns it.
     *
     * @return FileCoverageTable.
     */
    public FileCoverageTable getCoverageTable() {
        return openFileCoverageTable();
    }

    /**
     * Ensures the tab is 'File Overview'/FileCoverageTable is active.
     */
    private void ensureCoverageTableTabIsActive() {
        if (!getActiveTab().equals(Tab.FILE_OVERVIEW)) {
            openTabCoverageTable();
        }
    }

    /**
     * Ensures the tab is 'Package Overview'/CoverageTree is active.
     */
    private void ensureCoverageTreeTabIsActive() {
        if (!getActiveTab().equals(Tab.PACKAGE_OVERVIEW)) {
            openTabCoverageTree();
        }
    }

    /**
     * Ensures CoverageReport Page is opened.
     */
    private void ensureCoverageReportPageIsOpen() {
        MatcherAssert.assertThat("coverage report page was not opened", this.driver.getCurrentUrl(),
                CoreMatchers.anyOf(CoreMatchers.containsString(this.url.toString()),
                        CoreMatchers.containsString(this.url + "/")));
    }

    /**
     * Opens File Coverage Table.
     *
     * @return FileCoverageTable
     */
    public FileCoverageTable openFileCoverageTable() {
        ensureCoverageReportPageIsOpen();
        ensureCoverageTableTabIsActive();
        return new FileCoverageTable(this);
    }

    /**
     * Opens a tab within the overview section of the CoverageReport which contains the CoverageTree, aka Package
     * Overview.
     */
    public void openTabCoverageTree() {
        openTab(REPORT_OVERVIEW_TOGGLE, TAB_NAV_TREE, Tab.PACKAGE_OVERVIEW);
    }

    /**
     * Opens a tab within the detail section of the CoverageReport which contains the CoverageTable, aka Details
     * Overview.
     */
    public void openTabCoverageTable() {
        openTab(REPORT_DETAILS_TOGGLE, TAB_NAV_DETAILS, Tab.FILE_OVERVIEW);
    }

    /**
     * Returns the active tab.
     *
     * @return the active tab
     */
    public Tab getActiveTab() {
        WebElement activeTab = find(By.xpath("//a[@role='tab' and contains(@class, 'active')]"));
        return Tab.valueWithHref(extractRelativeUrl(activeTab.getAttribute("href")));
    }

    /**
     * Opens the specified tab.
     *
     * @param reportToggleId
     *         The ID of the report view toggle which opens the required report view
     * @param tabNavId
     *         The ID of the navigation bar that redirects to the tab to be selected
     * @param tab
     *         The tab that should be selected
     */
    private void openTab(final String reportToggleId, final String tabNavId, final Tab tab) {
        toggleReportView(reportToggleId);
        WebElement tabElement = getElement(By.id(tabNavId)).findElement(tab.getXpath());
        tabElement.click();
    }

    /**
     * Toggles the report view using the passed ID.
     *
     * @param id
     *         The ID of the toggle button to be used
     */
    private void toggleReportView(final String id) {
        WebElement viewToggleElement = getElement(By.id(id));
        viewToggleElement.click();
    }

    /**
     * Extract the relative url of a href (of a tab).
     *
     * @param absoluteUrl of href of tab
     * @return relative url.
     */
    private String extractRelativeUrl(String absoluteUrl) {
        return "#" + StringUtils.substringAfterLast(absoluteUrl, "#");
    }

    /**
     * Returns whether the right source file content is displayed depending on the availability of the source file.
     *
     * @param sourceFileAvailable
     *         {@code true} whether the source file is available and should be displayed
     *
     * @return {@code true} whether the expected source file content is displayed
     */
    public boolean isExpectedSourceFileContentDisplayed(final boolean sourceFileAvailable) {
        try {
            WebElement fileTable;
            if (sourceFileAvailable) {
                fileTable = getElement(By.id("source-file"));
            }
            else {
                fileTable = getElement((By.id("source-file-unavailable")));
            }
            return fileTable != null && fileTable.isDisplayed();
        }
        catch (NoSuchElementException exception) {
            return false;
        }
    }

    /**
     * Tabs of File Coverage Overview Table.
     */
    public enum Tab {
        PACKAGE_OVERVIEW("coverageTree"),
        FILE_OVERVIEW("coverageTable");

        private final String id;

        /**
         * Constructor for Tab.
         *
         * @param id of tab
         */
        Tab(String id) {
            this.id = id;
        }

        /**
         * Returns the enum element that has the specified href property.
         *
         * @param href to select the tab
         * @return the tab
         * @throws NoSuchElementException if the tab could not be found
         */
        static Tab valueWithHref(String href) {
            for (Tab tab : Tab.values()) {
                if (tab.id.equals(href.substring(1))) {
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
            return By.xpath("//a[@href='#" + id + "']");
        }
    }

}
