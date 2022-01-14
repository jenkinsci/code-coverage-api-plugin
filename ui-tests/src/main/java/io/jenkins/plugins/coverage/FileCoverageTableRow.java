package io.jenkins.plugins.coverage;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Representation of a table row in {@link FileCoverageTable}
 */
public class FileCoverageTableRow {
    private final WebElement row;
    private final FileCoverageTable table;

    FileCoverageTableRow(final WebElement row, final FileCoverageTable table) {
        this.row = row;
        this.table = table;
    }

    final WebElement getRow() {
        return row;
    }

    final FileCoverageTable getTable() {
        return table;
    }

    public String getPackage() {
        return this.row.findElements(By.tagName("td")).get(0).getText();
    }

    public String getFile() {
        return this.row.findElements(By.tagName("td")).get(1).getText();
    }

    public String getLineCoverage() {
        return this.row.findElements(By.tagName("td")).get(2).getText();
    }

    public String getBranchCoverage() {
        return this.row.findElements(By.tagName("td")).get(4).getText();
    }

    /**
     * Returns all possible headers representing the columns of the table.
     *
     * @return the headers of the table
     */
    List<String> getHeaders() {
        return getTable().getHeaders();
    }

    /**
     * Returns all table data fields in the table row.
     *
     * @return the table data fields
     */
    final List<WebElement> getCells() {
        return getRow().findElements(By.tagName("td"));
    }

    /**
     * Returns a specific table data field specified by the header of the column.
     *
     * @param header
     *         the header text specifying the column
     *
     * @return the WebElement of the table data field
     */
    final WebElement getCell(final String header) {
        return getCells().get(getHeaders().indexOf(header));
    }

    /**
     * Returns the String representation of the table cell.
     *
     * @param header
     *         the header specifying the column
     *
     * @return the String representation of the cell
     */
    final String getCellContent(final String header) {
        if (!getHeaders().contains(header)) {
            return "-";
        }
        return getCell(header).getText();
    }

    final boolean isDetailsRow() {
        return getCells().size() == 1;
    }
}
