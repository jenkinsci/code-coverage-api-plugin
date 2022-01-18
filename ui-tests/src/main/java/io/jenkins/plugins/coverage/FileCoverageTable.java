package io.jenkins.plugins.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Area that represents the file coverage table in a {@link CoverageReport} page.
 */
@SuppressFBWarnings("EI")
public class FileCoverageTable {
    private static final String ID_OF_FILE_COVERAGE_TABLE = "coverage-details";
    private final CoverageReport coverageReport;
    private final List<FileCoverageTableRow> tableRows = new ArrayList<>();
    private final List<String> headers;
    private final WebElement tableElement;
    private final WebElement tableInfo;

    FileCoverageTable(final CoverageReport coverageReport) {
        this.coverageReport = coverageReport;

        tableElement = coverageReport.waitFor(
                By.xpath("//table[@id='" + ID_OF_FILE_COVERAGE_TABLE + "' and @isloaded='true']"));
        headers = tableElement.findElements(By.xpath(".//thead/tr/th"))
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
        tableInfo = coverageReport.find(By.id("coverage-details_info"));

        updateTableRows();
    }

    /**
     * Updates the table rows. E.g. if they are changed by toggling a details-row.
     */
    public final void updateTableRows() {
        tableRows.clear();

        List<WebElement> tableRowsAsWebElements = tableElement.findElements(By.xpath(".//tbody/tr"));
        tableRowsAsWebElements.forEach(element -> tableRows.add(createRow(element)));
    }

    /**
     * Creates the concrete table row as an object of the matching sub-class of {@link FileCoverageTableRow}. This row
     * contains the specialized column mapping of the corresponding issues table.
     *
     * @param row
     *         the WebElement representing the specific row
     *
     * @return the table row
     */
    protected FileCoverageTableRow createRow(final WebElement row) {
        return new FileCoverageTableRow(row, this);
    }

    /**
     * Returns the table row at the given index. This row instance contains the specialized column mapping of the
     * corresponding issues table.
     *
     * @param rowIndex
     *         the number of the row to be returned
     *
     * @return the row
     * @see #createRow(WebElement)
     */
    public FileCoverageTableRow getRow(final int rowIndex) {
        return getTableRows().get(rowIndex);
    }

    public List<String> getColumnHeaders() {
        return null; //getHeaders().stream().map(Header::fromTitle).collect(Collectors.toList());
    }

    /**
     * Returns the amount of the headers for this table.
     *
     * @return the amount of table headers
     */
    public int getHeaderSize() {
        return headers.size();
    }

    /**
     * Returns the amount of table rows.
     *
     * @return the amount of table rows.
     */
    public int getSize() {
        return tableRows.size();
    }

    /**
     * Returns the table rows as List.
     *
     * @return the rows of the table
     */
    public List<FileCoverageTableRow> getTableRows() {
        return tableRows;
    }

    /**
     * Return the headers of the table.
     *
     * @return the headers
     */
    public List<String> getHeaders() {
        return headers;
    }

    /**
     * Performs a click on the page button to open the page of the table.
     *
     * @param pageNumber
     *         the number representing the page to open
     */
    public void openTablePage(final int pageNumber) {
        WebElement webElement = coverageReport.find(
                By.xpath("//a[@class='page-link' and @data-dt-idx='" + (pageNumber - 1) + "']"));
        webElement.click();

        coverageReport.waitFor(By.xpath("//a[@class='page-link' and @data-dt-idx='" + (pageNumber - 1)
                + "']/parent::li[contains(@class, 'active')]"));

        updateTableRows();
    }

    /**
     * Get total number of entries.
     *
     * @return value of total entries
     */
    public int getTotals() {
        String textOfTableInfo = this.tableInfo.getText();
        String total = StringUtils.substringAfter(textOfTableInfo, "of ");
        return Integer.parseInt(StringUtils.substringBefore(total, " "));
    }

    /**
     * Enum representing the headers which should be present in a {@link FileCoverageTable}.
     */
    public enum Header {
        PACKAGE("Package"),
        FILE("File"),
        LINE_COVERAGE("Line Coverage"),
        BRANCH_COVERAGE("Branch Coverage");

        private final String title;

        Header(final String property) {
            title = property;
        }

        public String getTitle() {
            return title;
        }
    }

}
